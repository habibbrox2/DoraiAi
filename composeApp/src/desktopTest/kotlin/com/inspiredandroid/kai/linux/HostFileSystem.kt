package com.inspiredandroid.kai.linux

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/**
 * What the host filesystem can do, probed once per test run.
 *
 * The guest home lives on the host disk, and the two POSIX features the migration relies on —
 * symlinks and owner-only permissions — are not universally available there: Windows needs
 * Developer Mode (or an elevated process) before a symlink can be created at all, and its
 * permission model has no POSIX bits to set. The features are worth testing wherever they
 * exist, and worth skipping — loudly, not silently — where they do not.
 */
internal object HostFileSystem {

    /** True when a symlink can actually be created on this filesystem. */
    val canCreateSymlinks: Boolean by lazy {
        runCatching {
            val dir = Files.createTempDirectory("kai-symlink-probe")
            try {
                Files.createSymbolicLink(dir.resolve("link"), dir)
                true
            } finally {
                dir.toFile().deleteRecursively()
            }
        }.getOrDefault(false)
    }

    /** True when this filesystem has POSIX permission bits to read and write. */
    val hasPosixPermissions: Boolean by lazy {
        runCatching {
            val file = Files.createTempFile("kai-posix-probe", ".txt")
            try {
                Files.setPosixFilePermissions(file, setOf(PosixFilePermission.OWNER_READ))
                Files.getPosixFilePermissions(file).contains(PosixFilePermission.OWNER_READ)
            } finally {
                file.toFile().delete()
            }
        }.getOrDefault(false)
    }
}
