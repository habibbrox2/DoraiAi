package com.inspiredandroid.kai.filereview

import java.io.File
import java.nio.file.Files

/**
 * Single Android project workspace shared by Chat Sandbox and Workspace.
 * Both environments mount LinuxPaths.projectsDir at /root/projects.
 */
class AndroidProjectFileReviewFileSystem(
    private val workspaceRoot: File,
) : FileReviewFileSystem {
    override fun resolve(workspace: FileReviewWorkspace, guestPath: String): File? {
        if (workspace != FileReviewWorkspace.PROJECT) return null
        if (guestPath.isBlank() || guestPath.startsWith("/") || guestPath.contains('\\') || guestPath.contains('\u0000')) return null
        val parts = guestPath.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) return null
        // Check the configured root itself before canonicalization; canonicalFile would
        // otherwise follow a symlink and make the later check ineffective.
        if (Files.isSymbolicLink(workspaceRoot.toPath())) return null
        val root = runCatching { workspaceRoot.canonicalFile }.getOrNull() ?: return null
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath())) return null
        var target = root
        for (part in parts) {
            target = File(target, part)
            if (Files.isSymbolicLink(target.toPath())) return null
        }
        val canonicalTarget = runCatching { target.canonicalFile }.getOrNull() ?: return null
        if (canonicalTarget != root &&
            !canonicalTarget.path.startsWith(root.path + File.separator)
        ) {
            return null
        }
        return target
    }
}




