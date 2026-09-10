package com.example.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Handles locating, extracting, and verifying the ARM64 PRoot binary with execution permissions.
 * Provides detailed, transparent logging and errors identifying the exact binary source.
 */
class ProotInstaller(private val context: Context) {

    private val tag = "ProotInstaller"

    val prootBinary: File = File(context.filesDir, "proot")

    fun isInstalled(): Boolean {
        return prootBinary.exists() && prootBinary.canExecute()
    }

    fun install(): Result<File> {
        val checkedPaths = mutableListOf<String>()
        var resolvedSource: String? = null

        try {
            // Source 1: Native library directory (standard APK extraction)
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val sourceSo = File(nativeLibDir, "libproot.so")
            checkedPaths.add(sourceSo.absolutePath)

            if (sourceSo.exists() && sourceSo.length() > 0) {
                sourceSo.copyTo(prootBinary, overwrite = true)
                // Also copy loaders
                val loader1 = File(nativeLibDir, "libloader.so")
                if (loader1.exists()) loader1.copyTo(File(context.filesDir, "libloader.so"), overwrite = true)
                val loader2 = File(nativeLibDir, "libloader_m32.so")
                if (loader2.exists()) loader2.copyTo(File(context.filesDir, "libloader_m32.so"), overwrite = true)
                
                resolvedSource = "nativeLibraryDir (${sourceSo.absolutePath})"
                Log.i(tag, "Extracted proot from $resolvedSource")
            } else {
                // Source 2: Parent ABI lib directory
                val abiDir = File(context.filesDir.parentFile, "lib")
                val altSo = File(abiDir, "libproot.so")
                checkedPaths.add(altSo.absolutePath)

                if (altSo.exists() && altSo.length() > 0) {
                    altSo.copyTo(prootBinary, overwrite = true)
                    // Also copy loaders
                    val loader1 = File(abiDir, "libloader.so")
                    if (loader1.exists()) loader1.copyTo(File(context.filesDir, "libloader.so"), overwrite = true)
                    val loader2 = File(abiDir, "libloader_m32.so")
                    if (loader2.exists()) loader2.copyTo(File(context.filesDir, "libloader_m32.so"), overwrite = true)

                    resolvedSource = "parentLibDir (${altSo.absolutePath})"
                    Log.i(tag, "Extracted proot from $resolvedSource")
                } else {
                    // Source 3: Assets fallback (assets/libproot.so or assets/proot)
                    checkedPaths.add("assets/libproot.so")
                    try {
                        context.assets.open("libproot.so").use { input ->
                            FileOutputStream(prootBinary).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (prootBinary.exists() && prootBinary.length() > 0) {
                            resolvedSource = "assets/libproot.so"
                            Log.i(tag, "Extracted proot from $resolvedSource")
                        }
                    } catch (assetEx: Exception) {
                        val assetWarn = "Asset libproot.so not found or extraction failed: ${assetEx.message}"
                        Log.w(tag, assetWarn, assetEx)
                        checkedPaths.add("assets/libproot.so (failed: ${assetEx.message})")
                    }
                }
            }

            if (resolvedSource == null && !prootBinary.exists()) {
                Log.i(tag, "Proot binary local source could not be found. Trying network download fallback...")
                val downloaded = downloadProotFallback()
                if (downloaded) {
                    resolvedSource = "network download fallback"
                } else {
                    val errorMsg = "Proot binary source could not be found locally or via download. Checked locations: ${checkedPaths.joinToString("; ")}. Ensure native ARM64 library or asset is packaged or internet is available."
                    Log.e(tag, errorMsg)
                    return Result.failure(IllegalStateException(errorMsg))
                }
            }

            // Apply read and execute permissions for userland
            prootBinary.setReadable(true, false)
            prootBinary.setExecutable(true, false)

            try {
                val chmodExit = Runtime.getRuntime().exec(arrayOf("chmod", "755", prootBinary.absolutePath)).waitFor()
                if (chmodExit != 0) {
                    Log.w(tag, "chmod 755 exited with code $chmodExit for ${prootBinary.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w(tag, "chmod 755 execution error: ${e.message}")
            }

            if (prootBinary.exists() && prootBinary.canExecute()) {
                Log.i(tag, "PRoot binary verified successfully from source '$resolvedSource' at: ${prootBinary.absolutePath} (size: ${prootBinary.length()} bytes)")
                return Result.success(prootBinary)
            } else {
                val errorDetails = "PRoot binary exists=${prootBinary.exists()} canExecute=${prootBinary.canExecute()} size=${if (prootBinary.exists()) prootBinary.length() else 0} bytes from source '$resolvedSource'. Checked: [${checkedPaths.joinToString(", ")}]"
                Log.e(tag, errorDetails)
                return Result.failure(IllegalStateException(errorDetails))
            }
        } catch (e: Exception) {
            val fullError = "Failed to install PRoot binary. Checked paths: [${checkedPaths.joinToString(", ")}]. Cause: ${e.message}"
            Log.e(tag, fullError, e)
            return Result.failure(IllegalStateException(fullError, e))
        }
    }

    private fun downloadProotFallback(): Boolean {
        val urls = listOf(
            "https://skirsten.github.io/proot-portable-android-binaries/aarch64/proot",
            "https://raw.githubusercontent.com/skirsten/proot-portable-android-binaries/master/aarch64/proot"
        )
        for (urlStr in urls) {
            try {
                Log.i(tag, "Attempting to download PRoot fallback from $urlStr")
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                if (connection.responseCode == 200) {
                    connection.inputStream.use { input ->
                        FileOutputStream(prootBinary).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (prootBinary.exists() && prootBinary.length() > 0) {
                        Log.i(tag, "Successfully downloaded fallback PRoot binary")
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to download from $urlStr: ${e.message}")
            }
        }
        return false
    }
}
