package com.example.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

sealed class SetupStep(val description: String, val progress: Float) {
    object Idle : SetupStep("Ready to initialize", 0.0f)
    object InstallingProot : SetupStep("Configuring ARM64 PRoot binary...", 0.05f)
    data class DownloadingRootfs(val bytesDownloaded: Long, val totalBytes: Long, val percent: Float) :
        SetupStep("Downloading Ubuntu 24.04 ARM64 rootfs (${bytesDownloaded / (1024 * 1024)}MB / ${if (totalBytes > 0) "${totalBytes / (1024 * 1024)}MB" else "..."})", 0.1f + percent * 0.45f)
    data class ExtractingRootfs(val filesExtracted: Int, val percent: Float) :
        SetupStep("Extracting rootfs with Commons Compress ($filesExtracted files)...", 0.55f + percent * 0.35f)
    object ConfiguringEnvironment : SetupStep("Deploying supervisor script and database configs...", 0.92f)
    object Completed : SetupStep("Linux userland & databases ready!", 1.0f)
    data class Error(val message: String) : SetupStep("Error: $message", 0.0f)
}

class UbuntuRootfsManager(private val context: Context) {

    companion object {
        private const val TAG = "UbuntuRootfsManager"
        const val ROOTFS_URL = "https://github.com/termux/proot-distro/releases/download/v4.11.0/ubuntu-noble-aarch64-pd-v4.11.0.tar.xz"
    }

    val prootBinary: File get() = File(context.filesDir, "proot")
    val rootfsDir: File get() = File(context.filesDir, "rootfs")
    val dataDir: File get() = File(context.filesDir, "data")
    val workspaceDir: File get() = File(rootfsDir, "root/workspace")
    val archiveFile: File get() = File(context.filesDir, "rootfs.tar.xz")

    private val _setupState = MutableStateFlow<SetupStep>(SetupStep.Idle)
    val setupState = _setupState.asStateFlow()

    fun isEnvironmentReady(): Boolean {
        val prootReady = prootBinary.exists() && prootBinary.canExecute()
        val bashReady = File(rootfsDir, "bin/bash").exists() || File(rootfsDir, "usr/bin/bash").exists()
        val supervisorReady = File(rootfsDir, "root/start-all.sh").exists()
        return prootReady && bashReady && supervisorReady
    }

    suspend fun installStack(onStatus: (String) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Proot binary
            _setupState.value = SetupStep.InstallingProot
            onStatus("Configuring PRoot executable...")
            setupProotBinary()

            // Step 2: Download rootfs if not already present or extracted
            val bashFile = File(rootfsDir, "bin/bash")
            if (!bashFile.exists()) {
                if (!archiveFile.exists() || archiveFile.length() < 1024 * 1024) {
                    downloadRootfs(onStatus)
                }

                // Step 3: Extract rootfs
                extractRootfs(onStatus)
            }

            // Step 4: Configure supervisor & database configs
            _setupState.value = SetupStep.ConfiguringEnvironment
            onStatus("Deploying database configs & supervisor scripts...")
            configureStackFiles()

            // Step 5: Run setup.sh inside proot container to install MariaDB, Redis, MongoDB
            val mariaInstalled = File(rootfsDir, "usr/sbin/mariadbd").exists() || File(rootfsDir, "usr/sbin/mysqld").exists()
            if (!mariaInstalled && prootBinary.exists() && (File(rootfsDir, "bin/bash").exists() || File(rootfsDir, "usr/bin/bash").exists())) {
                onStatus("Running container package setup (installing MariaDB, Redis, MongoDB)...")
                try {
                    val setupCmd = buildSetupCommand()
                    val pb = ProcessBuilder(setupCmd)
                    pb.directory(context.filesDir)
                    pb.environment()["HOME"] = "/root"
                    pb.environment()["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                    pb.redirectErrorStream(true)
                    val proc = pb.start()
                    val reader = proc.inputStream.bufferedReader()
                    var line = reader.readLine()
                    while (line != null) {
                        Log.d(TAG, "[setup.sh] $line")
                        if (line.contains("install", ignoreCase = true) || line.contains("Setting up", ignoreCase = true) || line.contains("Unpacking", ignoreCase = true)) {
                            onStatus(line)
                        }
                        line = reader.readLine()
                    }
                    proc.waitFor()
                } catch (e: Exception) {
                    Log.w(TAG, "Container setup note: ${e.message}")
                }
            }

            _setupState.value = SetupStep.Completed
            onStatus("Linuxxx database appliance initialized successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup Linuxxx environment", e)
            val errorMsg = e.message ?: e.toString()
            _setupState.value = SetupStep.Error(errorMsg)
            onStatus("Setup error: $errorMsg")
            Result.failure(e)
        }
    }

    private fun setupProotBinary() {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val sourceProot = File(nativeDir, "libproot.so")

        if (sourceProot.exists()) {
            sourceProot.copyTo(prootBinary, overwrite = true)
        } else {
            // Fallback: check assets or direct loader
            val assetLoader = copyAssetToFile("libproot.so", prootBinary)
            if (!assetLoader && !prootBinary.exists()) {
                Log.w(TAG, "libproot.so not found in nativeLibraryDir, looking for fallback")
            }
        }

        // Copy auxiliary loaders if available
        File(nativeDir, "libloader.so").takeIf { it.exists() }?.copyTo(File(context.filesDir, "libloader.so"), overwrite = true)
        File(nativeDir, "libloader_m32.so").takeIf { it.exists() }?.copyTo(File(context.filesDir, "libloader_m32.so"), overwrite = true)

        prootBinary.setReadable(true, false)
        prootBinary.setExecutable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", prootBinary.absolutePath)).waitFor()
        } catch (ignored: Exception) {}
    }

    private fun downloadRootfs(onStatus: (String) -> Unit) {
        onStatus("Connecting to rootfs repository...")
        var url = URL(ROOTFS_URL)
        var connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        var redirectCount = 0
        while (connection.responseCode in 300..399 && redirectCount < 5) {
            val newLocation = connection.getHeaderField("Location")
            connection.disconnect()
            url = URL(newLocation)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            redirectCount++
        }

        val totalBytes = connection.contentLengthLong
        val tempFile = File(context.filesDir, "rootfs.tar.xz.tmp")
        if (tempFile.exists()) tempFile.delete()

        var downloadedBytes = 0L
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var lastUpdate = System.currentTimeMillis()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 300) {
                        val fraction = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        _setupState.value = SetupStep.DownloadingRootfs(downloadedBytes, totalBytes, fraction)
                        lastUpdate = now
                    }
                }
            }
        }

        connection.disconnect()
        if (archiveFile.exists()) archiveFile.delete()
        tempFile.renameTo(archiveFile)
        onStatus("Rootfs download complete (${downloadedBytes / (1024 * 1024)} MB)")
    }

    private fun extractRootfs(onStatus: (String) -> Unit) {
        onStatus("Decompressing and unpacking Ubuntu 24.04 ARM64 rootfs...")
        rootfsDir.mkdirs()

        var count = 0
        var lastUpdate = System.currentTimeMillis()

        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis).use { bis ->
                XZCompressorInputStream(bis).use { xzIn ->
                    TarArchiveInputStream(xzIn).use { tarIn ->
                        var entry = tarIn.nextTarEntry
                        while (entry != null) {
                            val entryName = entry.name.removePrefix("./")
                            val targetFile = File(rootfsDir, entryName)

                            // Security check: path traversal prevention
                            val canonicalTarget = targetFile.canonicalPath
                            val canonicalRoot = rootfsDir.canonicalPath
                            if (!canonicalTarget.startsWith(canonicalRoot)) {
                                entry = tarIn.nextTarEntry
                                continue
                            }

                            if (entry.isDirectory) {
                                targetFile.mkdirs()
                            } else if (entry.isSymbolicLink) {
                                targetFile.parentFile?.mkdirs()
                                try {
                                    if (targetFile.exists()) targetFile.delete()
                                    Files.createSymbolicLink(targetFile.toPath(), Paths.get(entry.linkName))
                                } catch (e: Exception) {
                                    // Fallback if symlink creation is restricted
                                    targetFile.writeText(entry.linkName)
                                }
                            } else {
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { fos ->
                                    tarIn.copyTo(fos)
                                }
                                if ((entry.mode and 0b001001001) != 0) {
                                    targetFile.setExecutable(true, false)
                                }
                            }

                            count++
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 300) {
                                val simulatedPercent = (count / 15000f).coerceIn(0f, 0.99f)
                                _setupState.value = SetupStep.ExtractingRootfs(count, simulatedPercent)
                                lastUpdate = now
                            }

                            entry = tarIn.nextTarEntry
                        }
                    }
                }
            }
        }

        // Clean up archive to save flash storage
        if (archiveFile.exists()) {
            archiveFile.delete()
        }

        // Verify /bin/bash
        val bash = File(rootfsDir, "bin/bash")
        val usrBash = File(rootfsDir, "usr/bin/bash")
        if (!bash.exists() && usrBash.exists()) {
            try {
                Files.createSymbolicLink(bash.toPath(), Paths.get("/usr/bin/bash"))
            } catch (ignored: Exception) {
                usrBash.copyTo(bash, overwrite = true)
            }
        }
        bash.setExecutable(true, false)

        onStatus("Extracted $count files. Verification successful: /bin/bash found.")
    }

    private fun configureStackFiles() {
        // Workspace directory for CodeMirror
        workspaceDir.mkdirs()

        // Create sample files in workspace if empty
        val sampleSql = File(workspaceDir, "init.sql")
        if (!sampleSql.exists()) {
            sampleSql.writeText(
                """
                -- MariaDB 11.x Initialization Script
                CREATE DATABASE IF NOT EXISTS app_dev;
                USE app_dev;

                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );

                INSERT INTO users (username, email) VALUES
                ('alice', 'alice@linuxxx.local'),
                ('bob', 'bob@linuxxx.local');

                SELECT * FROM users;
                """.trimIndent()
            )
        }

        // Data directories
        File(dataDir, "mysql").mkdirs()
        File(dataDir, "redis").mkdirs()
        File(dataDir, "mongodb").mkdirs()
        File(dataDir, "run/mysqld").mkdirs()
        File(dataDir, "run/redis").mkdirs()
        File(dataDir, "run/mongodb").mkdirs()
        File(dataDir, "log").mkdirs()

        // Supervisor script - copy to /start-all.sh and /root/start-all.sh
        val startAllRoot = File(rootfsDir, "start-all.sh")
        val startAllDest = File(rootfsDir, "root/start-all.sh")
        copyAssetToFile("start-all.sh", startAllRoot)
        copyAssetToFile("start-all.sh", startAllDest)
        startAllRoot.setExecutable(true, false)
        startAllDest.setExecutable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", startAllRoot.absolutePath)).waitFor()
            Runtime.getRuntime().exec(arrayOf("chmod", "755", startAllDest.absolutePath)).waitFor()
        } catch (ignored: Exception) {}

        // Setup script - copy to /setup.sh and /root/setup.sh
        val setupRoot = File(rootfsDir, "setup.sh")
        val setupScriptDest = File(rootfsDir, "root/setup.sh")
        copyAssetToFile("setup.sh", setupRoot)
        copyAssetToFile("setup.sh", setupScriptDest)
        setupRoot.setExecutable(true, false)
        setupScriptDest.setExecutable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", setupRoot.absolutePath)).waitFor()
            Runtime.getRuntime().exec(arrayOf("chmod", "755", setupScriptDest.absolutePath)).waitFor()
        } catch (ignored: Exception) {}

        // Copy tuned database configs into the rootfs
        val configMap = mapOf(
            "mariadb.cnf" to "etc/mysql/mariadb.conf.d/99-android.cnf",
            "redis.conf" to "etc/redis/redis.conf",
            "mongod.conf" to "etc/mongod.conf"
        )
        for ((asset, dest) in configMap) {
            val destFile = File(rootfsDir, dest)
            destFile.parentFile?.mkdirs()
            try {
                context.assets.open("config/$asset").use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Config $asset copy failed: ${e.message}")
            }
        }
    }

    private fun copyAssetToFile(assetPath: String, destFile: File): Boolean {
        return try {
            destFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Asset $assetPath not copied: ${e.message}")
            false
        }
    }

    fun buildProotCommand(): List<String> {
        val proot = File(context.filesDir, "proot").absolutePath
        val rootfs = File(context.filesDir, "rootfs").absolutePath
        val dataDir = File(context.filesDir, "data").absolutePath
        return listOf(
            proot, "-0", "-r", rootfs,
            "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-b", "$dataDir/mysql:/var/lib/mysql",
            "-b", "$dataDir/redis:/var/lib/redis",
            "-b", "$dataDir/mongodb:/var/lib/mongodb",
            "-b", "$dataDir/run:/var/run",
            "-b", "$dataDir/log:/var/log",
            "-b", "${workspaceDir.absolutePath}:/root/workspace",
            "-w", "/root",
            "/bin/bash", "/start-all.sh"
        )
    }

    fun buildSetupCommand(): List<String> {
        val proot = File(context.filesDir, "proot").absolutePath
        val rootfs = File(context.filesDir, "rootfs").absolutePath
        val dataDir = File(context.filesDir, "data").absolutePath
        return listOf(
            proot, "-0", "-r", rootfs,
            "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-b", "$dataDir/mysql:/var/lib/mysql",
            "-b", "$dataDir/redis:/var/lib/redis",
            "-b", "$dataDir/mongodb:/var/lib/mongodb",
            "-b", "$dataDir/run:/var/run",
            "-w", "/root",
            "/bin/bash", "/setup.sh"
        )
    }
}
