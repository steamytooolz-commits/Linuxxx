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
        val proot = File(context.filesDir, "proot").absolutePath
        val rootfs = rootfsDir.absolutePath
        val data = dataDir.absolutePath

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
            "/bin/bash", "/start-all.sh"
        )
    }

    fun buildSetupCommand(
        rootfsDir: File,
        dataDir: File
    ): List<String> {
        val proot = File(context.filesDir, "proot").absolutePath
        val rootfs = rootfsDir.absolutePath
        val data = dataDir.absolutePath

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
            "/bin/bash", "/setup.sh"
        )
    }
}
