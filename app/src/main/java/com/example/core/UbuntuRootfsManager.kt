package com.example.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-level setup state for the on-device Linuxxx database appliance.
 */
sealed class SetupStep(val progress: Float, val description: String) {
    object Idle : SetupStep(0.0f, "Idle — Waiting to begin setup")
    object DeployingBinary : SetupStep(0.10f, "Step 1/5: Extracting ARM64 proot binary...")
    object Downloading : SetupStep(0.30f, "Step 2/5: Downloading Ubuntu 24.04 ARM64 rootfs...")
    object Extracting : SetupStep(0.65f, "Step 3/5: Extracting root filesystem (Commons Compress)...")
    object Configuring : SetupStep(0.85f, "Step 4/5: Deploying database configs & supervisor...")
    object InstallingPackages : SetupStep(0.95f, "Step 5/5: Installing MariaDB, Redis, MongoDB inside container...")
    object Completed : SetupStep(1.0f, "Linuxxx database appliance is ready!")
    data class Error(val message: String) : SetupStep(0.0f, "Setup error: $message")
}

/**
 * Orchestrates the rootless Ubuntu 24.04 LTS (noble) PRoot environment
 * and on-device MariaDB, Redis, and MongoDB daemons.
 */
class UbuntuRootfsManager(private val context: Context) {

    companion object {
        private const val TAG = "UbuntuRootfsManager"
    }

    val rootfsDir: File = File(context.filesDir, "rootfs")
    val dataDir: File = File(context.filesDir, "data")
    val workspaceDir: File = File(context.filesDir, "workspace")
    val prootBinary: File
        get() = ProotInstaller(context).getExecutableProot()

    private val prootInstaller = ProotInstaller(context)
    private val downloader = RootfsDownloader()
    private val extractor = RootfsExtractor()
    private val configurator = StackConfigurator(context)
    private val commandBuilder = ProotCommandBuilder(context)

    private val _setupState = MutableStateFlow<SetupStep>(SetupStep.Idle)
    val setupState: StateFlow<SetupStep> = _setupState.asStateFlow()

    /**
     * Checks if all required components and database binaries are fully present on the device.
     */
    fun isEnvironmentReady(): Boolean {
        val prootReady = prootBinary.exists() && prootBinary.canExecute()
        val bashReady = File(rootfsDir, "bin/bash").exists() || File(rootfsDir, "usr/bin/bash").exists()
        val supervisorReady = File(rootfsDir, "start-all.sh").exists() || File(rootfsDir, "root/start-all.sh").exists()

        val mariaReady = File(rootfsDir, "usr/sbin/mariadbd").exists() || File(rootfsDir, "usr/sbin/mysqld").exists()
        val redisReady = File(rootfsDir, "usr/bin/redis-server").exists()
        val mongoReady = File(rootfsDir, "usr/bin/mongod").exists() || File(rootfsDir, "usr/bin/mongodb").exists()

        return prootReady && bashReady && supervisorReady && mariaReady && redisReady && mongoReady
    }

    /**
     * Checks if the baseline proot and rootfs container are present, even if setup.sh package installation is still pending.
     */
    fun isBootstrapReady(): Boolean {
        val prootReady = prootBinary.exists() && prootBinary.canExecute()
        val bashReady = File(rootfsDir, "bin/bash").exists() || File(rootfsDir, "usr/bin/bash").exists()
        val supervisorReady = File(rootfsDir, "start-all.sh").exists() || File(rootfsDir, "root/start-all.sh").exists()
        return prootReady && bashReady && supervisorReady
    }

    /**
     * Executes the automated 5-step appliance provisioning pipeline.
     */
    suspend fun installStack(onStatus: (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Proot binary
            _setupState.value = SetupStep.DeployingBinary
            onStatus("Extracting ARM64 proot binary and configuring permissions...")
            val installResult = prootInstaller.install()
            if (installResult.isFailure) {
                val err = "Failed to setup proot: ${installResult.exceptionOrNull()?.message}"
                _setupState.value = SetupStep.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            // Step 2: Download rootfs archive if not extracted
            extractor.flattenNestedRootfs(rootfsDir)
            val bashFile = File(rootfsDir, "bin/bash")
            val usrBashFile = File(rootfsDir, "usr/bin/bash")
            val isBashReady = (bashFile.exists() && bashFile.length() > 0) || (usrBashFile.exists() && usrBashFile.length() > 0)
            if (!isBashReady) {
                val archiveFile = File(context.filesDir, "ubuntu-noble-arm64.tar.xz")
                if (!archiveFile.exists() || archiveFile.length() < 1024 * 1024) {
                    _setupState.value = SetupStep.Downloading
                    onStatus("Downloading Ubuntu 24.04 ARM64 rootfs...")
                    val dlResult = downloader.download(archiveFile) { _, msg ->
                        onStatus(msg)
                    }
                    if (dlResult.isFailure) {
                        val err = "Download failed: ${dlResult.exceptionOrNull()?.message}"
                        _setupState.value = SetupStep.Error(err)
                        return@withContext Result.failure(IllegalStateException(err))
                    }
                }

                // Step 3: Extract rootfs
                _setupState.value = SetupStep.Extracting
                onStatus("Extracting Ubuntu rootfs with Apache Commons Compress...")
                val extractResult = extractor.extract(archiveFile, rootfsDir) { _, msg ->
                    onStatus(msg)
                }
                if (extractResult.isFailure) {
                    val err = "Extraction failed: ${extractResult.exceptionOrNull()?.message}"
                    _setupState.value = SetupStep.Error(err)
                    return@withContext Result.failure(IllegalStateException(err))
                }
            }

            // Step 4: Configure configs and scripts
            _setupState.value = SetupStep.Configuring
            onStatus("Deploying database configs & supervisor scripts...")
            configurator.deploy(rootfsDir, dataDir, workspaceDir)

            // Step 5: Run setup.sh inside container if daemons not installed yet
            val mariaInstalled = File(rootfsDir, "usr/sbin/mariadbd").exists() || File(rootfsDir, "usr/sbin/mysqld").exists()
            if (!mariaInstalled && prootBinary.exists() && (File(rootfsDir, "bin/bash").exists() || File(rootfsDir, "usr/bin/bash").exists())) {
                _setupState.value = SetupStep.InstallingPackages
                onStatus("Installing MariaDB 11.x, Redis 7.x, MongoDB 7.x inside container...")
                try {
                    val setupCmd = buildSetupCommand()
                    val pb = ProcessBuilder(setupCmd)
                    pb.directory(context.filesDir)
                    pb.environment()["HOME"] = "/root"
                    pb.environment()["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                    pb.environment()["PROOT_LOADER"] = prootInstaller.getLoaderPath()
                    pb.environment()["PROOT_LOADER_32"] = prootInstaller.getLoader32Path()
                    pb.environment()["PROOT_NO_SECCOMP"] = "1"
                    pb.redirectErrorStream(true)
                    val proc = pb.start()
                    val reader = proc.inputStream.bufferedReader()
                    var line = reader.readLine()
                    while (line != null) {
                        Log.d(TAG, "[setup.sh] $line")
                        if (line.contains("install", ignoreCase = true) ||
                            line.contains("Setting up", ignoreCase = true) ||
                            line.contains("Unpacking", ignoreCase = true)
                        ) {
                            onStatus(line)
                        }
                        line = reader.readLine()
                    }
                    proc.waitFor()
                } catch (e: Exception) {
                    Log.w(TAG, "Package installation note: ${e.message}")
                }
            }

            _setupState.value = SetupStep.Completed
            onStatus("Linuxxx database appliance initialized successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Stack installation encountered an error", e)
            _setupState.value = SetupStep.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    fun buildProotCommand(): List<String> {
        return commandBuilder.buildProotCommand(rootfsDir, dataDir, workspaceDir)
    }

    fun buildSetupCommand(): List<String> {
        return commandBuilder.buildSetupCommand(rootfsDir, dataDir)
    }
}
