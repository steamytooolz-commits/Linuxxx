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
                            val entryName = entry.name.removePrefix("./")
                            if (entryName.isNotEmpty() && entryName != ".") {
                                val targetFile = File(destDir, entryName)

                                // Guard against Zip Slip / path traversal
                                val canonicalTarget = try { targetFile.canonicalFile } catch (e: Exception) { targetFile }

                                if (!canonicalTarget.canonicalPath.startsWith(canonicalDest) &&
                                    !targetFile.absolutePath.startsWith(canonicalDest)) {
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
                                        createSymbolicLink(entry.linkName, targetFile)
                                    } else if (entry.isLink) {
                                        canonicalTarget.parentFile?.mkdirs()
                                        targetFile.parentFile?.mkdirs()
                                        val linkTargetFile = File(destDir, entry.linkName.removePrefix("./"))
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

            // Verify essential bash executable exists
            val bash = File(destDir, "bin/bash")
            val usrBash = File(destDir, "usr/bin/bash")

            // Auto-repair missing links between /bin/bash and /usr/bin/bash
            if (!bash.exists() && usrBash.exists()) {
                try {
                    val binDir = File(destDir, "bin")
                    if (!binDir.exists()) binDir.mkdirs()
                    createSymbolicLink("usr/bin/bash", bash)
                    if (!bash.exists()) {
                        usrBash.copyTo(bash, overwrite = true)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed bash symlink repair: ${e.message}")
                }
            }

            if (!usrBash.exists() && bash.exists()) {
                try {
                    val usrBinDir = File(destDir, "usr/bin")
                    if (!usrBinDir.exists()) usrBinDir.mkdirs()
                    bash.copyTo(usrBash, overwrite = true)
                } catch (e: Exception) {
                    Log.w(tag, "Failed usrBash copy repair: ${e.message}")
                }
            }

            val isBashValid = (bash.exists() && bash.length() > 0) || (usrBash.exists() && usrBash.length() > 0)
            if (isBashValid) {
                if (bash.exists()) bash.setExecutable(true, false)
                if (usrBash.exists()) usrBash.setExecutable(true, false)
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

    private fun createSymbolicLink(linkTarget: String, linkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.symlink(linkTarget, linkFile.absolutePath)
                return
            } catch (e: Exception) {
                Log.d(tag, "Os.symlink failed for $linkTarget -> ${linkFile.name}: ${e.message}")
            }
        }
        try {
            Runtime.getRuntime().exec(arrayOf("ln", "-s", linkTarget, linkFile.absolutePath)).waitFor()
        } catch (ignored: Exception) {}
    }

    private fun createHardLink(srcFile: File, linkFile: File) {
        try {
            linkFile.parentFile?.mkdirs()
            linkFile.delete()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    Os.link(srcFile.absolutePath, linkFile.absolutePath)
                    return
                } catch (e: Exception) {
                    Log.w(tag, "Os.link failed for ${srcFile.name} -> ${linkFile.name}: ${e.message}")
                }
            }
            if (srcFile.exists()) {
                srcFile.copyTo(linkFile, overwrite = true)
            } else {
                createSymbolicLink(srcFile.name, linkFile)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to create hard link ${linkFile.name}: ${e.message}")
        }
    }

    private fun applyPermissions(mode: Int, file: File) {
        if (mode and 0b001_000_000 != 0 || file.parentFile?.name == "bin" || file.parentFile?.name == "sbin") {
            file.setExecutable(true, false)
        }
    }
}
