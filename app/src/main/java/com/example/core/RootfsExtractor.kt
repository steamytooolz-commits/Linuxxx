package com.example.core

import android.os.Build
import android.system.Os
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Handles tar.xz and tar.gz rootfs extraction with Apache Commons Compress,
 * symlink resolution, permissions preservation, and directory traversal protection.
 */
class RootfsExtractor {

    private val tag = "RootfsExtractor"

    fun extract(
        archiveFile: File,
        destDir: File,
        onProgress: (Float, String) -> Unit
    ): Result<Unit> {
        return try {
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            onProgress(0.52f, "Inspecting archive: ${archiveFile.name}...")

            var isXz = archiveFile.name.endsWith(".xz", ignoreCase = true)
            var isGz = archiveFile.name.endsWith(".gz", ignoreCase = true) || archiveFile.name.endsWith(".tgz", ignoreCase = true)

            // Probe magic bytes to override incorrect extensions
            try {
                FileInputStream(archiveFile).use { fis ->
                    val header = ByteArray(6)
                    val read = fis.read(header)
                    if (read >= 2) {
                        if (header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) {
                            isGz = true
                            isXz = false
                        } else if (read >= 6 &&
                            header[0] == 0xFD.toByte() &&
                            header[1] == 0x37.toByte() &&
                            header[2] == 0x7A.toByte() &&
                            header[3] == 0x58.toByte() &&
                            header[4] == 0x5A.toByte() &&
                            header[5] == 0x00.toByte()
                        ) {
                            isXz = true
                            isGz = false
                        }
                    }
                }
            } catch (probeEx: Exception) {
                Log.w(tag, "Failed to probe file magic bytes: ${probeEx.message}")
            }

            var extractedCount = 0
            var rootPrefix: String? = null
            val standardRootDirs = setOf(
                "bin", "boot", "dev", "etc", "home", "lib", "lib64", "media",
                "mnt", "opt", "proc", "root", "run", "sbin", "srv", "sys", "tmp", "usr", "var"
            )

            FileInputStream(archiveFile).use { fis ->
                BufferedInputStream(fis, 128 * 1024).use { bis ->
                    val decompressor: InputStream = when {
                        isXz -> XZCompressorInputStream(bis)
                        isGz -> GzipCompressorInputStream(bis)
                        else -> bis
                    }

                    TarArchiveInputStream(decompressor).use { tarIn ->
                        var entry: TarArchiveEntry? = tarIn.nextEntry
                        val canonicalDest = destDir.canonicalPath

                        while (entry != null) {
                            var entryName = entry.name.removePrefix("./")
                            val rawName = entryName.trim('/')

                            // Detect if archive has a top-level wrapper directory (e.g. ubuntu-noble-aarch64/)
                            if (rootPrefix == null && rawName.isNotEmpty()) {
                                val firstSlash = rawName.indexOf('/')
                                if (firstSlash > 0) {
                                    val topDir = rawName.substring(0, firstSlash)
                                    val subPath = rawName.substring(firstSlash + 1).trim('/')
                                    val subFirst = subPath.substringBefore('/')
                                    if (topDir !in standardRootDirs && subFirst in standardRootDirs) {
                                        rootPrefix = "$topDir/"
                                        Log.i(tag, "Detected archive root wrapper prefix: $rootPrefix")
                                    }
                                } else if (entry.isDirectory && rawName !in standardRootDirs) {
                                    rootPrefix = "$rawName/"
                                    Log.i(tag, "Detected archive root wrapper directory: $rootPrefix")
                                }
                            }

                            // If this entry is the wrapper directory itself, skip creating it
                            if (rootPrefix != null && (entryName == rootPrefix || entryName == rootPrefix.removeSuffix("/"))) {
                                entry = tarIn.nextEntry
                                continue
                            }

                            // Strip wrapper directory prefix
                            if (rootPrefix != null && entryName.startsWith(rootPrefix!!)) {
                                entryName = entryName.removePrefix(rootPrefix!!)
                            }

                            if (entryName.isNotEmpty() && entryName != ".") {
                                val targetFile = File(destDir, entryName)

                                // Guard against Zip Slip / path traversal
                                val canonicalTarget = try { targetFile.canonicalFile } catch (e: Exception) { targetFile }
                                val canonicalTargetPath = canonicalTarget.canonicalPath

                                if (!canonicalTargetPath.startsWith(canonicalDest + File.separator) &&
                                    canonicalTargetPath != canonicalDest) {
                                    throw SecurityException("Illegal archive path traversal: ${entry.name}")
                                }

                                try {
                                    if (entry.isDirectory) {
                                        targetFile.mkdirs()
                                        canonicalTarget.mkdirs()
                                    } else if (entry.isSymbolicLink) {
                                        canonicalTarget.parentFile?.mkdirs()
                                        targetFile.parentFile?.mkdirs()
                                        targetFile.delete()
                                        val linkTarget = if (rootPrefix != null && entry.linkName.startsWith(rootPrefix!!)) {
                                            entry.linkName.removePrefix(rootPrefix!!)
                                        } else {
                                            entry.linkName
                                        }
                                        createSymbolicLink(linkTarget, targetFile)
                                    } else if (entry.isLink) {
                                        canonicalTarget.parentFile?.mkdirs()
                                        targetFile.parentFile?.mkdirs()
                                        val rawLinkName = if (rootPrefix != null && entry.linkName.startsWith(rootPrefix!!)) {
                                            entry.linkName.removePrefix(rootPrefix!!)
                                        } else {
                                            entry.linkName
                                        }
                                        val linkTargetFile = File(destDir, rawLinkName.removePrefix("./"))
                                        createHardLink(linkTargetFile, targetFile)
                                    } else {
                                        canonicalTarget.parentFile?.mkdirs()
                                        targetFile.parentFile?.mkdirs()
                                        targetFile.delete()
                                        FileOutputStream(targetFile).use { fos ->
                                            tarIn.copyTo(fos)
                                        }
                                        applyPermissions(entry.mode, targetFile)
                                        applyPermissions(entry.mode, canonicalTarget)
                                    }
                                } catch (fileEx: Exception) {
                                    Log.w(tag, "Failed to extract entry $entryName: ${fileEx.message}")
                                }

                                extractedCount++
                                if (extractedCount % 400 == 0) {
                                    onProgress(
                                        0.55f + ((extractedCount % 10000) / 10000f) * 0.35f,
                                        "Extracting system rootfs: $extractedCount files..."
                                    )
                                }
                            }
                            entry = tarIn.nextEntry
                        }
                    }
                }
            }

            // Fallback: Check if rootfs files were extracted into a nested subdirectory and promote them
            flattenNestedRootfs(destDir)

            // 1. Verify and repair /bin/bash
            val bash = File(destDir, "bin/bash")
            val usrBash = File(destDir, "usr/bin/bash")

            // Case A: /usr/bin/bash exists but /bin/bash doesn't (or is broken)
            if (usrBash.exists() && usrBash.length() > 0) {
                if (!bash.exists() || bash.length() == 0L) {
                    try {
                        // Delete any broken symlink first
                        if (bash.exists() || !bash.canonicalPath.endsWith("bash")) {
                            bash.delete()
                        }
                        // Create parent dirs and a proper absolute symlink
                        bash.parentFile?.mkdirs()
                        val targetPath = usrBash.absolutePath
                        createSymbolicLink(targetPath, bash)
                        Log.i(tag, "Repaired /bin/bash -> $targetPath")
                    } catch (e: Throwable) {
                        // Fallback: copy the physical file instead of symlinking
                        Log.w(tag, "Symlink repair failed, falling back to copy: ${e.message}")
                        try {
                            usrBash.copyTo(bash, overwrite = true)
                        } catch (copyEx: Throwable) {
                            Log.e(tag, "Failed copy fallback for bash: ${copyEx.message}")
                        }
                    }
                }
            }

            // Case B: /bin/bash exists but /usr/bin/bash doesn't
            if (bash.exists() && bash.length() > 0) {
                if (!usrBash.exists() || usrBash.length() == 0L) {
                    try {
                        usrBash.parentFile?.mkdirs()
                        bash.copyTo(usrBash, overwrite = true)
                        Log.i(tag, "Repaired /usr/bin/bash from /bin/bash")
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to repair /usr/bin/bash: ${e.message}")
                    }
                }
            }

            // Case C: Fallback to /bin/sh or /usr/bin/sh if bash is missing
            if ((!bash.exists() || bash.length() == 0L) && (!usrBash.exists() || usrBash.length() == 0L)) {
                val sh = File(destDir, "bin/sh")
                val usrSh = File(destDir, "usr/bin/sh")
                val validSh = when {
                    usrSh.exists() && usrSh.length() > 0 -> usrSh
                    sh.exists() && sh.length() > 0 -> sh
                    else -> null
                }
                if (validSh != null) {
                    Log.i(tag, "Bash binary missing, bootstrapping from ${validSh.name}...")
                    try {
                        bash.parentFile?.mkdirs()
                        validSh.copyTo(bash, overwrite = true)
                        usrBash.parentFile?.mkdirs()
                        validSh.copyTo(usrBash, overwrite = true)
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to bootstrap bash from sh: ${e.message}")
                    }
                }
            }

            // 2. Final validation
            val isBashValid = (bash.exists() && bash.length() > 0) || (usrBash.exists() && usrBash.length() > 0)

            if (isBashValid) {
                // Ensure executable permissions on all shell binaries
                if (bash.exists()) bash.setExecutable(true, false)
                if (usrBash.exists()) usrBash.setExecutable(true, false)
                val sh = File(destDir, "bin/sh")
                val usrSh = File(destDir, "usr/bin/sh")
                if (sh.exists()) sh.setExecutable(true, false)
                if (usrSh.exists()) usrSh.setExecutable(true, false)

                onProgress(0.92f, "Rootfs extracted and verified (/bin/bash available)")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Archive extracted ($extractedCount files) but /bin/bash was not found"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Extraction failed", e)
            Result.failure(e)
        }
    }

    fun createSymbolicLink(linkTarget: String, linkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                java.nio.file.Files.createSymbolicLink(
                    linkFile.toPath(),
                    java.nio.file.Paths.get(linkTarget)
                )
                return
            } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.symlink(linkTarget, linkFile.absolutePath)
                return
            } catch (e: Throwable) {
                Log.d(tag, "Os.symlink failed for $linkTarget -> ${linkFile.name}: ${e.message}")
            }
        }
        try {
            Runtime.getRuntime().exec(arrayOf("ln", "-s", linkTarget, linkFile.absolutePath)).waitFor()
        } catch (_: Throwable) {}
    }

    fun createHardLink(srcFile: File, linkFile: File) {
        try {
            linkFile.parentFile?.mkdirs()
            linkFile.delete()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    java.nio.file.Files.createLink(linkFile.toPath(), srcFile.toPath())
                    return
                } catch (_: Throwable) {}
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    Os.link(srcFile.absolutePath, linkFile.absolutePath)
                    return
                } catch (e: Throwable) {
                    Log.w(tag, "Os.link failed for ${srcFile.name} -> ${linkFile.name}: ${e.message}")
                }
            }
            if (srcFile.exists()) {
                srcFile.copyTo(linkFile, overwrite = true)
            } else {
                createSymbolicLink(srcFile.name, linkFile)
            }
        } catch (e: Throwable) {
            Log.w(tag, "Failed to create hard link ${linkFile.name}: ${e.message}")
        }
    }

    private fun applyPermissions(mode: Int, file: File) {
        if (mode and 0b001_000_000 != 0 || file.parentFile?.name == "bin" || file.parentFile?.name == "sbin") {
            file.setExecutable(true, false)
        }
    }

    /**
     * If the rootfs was extracted into a single nested wrapper folder, promote all contents
     * to the destDir so PRoot can locate /bin, /usr, /etc at root.
     */
    fun flattenNestedRootfs(destDir: File) {
        val topBash = File(destDir, "bin/bash")
        val topUsrBash = File(destDir, "usr/bin/bash")
        if ((topBash.exists() && topBash.length() > 0) || (topUsrBash.exists() && topUsrBash.length() > 0)) {
            return
        }

        val subDirs = destDir.listFiles()?.filter { it.isDirectory } ?: return
        for (sub in subDirs) {
            val subBash = File(sub, "bin/bash")
            val subUsrBash = File(sub, "usr/bin/bash")
            val subSh = File(sub, "bin/sh")
            val subUsrSh = File(sub, "usr/bin/sh")
            val hasShell = (subBash.exists() && subBash.length() > 0) ||
                    (subUsrBash.exists() && subUsrBash.length() > 0) ||
                    (subSh.exists() && subSh.length() > 0) ||
                    (subUsrSh.exists() && subUsrSh.length() > 0)

            if (hasShell) {
                Log.i(tag, "Found nested rootfs in ${sub.name}, promoting files to ${destDir.name}...")
                moveDirectoryContents(sub, destDir)
                sub.deleteRecursively()
                break
            }
        }
    }

    private fun moveDirectoryContents(sourceDir: File, targetDir: File) {
        val children = sourceDir.listFiles() ?: return
        for (child in children) {
            val dest = File(targetDir, child.name)
            if (child.isDirectory) {
                if (!dest.exists()) {
                    if (!child.renameTo(dest)) {
                        dest.mkdirs()
                        moveDirectoryContents(child, dest)
                        child.delete()
                    }
                } else {
                    moveDirectoryContents(child, dest)
                    child.delete()
                }
            } else {
                dest.delete()
                if (!child.renameTo(dest)) {
                    try {
                        child.copyTo(dest, overwrite = true)
                        child.delete()
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to move ${child.name} to ${dest.name}: ${e.message}")
                    }
                }
            }
        }
    }
}
