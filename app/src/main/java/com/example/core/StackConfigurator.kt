package com.example.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Handles deploying tuned database configurations (MariaDB, Redis, MongoDB)
 * and supervisor / setup scripts into the Ubuntu rootfs.
 */
class StackConfigurator(private val context: Context) {

    private val tag = "StackConfigurator"

    fun deploy(
        rootfsDir: File,
        dataDir: File,
        workspaceDir: File
    ): Boolean {
        return try {
            // 1. Data and runtime directories
            File(dataDir, "mysql").mkdirs()
            File(dataDir, "redis").mkdirs()
            File(dataDir, "mongodb").mkdirs()
            File(dataDir, "run/mysqld").mkdirs()
            File(dataDir, "run/redis").mkdirs()
            File(dataDir, "run/mongodb").mkdirs()
            File(dataDir, "log").mkdirs()

            // 2. Proot rootfs mount points
            File(rootfsDir, "var/lib/mysql").mkdirs()
            File(rootfsDir, "var/lib/redis").mkdirs()
            File(rootfsDir, "var/lib/mongodb").mkdirs()
            File(rootfsDir, "var/run/mysqld").mkdirs()
            File(rootfsDir, "var/run/redis").mkdirs()
            File(rootfsDir, "var/run/mongodb").mkdirs()
            File(rootfsDir, "var/log/mysql").mkdirs()
            File(rootfsDir, "var/log/redis").mkdirs()
            File(rootfsDir, "var/log/mongodb").mkdirs()
            File(rootfsDir, "root/workspace").mkdirs()
            workspaceDir.mkdirs()

            // 3. Deploy supervisor script: start-all.sh
            val startAllRoot = File(rootfsDir, "start-all.sh")
            val startAllInRoot = File(rootfsDir, "root/start-all.sh")
            copyAssetToFile("start-all.sh", startAllRoot)
            copyAssetToFile("start-all.sh", startAllInRoot)
            startAllRoot.setExecutable(true, false)
            startAllInRoot.setExecutable(true, false)
            chmod755(startAllRoot)
            chmod755(startAllInRoot)

            // 4. Deploy container setup script: setup.sh
            val setupRoot = File(rootfsDir, "setup.sh")
            val setupInRoot = File(rootfsDir, "root/setup.sh")
            copyAssetToFile("setup.sh", setupRoot)
            copyAssetToFile("setup.sh", setupInRoot)
            setupRoot.setExecutable(true, false)
            setupInRoot.setExecutable(true, false)
            chmod755(setupRoot)
            chmod755(setupInRoot)

            // 5. Deploy tuned database configs
            val configMap = mapOf(
                "mariadb.cnf" to "etc/mysql/mariadb.conf.d/99-android.cnf",
                "redis.conf" to "etc/redis/redis.conf",
                "mongod.conf" to "etc/mongod.conf"
            )
            for ((asset, dest) in configMap) {
                val destFile = File(rootfsDir, dest)
                destFile.parentFile?.mkdirs()
                copyAssetToFile("config/$asset", destFile)
            }

            // Note: init.sql is now exclusively managed by bridge.js (DEFAULT_FILES)
            // preventing dual source-of-truth drift.

            true
        } catch (e: Exception) {
            Log.e(tag, "Stack configuration failed", e)
            false
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
            Log.w(tag, "Could not copy asset $assetPath: ${e.message}")
            false
        }
    }

    private fun chmod755(file: File) {
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", file.absolutePath)).waitFor()
        } catch (_: Exception) {}
    }
}
