package com.dorai.ai.data

import com.dorai.ai.TerminalLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

expect fun readLegacyConversationFile(): ByteArray?
expect fun deleteLegacyConversationFile()

private const val MAX_SHELL_TRANSCRIPT_CHARS = 10_000

class ConversationStorage(
    private val appSettings: AppSettings,
    private val persistence: ConversationPersistence,
) {
    private val mutableConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = mutableConversations.asStateFlow()

    fun loadConversations() {
        val loaded = try {
            persistence.loadAll()
        } catch (e: Exception) {
            // A persistence failure (e.g. corrupt SQL driver / OOM) must not crash
            // the app at startup — start empty and log loudly instead.
            println("ConversationStorage: failed to load conversations (" + e.message?.take(120) + "); starting with empty list")
            return
        }
        mutableConversations.value = loaded
        if (loaded.isEmpty()) {
            migrateLegacy()
        }
    }

    fun saveConversation(conversation: Conversation) {
        val previous = mutableConversations.value
        var saved = conversation
        mutableConversations.update { current ->
            val index = current.indexOfFirst { it.id == conversation.id }
            if (index >= 0) {
                val existing = current[index]
                // Chat-layer save paths rebuild Conversation without the shell
                // transcript; preserve the stored tail rather than wiping it.
                val merged = if (conversation.shellTranscript.isEmpty() && existing.shellTranscript.isNotEmpty()) {
                    conversation.copy(shellTranscript = existing.shellTranscript)
                } else {
                    conversation
                }
                saved = merged
                current.toMutableList().apply { set(index, merged) }
            } else {
                current + conversation
            }
        }
        try {
            persistence.save(saved, mutableConversations.value)
        } catch (e: Exception) {
            // Persistence failed — revert the in-memory change so the UI never
            // shows state that wasn't saved, then surface the failure to the caller.
            println("ConversationStorage: failed to persist conversation " + conversation.id + " (" + e.message?.take(120) + "); reverting in-memory state")
            mutableConversations.value = previous
            throw e
        }
    }

    /**
     * Persist a shell transcript snapshot for [conversationId]. No-op when the
     * conversation isn't in the saved set yet (chat hasn't been saved). The
     * stored transcript is trimmed from the head until the joined text is
     * within the per-conversation char budget — older lines drop first so the
     * tail (most recent activity) survives a restart.
     */
    fun updateShellTranscript(conversationId: String, lines: List<TerminalLine>) {
        val trimmed = trimToCharLimit(lines, MAX_SHELL_TRANSCRIPT_CHARS)
        val previous = mutableConversations.value
        var changed: Conversation? = null
        mutableConversations.update { current ->
            val idx = current.indexOfFirst { it.id == conversationId }
            if (idx < 0) {
                current
            } else if (current[idx].shellTranscript == trimmed) {
                current
            } else {
                val updated = current[idx].copy(shellTranscript = trimmed)
                changed = updated
                current.toMutableList().apply { set(idx, updated) }
            }
        }
        changed?.let {
            try {
                persistence.saveShellTranscript(it, mutableConversations.value)
            } catch (e: Exception) {
                println("ConversationStorage: failed to persist shell transcript for " + conversationId + " (" + e.message?.take(120) + "); reverting in-memory state")
                mutableConversations.value = previous
            }
        }
    }

    private fun trimToCharLimit(lines: List<TerminalLine>, limit: Int): List<TerminalLine> {
        if (lines.isEmpty()) return lines
        val totalChars = lines.sumOf { it.text.length }
        if (totalChars <= limit) return lines
        val tail = ArrayDeque<TerminalLine>()
        var running = 0
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (running + line.text.length <= limit) {
                tail.addFirst(line)
                running += line.text.length
            } else {
                break
            }
        }
        if (tail.isNotEmpty()) return tail.toList()
        // Single most-recent line is itself larger than the budget — keep its
        // tail so the user still sees something on next launch.
        val last = lines.last()
        return listOf(last.withText(last.text.takeLast(limit)))
    }

    fun deleteConversation(id: String) {
        val previous = mutableConversations.value
        mutableConversations.update { current ->
            current.filter { it.id != id }
        }
        try {
            persistence.delete(id, mutableConversations.value)
        } catch (e: Exception) {
            println("ConversationStorage: failed to delete " + id + " (" + e.message?.take(120) + "); reverting in-memory state")
            mutableConversations.value = previous
            throw e
        }
    }

    fun renameConversation(id: String, title: String) {
        val previous = mutableConversations.value
        var changed: Conversation? = null
        mutableConversations.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) {
                current
            } else {
                val updated = current[index].copy(title = title)
                changed = updated
                current.toMutableList().apply { set(index, updated) }
            }
        }
        changed?.let {
            try {
                persistence.save(it, mutableConversations.value)
            } catch (e: Exception) {
                println("ConversationStorage: failed to persist rename " + id + " (" + e.message?.take(120) + "); reverting in-memory state")
                mutableConversations.value = previous
                throw e
            }
        }
    }

    fun togglePinConversation(id: String) {
        val previous = mutableConversations.value
        var changed: Conversation? = null
        mutableConversations.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) {
                current
            } else {
                val existing = current[index]
                val nowPinned = !existing.isPinned
                val updated = existing.copy(
                    isPinned = nowPinned,
                    pinnedAt = if (nowPinned) Clock.System.now().toEpochMilliseconds() else 0L,
                )
                changed = updated
                current.toMutableList().apply { set(index, updated) }
            }
        }
        changed?.let {
            try {
                persistence.save(it, mutableConversations.value)
            } catch (e: Exception) {
                println("ConversationStorage: failed to persist pin toggle " + id + " (" + e.message?.take(120) + "); reverting in-memory state")
                mutableConversations.value = previous
                throw e
            }
        }
    }

    private fun migrateLegacy() {
        val legacyData = readLegacyConversationFile() ?: return
        val key = appSettings.getEncryptionKey() ?: return
        val decrypted = ByteArray(legacyData.size)
        for (i in legacyData.indices) {
            decrypted[i] = (legacyData[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        val decoded = decrypted.decodeToString()
        val conversations = try {
            ConversationJson.decodeFromString<ConversationsData>(decoded).conversations
        } catch (_: Exception) {
            // Unparseable legacy file: keep it (do NOT delete it) and leave the
            // database untouched — the user can still recover the data by hand.
            println("ConversationStorage: legacy file at ~/.dorai is undecodable; kept on disk for manual recovery")
            try {
                appSettings.backupCorruptJson("conversations_legacy_file", decoded)
            } catch (_: Exception) {
                // backup failed — but the file itself stays untouched
            }
            return
        }
        mutableConversations.value = conversations
        persistence.replaceAll(conversations)
        deleteLegacyConversationFile()
    }
}
