package com.example.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Handles extracting and verifying the ARM64 PRoot binary with execution permissions.
 */
class ProotInstaller(private val context: Context) {

    private val tag = "ProotInstaller"

    val prootBinary: File = File(context.filesDir, "proot")

    fun isInstalled(): Boolean {
        return prootBinary.exists() && prootBinary.canExecute()
    }

    fun install(): Result<File> {
        return try {
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val sourceSo = File(nativeLibDir, "libproot.so")

            if (!sourceSo.exists()) {
                val abiDir = File(context.filesDir.parentFile, "lib")
                val altSo = File(abiDir, "libproot.so")
                if (altSo.exists()) {
                    altSo.copyTo(prootBinary, overwrite = true)
                } else {
                    try {
                        context.assets.open("libproot.so").use { input ->
                            FileOutputStream(prootBinary).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Asset libproot.so not available: ${e.message}")
                    }
                }
            } else {
                sourceSo.copyTo(prootBinary, overwrite = true)
            }

            prootBinary.setReadable(true, false)
            prootBinary.setExecutable(true, false)

            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", prootBinary.absolutePath)).waitFor()
            } catch (e: Exception) {
                Log.w(tag, "chmod 755 proot warning: ${e.message}")
            }

            if (prootBinary.exists() && prootBinary.canExecute()) {
                Log.i(tag, "proot binary successfully installed at: ${prootBinary.absolutePath}")
                Result.success(prootBinary)
            } else {
                Result.failure(IllegalStateException("proot binary exists=${prootBinary.exists()} canExecute=${prootBinary.canExecute()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to install proot binary", e)
            Result.failure(e)
        }
    }
}
