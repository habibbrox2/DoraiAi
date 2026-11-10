package com.dorai.ai.ui.sandbox

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dorai.ai.FileBrowserSource
import com.dorai.ai.SandboxFileEntry
import com.dorai.ai.TextFileResult
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.sandbox_files_delete_failed
import dorai.composeapp.generated.resources.sandbox_files_delete_success
import dorai.composeapp.generated.resources.sandbox_files_editor_closed_after_delete
import dorai.composeapp.generated.resources.sandbox_files_import_failed
import dorai.composeapp.generated.resources.sandbox_files_import_success
import dorai.composeapp.generated.resources.sandbox_files_open_failed
import dorai.composeapp.generated.resources.sandbox_files_rename_error_collision
import dorai.composeapp.generated.resources.sandbox_files_rename_error_invalid
import dorai.composeapp.generated.resources.sandbox_files_rename_failed
import dorai.composeapp.generated.resources.sandbox_files_rename_success
import dorai.composeapp.generated.resources.sandbox_files_save_failed
import dorai.composeapp.generated.resources.sandbox_files_save_success
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * Extensions a tap hands straight to another app. Everything else — including
 * every file with no extension at all — goes to the in-app editor, which decides
 * from the bytes whether it is text and offers a way out when it is not.
 *
 * Listing what is *not* text is the way round that works here: a Linux tree is
 * full of text with no extension (`id_rsa`, `Makefile`, `known_hosts`) or an
 * extension no list will ever have (`.pub`, `.service`, `.rules`). Guessing
 * "text" wrong costs one tap and a message saying so; guessing "binary" wrong
 * meant the file could not be read in the app at all.
 */
private val EXTERNAL_EXTENSIONS = setOf(
    // Images and video.
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico", "tif", "tiff", "heic", "heif",
    "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp",
    // Audio.
    "mp3", "wav", "ogg", "flac", "m4a", "aac", "opus", "mid", "midi",
    // Documents another app renders far better than a text field.
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "epub",
    // Archives and packages.
    "zip", "tar", "gz", "tgz", "bz2", "xz", "zst", "7z", "rar", "jar", "apk", "aab",
    "deb", "rpm", "iso", "dmg",
    // Compiled output, databases, fonts.
    "so", "o", "a", "class", "dex", "bin", "exe", "dll", "dylib", "wasm", "pyc",
    "db", "sqlite", "sqlite3", "ttf", "otf", "woff", "woff2",
)

/** How many previous editor buffers undo can step back through. */
private const val MAX_UNDO_DEPTH = 100

@Immutable
sealed interface EditorState {
    data object Loading : EditorState

    /**
     * [readOnly] marks a buffer that was decoded lossily on the user's request. It can be
     * looked at but never saved — writing it back would replace the file's real bytes
     * with the replacement characters the decoder substituted.
     */
    data class Loaded(
        val path: String,
        val original: String,
        val current: String,
        val readOnly: Boolean = false,
    ) : EditorState {
        val dirty: Boolean get() = !readOnly && original != current
    }

    /** Not valid UTF-8. Offers a forced, read-only decode. */
    data class Binary(val path: String) : EditorState

    /** Past the editor's cap — no forced read, since a truncated buffer can't be edited safely. */
    data class TooLarge(val path: String, val sizeBytes: Long) : EditorState

    data class Unreadable(val path: String) : EditorState
}

@Immutable
data class RenameState(
    val originalEntry: SandboxFileEntry,
    val input: String,
    val error: StringResource? = null,
)

@Immutable
data class FileBrowserUiState(
    val currentPath: String = "/",
    val entries: List<SandboxFileEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val editor: EditorState? = null,
    val snackbarMessage: StringResource? = null,
    val pendingDelete: SandboxFileEntry? = null,
    val renaming: RenameState? = null,
    val importing: Boolean = false,
    /** A save() is in flight — duplicate saves are ignored until it finishes. */
    val editorSaving: Boolean = false,
    /** The undo stack has an entry for the open document. */
    val canUndoEditor: Boolean = false,
)

class SandboxFileBrowserViewModel(
    private val files: FileBrowserSource,
) : ViewModel() {

    private val _state = MutableStateFlow(FileBrowserUiState())
    val state = _state.asStateFlow()

    /** Last starting point [start] was given; null until the browser has opened once. */
    private var startPath: String? = null

    /**
     * Monotonic document revision, bumped on every load/edit/undo/reload. A save
     * captures the revision it started at, so an edit made while the write is in
     * flight is never mistaken for saved content.
     */
    private var editorRevision = 0L

    /** Monotonic load sequence — a slow read for a file no longer wanted is discarded. */
    private var loadSequence = 0L

    /** Previous buffers for the open document, oldest first. Capped at [MAX_UNDO_DEPTH]. */
    private val undoStack = ArrayDeque<String>()

    /**
     * Dirty buffers keyed by path, kept when the user switches away from an edited
     * file so reopening it restores the unsaved work instead of discarding it.
     */
    private val pendingBuffers = mutableMapOf<String, String>()

    /**
     * Called every time the browser becomes visible. The agent mutates the sandbox
     * behind our back, so re-entering an already-loaded directory re-lists it
     * instead of serving the cache.
     *
     * Where the user browsed to is kept across that: only the first open, or a
     * caller that moved its starting point (Workspace opening another project),
     * jumps to [initialPath]. Coming back from a terminal tab used to land on the
     * starting directory again, which made anywhere else in the tree a place the
     * browser would not stay.
     */
    fun start(initialPath: String) {
        val normalized = normalize(initialPath)
        val moved = startPath != normalized
        startPath = normalized
        if (moved) {
            navigateTo(normalized)
            return
        }
        viewModelScope.launch {
            refreshCurrent(silent = true)
            reloadEditorIfClean()
        }
    }

    fun navigateTo(path: String) {
        val normalized = normalize(path)
        val current = _state.value
        // Already showing this directory: keep the entries on screen and re-list
        // underneath, so re-tapping a breadcrumb doesn't flash a spinner.
        val samePath = current.currentPath == normalized && current.entries.isNotEmpty()
        preservePendingEditor()
        undoStack.clear()
        loadSequence++
        _state.update {
            it.copy(
                currentPath = normalized,
                loading = !samePath,
                error = null,
                editor = null,
                canUndoEditor = false,
            )
        }
        viewModelScope.launch { refreshCurrent(silent = samePath) }
    }

    /**
     * A [silent] refresh leaves the state instance untouched when the listing is
     * unchanged. That matters: an equal state is conflated by the StateFlow, so the
     * list never recomposes and its scroll position cannot shift.
     */
    private suspend fun refreshCurrent(silent: Boolean = false) {
        val path = _state.value.currentPath
        val entries = files.listDirectory(path)
        _state.update {
            when {
                // Navigated away while listing — the newer load owns the state.
                it.currentPath != path -> it

                silent && it.entries == entries -> it

                else -> it.copy(entries = entries, loading = false)
            }
        }
    }

    /**
     * Picks up agent edits to the file currently open in the editor. Unsaved user
     * edits always win — reloading over them would lose work, and saving a stale
     * buffer afterwards would clobber what the agent wrote.
     */
    private suspend fun reloadEditorIfClean() {
        val editor = _state.value.editor as? EditorState.Loaded ?: return
        if (editor.dirty || editor.readOnly) return
        val text = (files.readTextFile(editor.path) as? TextFileResult.Text)?.content ?: return
        if (text == editor.original) return
        editorRevision++
        _state.update {
            val latest = it.editor
            if (latest is EditorState.Loaded && latest.path == editor.path && !latest.dirty) {
                it.copy(editor = latest.copy(original = text, current = text))
            } else {
                it
            }
        }
    }

    fun openEntry(entry: SandboxFileEntry) {
        if (entry.isDirectory) {
            navigateTo(entry.path)
            return
        }
        viewModelScope.launch {
            val ext = entry.name.substringAfterLast('.', "").lowercase()
            if (ext in EXTERNAL_EXTENSIONS) {
                val result = files.openFile(entry.path)
                // No app took it: the editor still says something useful about why.
                if (result.isSuccess) return@launch
            }
            loadInEditor(entry.path)
        }
    }

    /**
     * Opens [path] in the editor whatever its name says — the row menu's way past
     * the extension rule, for the file this app would otherwise hand to another one.
     */
    fun openInEditor(path: String) {
        viewModelScope.launch { loadInEditor(path) }
    }

    fun openInExternalApp(path: String) {
        viewModelScope.launch {
            val result = files.openFile(path)
            if (result.isFailure) {
                _state.update { it.copy(snackbarMessage = Res.string.sandbox_files_open_failed) }
            }
        }
    }

    /** The escape hatch from [EditorState.Binary]: decode the bytes anyway, read-only. */
    fun loadAsText(path: String) {
        viewModelScope.launch {
            loadInEditor(path, force = true)
        }
    }

    private suspend fun loadInEditor(path: String, force: Boolean = false) {
        preservePendingEditor()
        undoStack.clear()
        val sequence = ++loadSequence
        _state.update { it.copy(editor = EditorState.Loading, canUndoEditor = false) }
        val editor = when (val result = files.readTextFile(path, force = force)) {
            is TextFileResult.Text -> {
                val pending = pendingBuffers[path]
                EditorState.Loaded(
                    path = path,
                    original = result.content,
                    current = pending ?: result.content,
                    readOnly = !result.editable,
                )
            }

            TextFileResult.Binary -> EditorState.Binary(path)

            is TextFileResult.TooLarge -> EditorState.TooLarge(path, result.sizeBytes)

            TextFileResult.Unreadable -> EditorState.Unreadable(path)
        }
        // A slower load for a file no longer wanted must not clobber the newer one.
        if (sequence != loadSequence) return
        editorRevision++
        _state.update { it.copy(editor = editor, canUndoEditor = false) }
    }

    private fun preservePendingEditor() {
        val editor = _state.value.editor as? EditorState.Loaded ?: return
        if (editor.dirty) {
            pendingBuffers[editor.path] = editor.current
        }
    }

    fun updateEditorContent(content: String) {
        _state.update { state ->
            val editor = state.editor
            if (editor is EditorState.Loaded && !editor.readOnly && editor.current != content) {
                if (undoStack.lastOrNull() != editor.current) {
                    undoStack += editor.current
                    while (undoStack.size > MAX_UNDO_DEPTH) undoStack.removeFirst()
                }
                editorRevision++
                if (content == editor.original) {
                    pendingBuffers.remove(editor.path)
                } else {
                    pendingBuffers[editor.path] = content
                }
                state.copy(editor = editor.copy(current = content), canUndoEditor = true)
            } else {
                state
            }
        }
    }

    /**
     * Steps the open document back to its previous buffer. No-op on read-only or
     * empty history — there is intentionally no redo in this phase.
     */
    fun undoEditor() {
        val editor = _state.value.editor as? EditorState.Loaded ?: return
        if (editor.readOnly) return
        val previous = undoStack.removeLastOrNull() ?: return
        editorRevision++
        if (previous == editor.original) {
            pendingBuffers.remove(editor.path)
        } else {
            pendingBuffers[editor.path] = previous
        }
        _state.update { state ->
            val latest = state.editor
            if (latest is EditorState.Loaded && latest.path == editor.path && !latest.readOnly) {
                state.copy(editor = latest.copy(current = previous), canUndoEditor = undoStack.isNotEmpty())
            } else {
                state
            }
        }
    }

    fun save() {
        val editor = _state.value.editor as? EditorState.Loaded ?: return
        if (editor.readOnly || _state.value.editorSaving) return
        if (!editor.dirty) return
        val snapshotPath = editor.path
        val snapshotContent = editor.current
        _state.update { it.copy(editorSaving = true) }
        viewModelScope.launch {
            val ok = files.writeTextFile(snapshotPath, snapshotContent)
            if (ok) {
                // The write persisted [snapshotContent], not whatever is on screen
                // now — edits made while it was in flight stay dirty.
                editorRevision++
                _state.update { state ->
                    val latest = state.editor
                    if (latest is EditorState.Loaded && latest.path == snapshotPath) {
                        if (latest.current == snapshotContent) {
                            pendingBuffers.remove(snapshotPath)
                            state.copy(
                                editor = latest.copy(original = snapshotContent),
                                editorSaving = false,
                                snackbarMessage = Res.string.sandbox_files_save_success,
                            )
                        } else {
                            pendingBuffers[snapshotPath] = latest.current
                            state.copy(
                                editor = latest.copy(original = snapshotContent),
                                editorSaving = false,
                                snackbarMessage = Res.string.sandbox_files_save_success,
                            )
                        }
                    } else {
                        // The user switched files mid-save: never repaint the new
                        // document with the old one's result.
                        state.copy(editorSaving = false, snackbarMessage = Res.string.sandbox_files_save_success)
                    }
                }
            } else {
                _state.update {
                    it.copy(editorSaving = false, snackbarMessage = Res.string.sandbox_files_save_failed)
                }
            }
        }
    }

    fun requestDelete(entry: SandboxFileEntry) {
        _state.update { it.copy(pendingDelete = entry) }
    }

    fun cancelDelete() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val entry = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            val ok = files.deleteEntry(entry.path, recursive = entry.isDirectory)
            if (ok) {
                pendingBuffers.remove(entry.path)
                val editor = _state.value.editor
                val editorPath = editorPathOf(editor)
                val editorClosed = editorPath != null && editorPath == entry.path
                if (editorClosed) {
                    undoStack.clear()
                    loadSequence++
                }
                val snackbar = if (editorClosed) {
                    Res.string.sandbox_files_editor_closed_after_delete
                } else {
                    Res.string.sandbox_files_delete_success
                }
                _state.update {
                    it.copy(
                        editor = if (editorClosed) null else it.editor,
                        canUndoEditor = if (editorClosed) false else it.canUndoEditor,
                        snackbarMessage = snackbar,
                    )
                }
                // Deleting a directory may also remove the open file's path.
                if (entry.isDirectory) {
                    val deleted = _state.value.editor as? EditorState.Loaded
                    if (deleted != null && deleted.path.startsWith(entry.path)) {
                        pendingBuffers.keys.filter { it.startsWith(entry.path) }.forEach { pendingBuffers.remove(it) }
                        undoStack.clear()
                        loadSequence++
                        _state.update { it.copy(editor = null, canUndoEditor = false) }
                    }
                }
                refreshCurrent()
            } else {
                _state.update { it.copy(snackbarMessage = Res.string.sandbox_files_delete_failed) }
            }
        }
    }

    fun requestRename(entry: SandboxFileEntry) {
        _state.update { it.copy(renaming = RenameState(originalEntry = entry, input = entry.name)) }
    }

    fun updateRenameInput(value: String) {
        _state.update { state ->
            val rename = state.renaming ?: return@update state
            state.copy(renaming = rename.copy(input = value, error = null))
        }
    }

    fun cancelRename() {
        _state.update { it.copy(renaming = null) }
    }

    fun confirmRename() {
        val rename = _state.value.renaming ?: return
        val entry = rename.originalEntry
        val newName = rename.input.trim()
        if (newName.isEmpty() || newName == entry.name ||
            newName.contains('/') || newName.contains('\\') ||
            newName == "." || newName == ".."
        ) {
            if (newName == entry.name) {
                _state.update { it.copy(renaming = null) }
            } else {
                _state.update {
                    it.copy(renaming = rename.copy(error = Res.string.sandbox_files_rename_error_invalid))
                }
            }
            return
        }
        viewModelScope.launch {
            val result = files.renameEntry(entry.path, newName)
            result.fold(
                onSuccess = { newPath ->
                    pendingBuffers.remove(entry.path)?.let { pendingBuffers[newPath] = it }
                    val editor = _state.value.editor
                    val updatedEditor = if (editor is EditorState.Loaded && editor.path == entry.path) {
                        editor.copy(path = newPath)
                    } else if (editor is EditorState.Binary && editor.path == entry.path) {
                        EditorState.Binary(newPath)
                    } else {
                        editor
                    }
                    _state.update {
                        it.copy(
                            renaming = null,
                            editor = updatedEditor,
                            snackbarMessage = Res.string.sandbox_files_rename_success,
                        )
                    }
                    refreshCurrent()
                },
                onFailure = { e ->
                    val message = e.message
                    val errorRes = when {
                        message == "collision" -> Res.string.sandbox_files_rename_error_collision
                        e is IllegalArgumentException -> Res.string.sandbox_files_rename_error_invalid
                        else -> null
                    }
                    if (errorRes != null) {
                        _state.update {
                            it.copy(renaming = rename.copy(error = errorRes))
                        }
                    } else {
                        _state.update {
                            it.copy(
                                renaming = null,
                                snackbarMessage = Res.string.sandbox_files_rename_failed,
                            )
                        }
                    }
                },
            )
        }
    }

    /**
     * Copies a file picked from device storage into the directory on screen. Until this
     * existed the only way into the sandbox was the agent's shell.
     */
    fun importFile(source: PlatformFile) {
        if (_state.value.importing) return
        val directory = _state.value.currentPath
        _state.update { it.copy(importing = true) }
        viewModelScope.launch {
            val result = files.importFile(directory, source)
            _state.update {
                it.copy(
                    importing = false,
                    snackbarMessage = if (result.isSuccess) {
                        Res.string.sandbox_files_import_success
                    } else {
                        Res.string.sandbox_files_import_failed
                    },
                )
            }
            // The listing only gains a row when the import landed in the directory the
            // user is still looking at; refreshCurrent is a no-op otherwise.
            if (result.isSuccess) refreshCurrent()
        }
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    private fun editorPathOf(editor: EditorState?): String? = when (editor) {
        is EditorState.Loaded -> editor.path
        is EditorState.Binary -> editor.path
        is EditorState.TooLarge -> editor.path
        is EditorState.Unreadable -> editor.path
        else -> null
    }

    private fun normalize(path: String): String {
        if (path.isEmpty()) return "/"
        if (!path.startsWith("/")) return "/$path"
        if (path.length > 1 && path.endsWith("/")) return path.dropLast(1)
        return path
    }
}
