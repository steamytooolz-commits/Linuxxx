package com.example.core

import android.content.Context
import android.system.Os
import android.system.OsConstants
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * High-performance, symlink-aware Linux bootstrap archive unpacker.
 * Reconstructs POSIX symbolic links via android.system.Os.symlink and applies
 * executable permissions (chmod 755) to all binary executables.
 */
class BootstrapExtractor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BootstrapExtractor"
        const val ASSET_NAME = "bootstrap-aarch64.zip"
        const val EXTRACTION_MARKER = ".bootstrap_extracted_v1"
        private const val S_IFLNK = 0b1010000000000000 // 0120000 octal (POSIX symlink)
        private const val S_IFMT = 0b1111000000000000  // File type bitmask
    }

    data class ExtractionProgress(
        val isExtracting: Boolean = false,
        val filesExtracted: Int = 0,
        val totalFiles: Int = 0,
        val currentFile: String = "",
        val isCompleted: Boolean = false,
        val errorMessage: String? = null
    )

    data class SymlinkRecord(
        val target: String,
        val linkPath: String
    )

    val prefixDir: File
        get() = File(context.filesDir, "usr")

    val homeDir: File
        get() = File(context.filesDir, "home")

    val markerFile: File
        get() = File(prefixDir, EXTRACTION_MARKER)

    fun isExtracted(): Boolean {
        return markerFile.exists() && prefixDir.isDirectory && File(prefixDir, "bin").isDirectory
    }

    /**
     * Unpacks bootstrap-aarch64.zip into context.filesDir/usr, handling POSIX permissions
     * and reconstructing symbolic links via android.system.Os.symlink.
     */
    suspend fun extractBootstrap(
        force: Boolean = false,
        onProgress: (ExtractionProgress) -> Unit = {}
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!force && isExtracted()) {
            Log.i(TAG, "Bootstrap already extracted at ${prefixDir.absolutePath}")
            onProgress(
                ExtractionProgress(
                    isExtracting = false,
                    isCompleted = true,
                    currentFile = "Already extracted"
                )
            )
            return@withContext Result.success(0)
        }

        try {
            onProgress(ExtractionProgress(isExtracting = true, currentFile = "Preparing environment..."))

            // 1. Prepare target directories
            if (!prefixDir.exists()) prefixDir.mkdirs()
            if (!homeDir.exists()) homeDir.mkdirs()

            val binDir = File(prefixDir, "bin")
            val libDir = File(prefixDir, "lib")
            val etcDir = File(prefixDir, "etc")
            val tmpDir = File(prefixDir, "tmp")
            binDir.mkdirs()
            libDir.mkdirs()
            etcDir.mkdirs()
            tmpDir.mkdirs()

            // Prepare Home database storage subdirectories
            File(homeDir, "mysql_data").mkdirs()
            File(homeDir, "redis_data").mkdirs()
            File(homeDir, "mongo_data").mkdirs()

            val symlinkRecords = mutableListOf<SymlinkRecord>()
            var extractedCount = 0

            // 2. Open input stream (from assets or download)
            val assetStream: InputStream = try {
                context.assets.open(ASSET_NAME)
            } catch (e: Exception) {
                onProgress(ExtractionProgress(isExtracting = true, currentFile = "Downloading Termux bootstrap via HTTP..."))
                
                // Determine architecture dynamically
                val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
                val arch = when {
                    abi.contains("x86_64") -> "x86_64"
                    abi.contains("x86") -> "i686"
                    abi.contains("armeabi-v7a") -> "arm"
                    else -> "aarch64"
                }
                
                val url = java.net.URL("https://github.com/termux/termux-packages/releases/latest/download/bootstrap-$arch.zip")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()
                
                var input = connection.inputStream
                var status = connection.responseCode
                var currentConn = connection
                
                while (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    val redirectUrl = currentConn.getHeaderField("Location")
                    currentConn = java.net.URL(redirectUrl).openConnection() as java.net.HttpURLConnection
                    currentConn.connect()
                    status = currentConn.responseCode
                    input = currentConn.inputStream
                }
                
                // Read fully to file to avoid timeout/stream issues
                val zipFile = File(context.filesDir, "bootstrap_downloaded.zip")
                if (!zipFile.exists() || zipFile.length() < 1000000) {
                    FileOutputStream(zipFile).use { out ->
                        input.copyTo(out)
                    }
                }
                input.close()
                java.io.FileInputStream(zipFile)
            }
            
            ZipInputStream(assetStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    // Ignore directory entries or strip parent directories if prefixed
                    val cleanPath = entryName.removePrefix("usr/").removePrefix("./")

                    if (cleanPath.isNotEmpty()) {
                        val destinationFile = File(prefixDir, cleanPath)

                        if (entry.isDirectory) {
                            destinationFile.mkdirs()
                        } else {
                            // Ensure parent exists
                            destinationFile.parentFile?.mkdirs()

                            // Check for custom manifest / SYMLINKS.txt
                            if (cleanPath == "SYMLINKS.txt" || cleanPath.endsWith("/SYMLINKS.txt")) {
                                parseSymlinksManifest(zipIn, symlinkRecords)
                            } else {
                                // Check if entry extra fields indicate a POSIX symlink
                                val isSymlinkByHeader = isSymlinkEntry(entry)

                                if (isSymlinkByHeader) {
                                    val target = readEntryString(zipIn)
                                    symlinkRecords.add(SymlinkRecord(target = target.trim(), linkPath = cleanPath))
                                } else {
                                    // Regular file extraction
                                    FileOutputStream(destinationFile).use { out ->
                                        zipIn.copyTo(out)
                                    }
                                    extractedCount++
                                }
                            }
                        }
                    }

                    onProgress(
                        ExtractionProgress(
                            isExtracting = true,
                            filesExtracted = extractedCount,
                            currentFile = cleanPath
                        )
                    )
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // 3. Reconstruct POSIX Symbolic Links using android.system.Os.symlink
            for (record in symlinkRecords) {
                val linkFile = File(prefixDir, record.linkPath)
                if (linkFile.exists() || isBrokenSymlink(linkFile)) {
                    linkFile.delete()
                }
                try {
                    Os.symlink(record.target, linkFile.absolutePath)
                    Log.d(TAG, "Created symlink: ${linkFile.absolutePath} -> ${record.target}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create Os.symlink for ${linkFile.absolutePath} -> ${record.target}: ${e.message}")
                    // Fallback for non-privileged links: copy target file if target is reachable
                    fallbackSymlinkCopy(linkFile, record.target)
                }
            }

            // 4. Execute chmod 755 on all binaries inside usr/bin
            val binFiles = binDir.listFiles()
            if (binFiles != null) {
                for (file in binFiles) {
                    if (file.isFile) {
                        try {
                            Os.chmod(file.absolutePath, 493) // 0755 in octal
                        } catch (e: Exception) {
                            Log.w(TAG, "Os.chmod failed on ${file.name}: ${e.message}")
                        }

                        // Mandatory execution per prompt specification:
                        try {
                            val chmodProcess = Runtime.getRuntime().exec("chmod 755 " + file.absolutePath)
                            chmodProcess.waitFor()
                            Log.v(TAG, "chmod 755 completed for ${file.absolutePath}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Runtime exec chmod failed for ${file.absolutePath}", e)
                        }
                    }
                }
            }

            // Write extraction marker
            markerFile.writeText("extracted_timestamp=${System.currentTimeMillis()}\nfiles=$extractedCount\nsymlinks=${symlinkRecords.size}\n")

            onProgress(
                ExtractionProgress(
                    isExtracting = false,
                    filesExtracted = extractedCount,
                    totalFiles = extractedCount,
                    currentFile = "Extraction complete",
                    isCompleted = true
                )
            )

            Result.success(extractedCount)
        } catch (e: Throwable) {
            Log.e(TAG, "Bootstrap extraction failed", e)
            onProgress(
                ExtractionProgress(
                    isExtracting = false,
                    isCompleted = false,
                    errorMessage = e.message ?: "Unknown extraction error"
                )
            )
            Result.failure(e)
        }
    }

    /**
     * Parses Termux standard SYMLINKS.txt format: target←linkPath or target linkPath
     */
    private fun parseSymlinksManifest(
        inputStream: InputStream,
        symlinkRecords: MutableList<SymlinkRecord>
    ) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                if (trimmed.contains("←")) {
                    val parts = trimmed.split("←")
                    if (parts.size >= 2) {
                        symlinkRecords.add(SymlinkRecord(target = parts[0].trim(), linkPath = parts[1].trim()))
                    }
                } else if (trimmed.contains("->")) {
                    val parts = trimmed.split("->")
                    if (parts.size >= 2) {
                        symlinkRecords.add(SymlinkRecord(linkPath = parts[0].trim(), target = parts[1].trim()))
                    }
                } else if (trimmed.contains("\t") || trimmed.contains(" ")) {
                    val parts = trimmed.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        symlinkRecords.add(SymlinkRecord(target = parts[0].trim(), linkPath = parts[1].trim()))
                    }
                }
            }
            line = reader.readLine()
        }
    }

    /**
     * Identifies standard Unix symlink headers from ZipEntry extra fields.
     */
    private fun isSymlinkEntry(entry: ZipEntry): Boolean {
        val extra = entry.extra ?: return false
        // Check for Info-ZIP Unix extra field (Header ID 0x7875 or 0x5855 or 0x000d)
        var i = 0
        while (i + 4 <= extra.size) {
            val headerId = (extra[i].toInt() and 0xFF) or ((extra[i + 1].toInt() and 0xFF) shl 8)
            val dataSize = (extra[i + 2].toInt() and 0xFF) or ((extra[i + 3].toInt() and 0xFF) shl 8)
            if (headerId == 0x7875 || headerId == 0x5855) {
                // Unix extra field found
                if (dataSize >= 4 && i + 4 + dataSize <= extra.size) {
                    // Check if Unix file mode indicates S_IFLNK
                    val modeByte1 = extra[i + 4].toInt() and 0xFF
                    val modeByte2 = extra[i + 5].toInt() and 0xFF
                    val mode = modeByte1 or (modeByte2 shl 8)
                    if ((mode and S_IFMT) == S_IFLNK) {
                        return true
                    }
                }
            }
            i += 4 + dataSize
        }
        return false
    }

    private fun readEntryString(inputStream: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(512)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            out.write(buffer, 0, length)
        }
        return out.toString("UTF-8")
    }

    private fun isBrokenSymlink(file: File): Boolean {
        return try {
            val stat = Os.lstat(file.absolutePath)
            OsConstants.S_ISLNK(stat.st_mode) && !file.exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun fallbackSymlinkCopy(linkFile: File, target: String) {
        try {
            val targetFile = if (target.startsWith("/")) File(target) else File(linkFile.parentFile, target)
            if (targetFile.exists() && targetFile.isFile) {
                targetFile.copyTo(linkFile, overwrite = true)
                linkFile.setExecutable(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback symlink copy failed for ${linkFile.absolutePath}", e)
        }
    }
}
