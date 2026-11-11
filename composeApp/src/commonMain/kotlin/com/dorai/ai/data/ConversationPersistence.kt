package com.dorai.ai.data

import app.cash.sqldelight.db.SqlDriver
import com.dorai.ai.TerminalLine
import com.dorai.ai.db.DoraIDatabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val ConversationJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Per-message byte budget for the database. Android hands query results back
 * through a CursorWindow that holds roughly 2 MB, and a single row larger than
 * the window aborts the whole read with SQLiteBlobTooBigException — which used
 * to crash the app on every launch once one oversized message was stored. Base64
 * file attachments (a PDF may be 20 MB) are the usual way a message gets there.
 */
private const val MAX_MESSAGE_JSON_BYTES = 1_000_000L

/** Head of the text kept when a message is still oversized after its files are dropped. */
private const val MAX_MESSAGE_TEXT_CHARS = 200_000

/** Cap on a persisted tool-call argument payload (e.g. a shell heredoc) before giving up on it. */
private const val MAX_TOOL_CALL_ARGUMENT_CHARS = 8_000

/**
 * Persistence backend for [ConversationStorage]. SQL-capable platforms store one
 * row per conversation and one row per message, so a save costs one conversation,
 * not the whole history. Platforms without a SQL driver (wasm) fall back to the
 * settings-key JSON blob. The `snapshot` parameter carries the full up-to-date
 * conversation list for backends that can only write wholesale.
 */
interface ConversationPersistence {
    fun loadAll(): List<Conversation>
    fun replaceAll(conversations: List<Conversation>)
    fun save(conversation: Conversation, snapshot: List<Conversation>)
    fun delete(id: String, snapshot: List<Conversation>)
    fun saveShellTranscript(conversation: Conversation, snapshot: List<Conversation>)
}

/** Returns null on platforms without a bundled SQLite (wasm). */
expect fun createConversationSqlDriver(): SqlDriver?

fun createConversationPersistence(appSettings: AppSettings): ConversationPersistence {
    val driver = createConversationSqlDriver()
    return if (driver != null) {
        SqlConversationPersistence(DoraIDatabase(driver), appSettings)
    } else {
        SettingsConversationPersistence(appSettings)
    }
}

class SqlConversationPersistence(
    private val database: DoraIDatabase,
    private val appSettings: AppSettings,
) : ConversationPersistence {

    private val queries get() = database.conversationQueries

    override fun loadAll(): List<Conversation> {
        importPendingJson()
        val messagesByConversation = queries.selectAllMessages(MAX_MESSAGE_JSON_BYTES).executeAsList()
            .groupBy({ it.conversationId }) { decodeMessage(it.messageJson) }
        return queries.selectAllConversations().executeAsList().map { row ->
            Conversation(
                id = row.id,
                messages = messagesByConversation[row.id].orEmpty().filterNotNull(),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                title = row.title,
                type = row.type,
                shellTranscript = decodeTranscript(row.shellTranscriptJson),
            )
        }
    }

    override fun replaceAll(conversations: List<Conversation>) {
        database.transaction {
            queries.deleteAllMessages()
            queries.deleteAllConversations()
            conversations.forEach { insert(it) }
        }
    }

    override fun save(conversation: Conversation, snapshot: List<Conversation>) {
        database.transaction {
            queries.deleteMessages(conversation.id)
            insert(conversation)
        }
    }

    override fun delete(id: String, snapshot: List<Conversation>) {
        database.transaction {
            queries.deleteMessages(id)
            queries.deleteConversation(id)
        }
    }

    override fun saveShellTranscript(conversation: Conversation, snapshot: List<Conversation>) {
        queries.updateShellTranscript(
            shellTranscriptJson = ConversationJson.encodeToString(conversation.shellTranscript),
            id = conversation.id,
        )
    }

    /**
     * The settings key doubles as an import inbox: settings import (and the one-time
     * migration from the pre-database format) writes the full conversation set there.
     * A present key replaces the database content; a blank key means "clear all".
     * An *unparseable* non-blank value used to wipe the database with `emptyList()`;
     * it is now quarantined and the database is left untouched instead.
     */
    private fun importPendingJson() {
        val pending = appSettings.getConversationsJson() ?: return
        if (pending.isBlank()) {
            // Explicit "clear conversations" signal from the importer.
            replaceAll(emptyList())
            appSettings.removeConversationsJson()
            return
        }
        val conversations = try {
            ConversationJson.decodeFromString<ConversationsData>(pending).conversations
        } catch (e: Exception) {
            // Keep the unparseable blob under its own key so a later inspection or
            // import attempt can recover it instead of overwriting the database.
            appSettings.backupCorruptJson("conversations_import_pending", pending)
            println("ConversationPersistence: ignoring unparseable pending import (" + e.message?.take(120) + ") — database left untouched")
            appSettings.removeConversationsJson()
            return
        }
        replaceAll(conversations)
        appSettings.removeConversationsJson()
    }

    private fun insert(conversation: Conversation) {
        queries.upsertConversation(
            id = conversation.id,
            title = conversation.title,
            type = conversation.type,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            shellTranscriptJson = ConversationJson.encodeToString(conversation.shellTranscript),
        )
        conversation.messages.forEachIndexed { index, message ->
            queries.insertMessage(
                conversationId = conversation.id,
                orderIndex = index.toLong(),
                messageJson = encodeMessage(message),
            )
        }
    }

    /**
     * Encodes a message, shrinking it when the JSON would exceed
     * [MAX_MESSAGE_JSON_BYTES]. Base64 file payloads go first — they are the part
     * that can be megabytes — and only then is the text cut, so a chat with a huge
     * attachment loses the attachment instead of the whole conversation history.
     */
    private fun encodeMessage(message: Conversation.Message): String {
        val encoded = ConversationJson.encodeToString(message)
        if (!encoded.exceedsUtf8Budget(MAX_MESSAGE_JSON_BYTES)) return encoded

        val withoutFiles = message.copy(
            attachments = emptyList(),
            mimeType = null,
            data = null,
            fileName = null,
        )
        val withoutFilesJson = ConversationJson.encodeToString(withoutFiles)
        if (!withoutFilesJson.exceedsUtf8Budget(MAX_MESSAGE_JSON_BYTES)) return withoutFilesJson

        // Everything that can still be large goes: the reasoning trace, and the copy
        // of the source message a dorai-ui submission carries. What is left is bounded
        // by MAX_MESSAGE_TEXT_CHARS, so the row is guaranteed to fit.
        val trimmed = withoutFiles.copy(
            content = withoutFiles.content.take(MAX_MESSAGE_TEXT_CHARS),
            reasoningContent = null,
            uiSubmission = null,
        )
        val trimmedJson = ConversationJson.encodeToString(trimmed)
        if (!trimmedJson.exceedsUtf8Budget(MAX_MESSAGE_JSON_BYTES)) return trimmedJson

        // Tool-call argument payloads are unbounded (a shell command can carry a whole
        // heredoc). Shrink them, then drop them, so the row always fits eventually.
        val argumentsTrimmed = trimmed.copy(
            toolCalls = trimmed.toolCalls?.map {
                it.copy(arguments = it.arguments.take(MAX_TOOL_CALL_ARGUMENT_CHARS), thoughtSignature = null)
            },
        )
        val argumentsJson = ConversationJson.encodeToString(argumentsTrimmed)
        if (!argumentsJson.exceedsUtf8Budget(MAX_MESSAGE_JSON_BYTES)) return argumentsJson
        return ConversationJson.encodeToString(argumentsTrimmed.copy(toolCalls = null))
    }

    private fun decodeMessage(json: String): Conversation.Message? = try {
        ConversationJson.decodeFromString<Conversation.Message>(json)
    } catch (e: Exception) {
        println("ConversationPersistence: dropping an undecodable message row (" + e.message?.take(120) + ")")
        null
    }

    private fun decodeTranscript(json: String): List<TerminalLine> = try {
        ConversationJson.decodeFromString<List<TerminalLine>>(json)
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * True when the UTF-8 encoding of this string is larger than [budget]. UTF-8 uses
 * one to three bytes per char, so the two length comparisons decide the common
 * cases without encoding megabytes of text on every save.
 */
private fun String.exceedsUtf8Budget(budget: Long): Boolean = when {
    length.toLong() > budget -> true
    length.toLong() * 3 <= budget -> false
    else -> encodeToByteArray().size > budget
}

class SettingsConversationPersistence(private val appSettings: AppSettings) : ConversationPersistence {

    override fun loadAll(): List<Conversation> {
        val data = appSettings.getConversationsJson() ?: return emptyList()
        return try {
            ConversationJson.decodeFromString<ConversationsData>(data).conversations
        } catch (e: Exception) {
            // A corrupt settings blob must not be silently overwritten with the
            // current in-memory state on the next save — quarantine it first so
            // the user's chat history survives for inspection/recovery.
            appSettings.backupCorruptJson("conversations_settings_blob", data)
            println("SettingsConversationPersistence: ignoring corrupt settings blob (" + e.message?.take(120) + ")")
            emptyList()
        }
    }

    override fun replaceAll(conversations: List<Conversation>) = writeAll(conversations)

    override fun save(conversation: Conversation, snapshot: List<Conversation>) = writeAll(snapshot)

    override fun delete(id: String, snapshot: List<Conversation>) = writeAll(snapshot)

    override fun saveShellTranscript(conversation: Conversation, snapshot: List<Conversation>) = writeAll(snapshot)

    private fun writeAll(conversations: List<Conversation>) {
        appSettings.setConversationsJson(
            ConversationJson.encodeToString(ConversationsData(conversations = conversations)),
        )
    }
}
