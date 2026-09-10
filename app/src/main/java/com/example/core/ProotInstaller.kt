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

    fun getExecutableProot(): File {
        val nativeLib = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
        if (nativeLib.exists() && nativeLib.canExecute()) {
            return nativeLib
        }
        val parentLib = File(context.filesDir.parentFile, "lib/libproot.so")
        if (parentLib.exists() && parentLib.canExecute()) {
            return parentLib
        }
        return prootBinary
    }

    fun getLoaderPath(): String {
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val l1 = File(nativeLibDir, "libloader.so")
        if (l1.exists()) return l1.absolutePath
        val parentLib = File(context.filesDir.parentFile, "lib/libloader.so")
        if (parentLib.exists()) return parentLib.absolutePath
        val f1 = File(context.filesDir, "libloader.so")
        if (f1.exists()) return f1.absolutePath
        return l1.absolutePath
    }

    fun getLoader32Path(): String {
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val l2 = File(nativeLibDir, "libloader_m32.so")
        if (l2.exists()) return l2.absolutePath
        val parentLib = File(context.filesDir.parentFile, "lib/libloader_m32.so")
        if (parentLib.exists()) return parentLib.absolutePath
        val f2 = File(context.filesDir, "libloader_m32.so")
        if (f2.exists()) return f2.absolutePath
        return l2.absolutePath
    }

    fun isInstalled(): Boolean {
        return getExecutableProot().exists() && getExecutableProot().canExecute()
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
                // If it can be executed directly (API 29+ requirement), we use it directly!
                if (sourceSo.canExecute()) {
                    resolvedSource = "nativeLibraryDir direct (${sourceSo.absolutePath})"
                    Log.i(tag, "Using executable proot directly from $resolvedSource")
                    return Result.success(sourceSo)
                }
                // Fallback copy for compatibility
                try {
                    sourceSo.copyTo(prootBinary, overwrite = true)
                    prootBinary.setReadable(true, false)
                    prootBinary.setExecutable(true, false)
                    val loader1 = File(nativeLibDir, "libloader.so")
                    if (loader1.exists()) loader1.copyTo(File(context.filesDir, "libloader.so"), overwrite = true)
                    val loader2 = File(nativeLibDir, "libloader_m32.so")
                    if (loader2.exists()) loader2.copyTo(File(context.filesDir, "libloader_m32.so"), overwrite = true)
                    resolvedSource = "extracted nativeLibraryDir to filesDir"
                } catch (e: Exception) {
                    Log.w(tag, "Failed fallback copy from nativeLibDir: ${e.message}")
                }
            }

            if (resolvedSource == null) {
                // Source 2: Parent ABI lib directory
                val abiDir = File(context.filesDir.parentFile, "lib")
                val altSo = File(abiDir, "libproot.so")
                checkedPaths.add(altSo.absolutePath)

                if (altSo.exists() && altSo.length() > 0) {
                    if (altSo.canExecute()) {
                        resolvedSource = "parentLibDir direct (${altSo.absolutePath})"
                        Log.i(tag, "Using executable proot directly from $resolvedSource")
                        return Result.success(altSo)
                    }
                    try {
                        altSo.copyTo(prootBinary, overwrite = true)
                        prootBinary.setReadable(true, false)
                        prootBinary.setExecutable(true, false)
                        val loader1 = File(abiDir, "libloader.so")
                        if (loader1.exists()) loader1.copyTo(File(context.filesDir, "libloader.so"), overwrite = true)
                        val loader2 = File(abiDir, "libloader_m32.so")
                        if (loader2.exists()) loader2.copyTo(File(context.filesDir, "libloader_m32.so"), overwrite = true)
                        resolvedSource = "extracted parentLibDir to filesDir"
                    } catch (e: Exception) {
                        Log.w(tag, "Failed fallback copy from parentLibDir: ${e.message}")
                    }
                }
            }

            if (resolvedSource == null) {
                // Source 3: Manual self-extraction from currently running APK ZIP file
                val apkPath = context.packageCodePath
                if (apkPath != null) {
                    checkedPaths.add("APK:$apkPath")
                    try {
                        java.util.zip.ZipFile(File(apkPath)).use { zip ->
                            val entry = zip.getEntry("lib/arm64-v8a/libproot.so")
                                ?: zip.getEntry("lib/arm64/libproot.so")
                            if (entry != null) {
                                zip.getInputStream(entry).use { input ->
                                    FileOutputStream(prootBinary).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                val l1 = zip.getEntry("lib/arm64-v8a/libloader.so") ?: zip.getEntry("lib/arm64/libloader.so")
                                if (l1 != null) {
                                    zip.getInputStream(l1).use { input ->
                                        FileOutputStream(File(context.filesDir, "libloader.so")).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                }
                                val l2 = zip.getEntry("lib/arm64-v8a/libloader_m32.so") ?: zip.getEntry("lib/arm64/libloader_m32.so")
                                if (l2 != null) {
                                    zip.getInputStream(l2).use { input ->
                                        FileOutputStream(File(context.filesDir, "libloader_m32.so")).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                }
                                resolvedSource = "APK self-extraction"
                                Log.i(tag, "Extracted proot from APK self-extraction!")
                            }
                        }
                    } catch (zipEx: Exception) {
                        Log.w(tag, "Self-extraction from APK failed: ${zipEx.message}")
                    }
                }
            }

            if (resolvedSource == null) {
                // Source 4: Assets fallback
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
                    Log.w(tag, "Asset libproot.so not found: ${assetEx.message}")
                }
            }

            if (resolvedSource == null && !prootBinary.exists()) {
                Log.i(tag, "Proot binary local source could not be found. Trying network download fallback...")
                val downloaded = downloadProotFallback()
                if (downloaded) {
                    resolvedSource = "network download fallback"
                } else {
                    val errorMsg = "Proot binary source could not be found locally or via download. Checked locations: ${checkedPaths.joinToString("; ")}"
                    Log.e(tag, errorMsg)
                    return Result.failure(IllegalStateException(errorMsg))
                }
            }

            // Apply executable permissions for fallback target
            if (prootBinary.exists()) {
                prootBinary.setReadable(true, false)
                prootBinary.setExecutable(true, false)
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "755", prootBinary.absolutePath)).waitFor()
                } catch (ignored: Exception) {}
            }

            val executableFile = getExecutableProot()
            if (executableFile.exists() && executableFile.canExecute()) {
                Log.i(tag, "PRoot binary verified successfully from source '$resolvedSource' at: ${executableFile.absolutePath}")
                return Result.success(executableFile)
            } else {
                val errorDetails = "PRoot binary exists=${executableFile.exists()} canExecute=${executableFile.canExecute()} path=${executableFile.absolutePath}"
                Log.e(tag, errorDetails)
                return Result.failure(IllegalStateException(errorDetails))
            }
        } catch (e: Exception) {
            val fullError = "Failed to install PRoot binary. Cause: ${e.message}"
            Log.e(tag, fullError, e)
            return Result.failure(IllegalStateException(fullError, e))
        }
    }

    private fun downloadProotFallback(): Boolean {
        val candidateUrls = listOf(
            "https://github.com/ahmed-alnassif/proot/releases/download/v26.08.25-7266fb3/proot-aarch64.zip",
            "https://github.com/ahmed-alnassif/proot/releases/download/v26.08.23-7266fb3/proot-aarch64.zip"
        )
        for (urlStr in candidateUrls) {
            try {
                Log.i(tag, "Attempting to download PRoot fallback zip from $urlStr")
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                if (connection.responseCode == 200) {
                    java.util.zip.ZipInputStream(connection.inputStream).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (name == "proot" || name.endsWith("/proot")) {
                                FileOutputStream(prootBinary).use { output ->
                                    zip.copyTo(output)
                                }
                            } else if (name == "libloader.so" || name.endsWith("/libloader.so")) {
                                FileOutputStream(File(context.filesDir, "libloader.so")).use { output ->
                                    zip.copyTo(output)
                                }
                            } else if (name == "libloader_m32.so" || name.endsWith("/libloader_m32.so")) {
                                FileOutputStream(File(context.filesDir, "libloader_m32.so")).use { output ->
                                    zip.copyTo(output)
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                    if (prootBinary.exists() && prootBinary.length() > 0) {
                        Log.i(tag, "Successfully downloaded fallback PRoot ZIP and extracted components from $urlStr")
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to download fallback from $urlStr: ${e.message}")
            }
        }
        return false
    }
}
