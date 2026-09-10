package com.example.core

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles downloading Ubuntu 24.04 ARM64 root filesystem archives with redirects and fallback mirrors.
 */
class RootfsDownloader {

    private val tag = "RootfsDownloader"

    // Prioritized list of known-working Ubuntu 24.04 ARM64 rootfs URLs
    val candidateUrls = listOf(
        "https://github.com/termux/proot-distro/releases/download/v4.11.0/ubuntu-noble-aarch64-pd-v4.11.0.tar.xz",
        "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz",
        "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.4-base-arm64.tar.gz",
        "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-arm64.tar.gz",
        "https://mirrors.ustc.edu.cn/ubuntu-cdimage/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz",
        "https://mirror.sjtu.edu.cn/ubuntu-cdimage/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz",
        "https://mirror.termux.dev/ubuntu-noble-aarch64-pd-v4.11.0.tar.xz"
    )

    fun download(
        destFile: File,
        onProgress: (Float, String) -> Unit
    ): Result<File> {
        val tempFile = File(destFile.parentFile, "${destFile.name}.download.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        var lastError: Exception? = null

        for (urlStr in candidateUrls) {
            try {
                Log.i(tag, "Attempting rootfs download from: $urlStr")
                val hostName = try { URL(urlStr).host } catch (_: Exception) { "mirror" }
                onProgress(0.05f, "Connecting to mirror ($hostName)...")

                val downloaded = downloadFromUrl(urlStr, tempFile, onProgress)
                if (downloaded) {
                    if (destFile.exists()) {
                        destFile.delete()
                    }
                    if (tempFile.renameTo(destFile)) {
                        val mbSize = destFile.length() / (1024 * 1024)
                        onProgress(0.50f, "Rootfs archive downloaded successfully ($mbSize MB)")
                        return Result.success(destFile)
                    } else {
                        tempFile.copyTo(destFile, overwrite = true)
                        tempFile.delete()
                        return Result.success(destFile)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Download failed from $urlStr: ${e.message}")
                lastError = e
            }
        }

        return Result.failure(lastError ?: IllegalStateException("All candidate rootfs URLs failed to download"))
    }

    private fun downloadFromUrl(
        urlStr: String,
        tempFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        var currentUrl = urlStr
        var redirects = 0
        val maxRedirects = 5

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 20_000
            conn.readTimeout = 40_000
            conn.setRequestProperty("User-Agent", "Linuxxx-Appliance/1.0 (Android; ARM64)")

            val responseCode = conn.responseCode
            if (responseCode in 300..399) {
                val newUrl = conn.getHeaderField("Location") ?: return false
                currentUrl = if (newUrl.startsWith("http")) newUrl else URL(url, newUrl).toString()
                conn.disconnect()
                redirects++
                continue
            }

            if (responseCode !in 200..299) {
                conn.disconnect()
                throw IllegalStateException("HTTP response code $responseCode from $currentUrl")
            }

            val totalBytes = conn.contentLengthLong
            val buffer = ByteArray(64 * 1024)
            var bytesReadTotal = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        output.write(buffer, 0, n)
                        bytesReadTotal += n
                        if (totalBytes > 0) {
                            val fraction = bytesReadTotal.toFloat() / totalBytes.toFloat()
                            val mappedProgress = 0.05f + (fraction * 0.45f)
                            val mbDownloaded = bytesReadTotal / (1024 * 1024)
                            val mbTotal = totalBytes / (1024 * 1024)
                            onProgress(mappedProgress, "Downloading Ubuntu ARM64: $mbDownloaded / $mbTotal MB")
                        }
                    }
                }
            }
            conn.disconnect()
            return tempFile.exists() && tempFile.length() > 1024 * 1024
        }
        return false
    }
}
