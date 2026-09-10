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

            // 6. Provide a default init.sql in workspace if absent
            val initSql = File(workspaceDir, "init.sql")
            if (!initSql.exists()) {
                val defaultSql = """
                    -- Linuxxx MariaDB 11.x Initialization
                    CREATE DATABASE IF NOT EXISTS appdb;
                    USE appdb;
                    CREATE TABLE IF NOT EXISTS users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(50) NOT NULL,
                        email VARCHAR(100),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                    INSERT INTO users (username, email) VALUES
                        ('admin', 'admin@localhost'),
                        ('developer', 'dev@linuxxx.internal');
                    SELECT * FROM users;
                """.trimIndent()
                initSql.writeText(defaultSql)
            }

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
