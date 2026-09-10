package com.example.core

import android.content.Context
import java.io.File

/**
 * Builds the exact CLI execution parameters for PRoot container startup and bootstrap setup.
 */
class ProotCommandBuilder(private val context: Context) {

    fun buildProotCommand(
        rootfsDir: File,
        dataDir: File,
        workspaceDir: File
    ): List<String> {
        val proot = ProotInstaller(context).getExecutableProot().absolutePath
        val rootfs = rootfsDir.absolutePath
        val data = dataDir.absolutePath
        val shell = resolveShellPath(rootfsDir)

        return listOf(
            proot,
            "-0",
            "-r", rootfs,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "$data/mysql:/var/lib/mysql",
            "-b", "$data/redis:/var/lib/redis",
            "-b", "$data/mongodb:/var/lib/mongodb",
            "-b", "$data/run:/var/run",
            "-b", "$data/log:/var/log",
            "-b", "${workspaceDir.absolutePath}:/root/workspace",
            "-w", "/root",
            shell, "/start-all.sh"
        )
    }

    fun buildSetupCommand(
        rootfsDir: File,
        dataDir: File
    ): List<String> {
        val proot = ProotInstaller(context).getExecutableProot().absolutePath
        val rootfs = rootfsDir.absolutePath
        val data = dataDir.absolutePath
        val shell = resolveShellPath(rootfsDir)

        return listOf(
            proot,
            "-0",
            "-r", rootfs,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "$data/mysql:/var/lib/mysql",
            "-b", "$data/redis:/var/lib/redis",
            "-b", "$data/mongodb:/var/lib/mongodb",
            "-b", "$data/run:/var/run",
            "-w", "/root",
            shell, "/setup.sh"
        )
    }

    private fun resolveShellPath(rootfsDir: File): String {
        val binBash = File(rootfsDir, "bin/bash")
        val usrBinBash = File(rootfsDir, "usr/bin/bash")
        return when {
            binBash.exists() && binBash.length() > 0 -> "/bin/bash"
            usrBinBash.exists() && usrBinBash.length() > 0 -> "/usr/bin/bash"
            else -> "/bin/bash"
        }
    }
}
