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

            val isXz = archiveFile.name.endsWith(".xz", ignoreCase = true)
            val isGz = archiveFile.name.endsWith(".gz", ignoreCase = true) || archiveFile.name.endsWith(".tgz", ignoreCase = true)

            FileInputStream(archiveFile).use { fis ->
                BufferedInputStream(fis, 128 * 1024).use { bis ->
                    val decompressor: InputStream = when {
                        isXz -> XZCompressorInputStream(bis)
                        isGz -> GzipCompressorInputStream(bis)
                        else -> bis
                    }

                    TarArchiveInputStream(decompressor).use { tarIn ->
                        var entry: TarArchiveEntry? = tarIn.nextEntry
                        var extractedCount = 0
                        val canonicalDest = destDir.canonicalPath

                        while (entry != null) {
                            val entryName = entry.name.removePrefix("./")
                            if (entryName.isNotEmpty() && entryName != ".") {
                                val targetFile = File(destDir, entryName)

                                // Guard against Zip Slip / path traversal
                                if (!targetFile.canonicalPath.startsWith(canonicalDest)) {
                                    throw SecurityException("Illegal archive path traversal: ${entry.name}")
                                }

                                if (entry.isDirectory) {
                                    targetFile.mkdirs()
                                } else if (entry.isSymbolicLink) {
                                    targetFile.parentFile?.mkdirs()
                                    targetFile.delete()
                                    createSymbolicLink(entry.linkName, targetFile)
                                } else {
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { fos ->
                                        tarIn.copyTo(fos)
                                    }
                                    applyPermissions(entry.mode, targetFile)
                                }

                                extractedCount++
                                if (extractedCount % 400 == 0) {
                                    onProgress(
                                        0.55f + ((extractedCount % 5000) / 5000f) * 0.35f,
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
            if (bash.exists() || usrBash.exists()) {
                bash.setExecutable(true, false)
                usrBash.setExecutable(true, false)
                onProgress(0.92f, "Rootfs extracted and verified (/bin/bash available)")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Archive extracted but /bin/bash was not found"))
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

    private fun applyPermissions(mode: Int, file: File) {
        if (mode and 0b001_000_000 != 0 || file.parentFile?.name == "bin" || file.parentFile?.name == "sbin") {
            file.setExecutable(true, false)
        }
    }
}
