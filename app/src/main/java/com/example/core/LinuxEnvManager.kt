package com.example.core

import android.content.Context
import android.util.Log
import java.io.File
import java.util.HashMap

/**
 * Core Linux Environment & Execution Manager.
 * Configures the POSIX-compliant environment paths (PATH, LD_LIBRARY_PATH, HOME, TMPDIR)
 * and spawns processes via ProcessBuilder with proper working directories and environment maps.
 */
class LinuxEnvManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "LinuxEnvManager"
    }

    val PREFIX: String = "${context.filesDir.absolutePath}/usr"
    val HOME: String = "${context.filesDir.absolutePath}/home"
    val TMPDIR: String = "$PREFIX/tmp"
    val BINDIR: String = "$PREFIX/bin"
    val LIBDIR: String = "$PREFIX/lib"

    init {
        // Guarantee directories exist on initialization
        File(PREFIX).mkdirs()
        File(HOME).mkdirs()
        File(TMPDIR).mkdirs()
        File(BINDIR).mkdirs()
        File(LIBDIR).mkdirs()
    }

    /**
     * Populates and returns the environment dictionary mirroring a Linux distribution sandbox.
     */
    fun getLinuxEnvironment(): HashMap<String, String> {
        val env = HashMap<String, String>()
        
        // Populate required system environment keys per Module 3 specifications
        env["PATH"] = "$PREFIX/bin:$PREFIX/bin/applets:/system/bin"
        env["LD_LIBRARY_PATH"] = "$PREFIX/lib"
        env["HOME"] = HOME
        env["TMPDIR"] = "$PREFIX/tmp"

        // Additional POSIX standard variables for database and runtime compatibility (Node.js, Python, etc.)
        env["PREFIX"] = PREFIX
        env["TERM"] = "xterm-256color"
        env["LANG"] = "en_US.UTF-8"
        env["LC_ALL"] = "en_US.UTF-8"
        env["USER"] = "u0_a0"
        env["SHELL"] = if (File(BINDIR, "bash").exists()) "$BINDIR/bash" else "/system/bin/sh"

        // Termux-compatible Node.js, npm, Python & Go environment configs
        env["NODE_PATH"] = "$PREFIX/lib/node_modules"
        env["npm_config_prefix"] = PREFIX
        env["npm_config_cache"] = "$TMPDIR/.npm"
        env["PYTHONPATH"] = "$PREFIX/lib/python3.11:$PREFIX/lib/python3.10:$PREFIX/lib/python3.12"
        env["PYTHONUSERBASE"] = "$HOME/.local"
        env["PIP_CACHE_DIR"] = "$TMPDIR/.pip"
        env["GOROOT"] = "$PREFIX/lib/go"
        env["GOPATH"] = "$HOME/go"

        return env
    }

    /**
     * Process runner spawning tasks using the configured Linux environment variables.
     */
    fun createProcessBuilder(
        command: List<String>,
        workingDir: File = File(HOME),
        additionalEnv: Map<String, String> = emptyMap(),
        redirectErrorStream: Boolean = true
    ): ProcessBuilder {
        val pb = ProcessBuilder(command)
        pb.directory(workingDir)
        pb.redirectErrorStream(redirectErrorStream)

        val env = pb.environment()
        val linuxEnv = getLinuxEnvironment()
        for ((key, value) in linuxEnv) {
            env[key] = value
        }
        for ((key, value) in additionalEnv) {
            env[key] = value
        }

        return pb
    }

    /**
     * Spawns a process directly and returns the running Process instance.
     */
    fun startProcess(
        command: List<String>,
        workingDir: File = File(HOME),
        additionalEnv: Map<String, String> = emptyMap()
    ): Process {
        Log.d(TAG, "Starting process: ${command.joinToString(" ")} in ${workingDir.absolutePath}")
        val pb = createProcessBuilder(
            command = command,
            workingDir = workingDir,
            additionalEnv = additionalEnv,
            redirectErrorStream = true
        )
        return pb.start()
    }

    /**
     * Spawns a shell script or one-line command string via /system/bin/sh.
     */
    fun startShellScript(
        scriptContent: String,
        workingDir: File = File(HOME)
    ): Process {
        val scriptFile = File(BINDIR, "cmd_${System.currentTimeMillis()}.sh")
        scriptFile.writeText("#!/system/bin/sh\n$scriptContent\n")
        scriptFile.setExecutable(true, false)

        return startProcess(listOf("/system/bin/sh", scriptFile.absolutePath), workingDir)
    }

    /**
     * Checks if a binary exists and is executable in usr/bin.
     */
    fun isBinaryReady(binaryName: String): Boolean {
        val file = File(BINDIR, binaryName)
        return file.exists() && file.canExecute()
    }
}
