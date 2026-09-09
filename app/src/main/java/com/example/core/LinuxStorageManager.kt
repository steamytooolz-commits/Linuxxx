package com.example.core

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Dedicated FileService for managing the Linux userland directory structure.
 * Leverages the MANAGE_EXTERNAL_STORAGE permission to persist database files
 * in shared storage when permitted, falling back to internal storage otherwise.
 */
class LinuxStorageManager(private val context: Context) {
    companion object {
        private const val TAG = "LinuxStorageManager"
        const val LINUX_EXTERNAL_ROOT = "LinuxData"
    }

    /**
     * Verifies if the MANAGE_EXTERNAL_STORAGE permission has been granted by the user.
     */
    fun hasExternalStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Fallback for older versions
        }
    }

    /**
     * Gets the root external directory for the Linux userland, creating it if it doesn't exist.
     */
    fun getExternalRoot(): File? {
        if (!hasExternalStorageAccess()) return null
        val root = File(Environment.getExternalStorageDirectory(), LINUX_EXTERNAL_ROOT)
        if (!root.exists()) {
            val created = root.mkdirs()
            Log.d(TAG, "External root created: $created at ${root.absolutePath}")
        }
        return root
    }

    /**
     * Creates and returns the required subdirectories for the database daemons.
     */
    fun setupDatabaseDirectories(): Map<String, File> {
        val dirs = mutableMapOf<String, File>()
        val extRoot = getExternalRoot()
        
        // Prefer external storage if permission is granted, otherwise fallback to internal app storage ($HOME)
        val baseDir = extRoot ?: File(context.filesDir, "home")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        val dbNames = listOf("mysql_data", "redis_data", "mongo_data")
        for (dbName in dbNames) {
            val dbDir = File(baseDir, dbName)
            if (!dbDir.exists()) {
                val created = dbDir.mkdirs()
                Log.d(TAG, "Created directory ${dbDir.absolutePath}: $created")
            }
            dirs[dbName] = dbDir
        }
        
        return dirs
    }
    
    /**
     * Ensures default configuration files exist for MariaDB, Redis, and MongoDB in $PREFIX/etc/
     */
    fun ensureConfigFilesExist(): Map<String, File> {
        val etcDir = File(context.filesDir, "usr/etc")
        if (!etcDir.exists()) {
            etcDir.mkdirs()
        }

        val myCnf = File(etcDir, "my.cnf")
        if (!myCnf.exists()) {
            myCnf.writeText(
                """
                [mysqld]
                port = 3306
                bind-address = 127.0.0.1
                max_connections = 100
                innodb_buffer_pool_size = 128M
                innodb_log_file_size = 32M
                key_buffer_size = 16M
                default_storage_engine = InnoDB
                character-set-server = utf8mb4
                collation-server = utf8mb4_unicode_ci
                """.trimIndent()
            )
        }

        val redisConf = File(etcDir, "redis.conf")
        if (!redisConf.exists()) {
            redisConf.writeText(
                """
                port 6379
                bind 127.0.0.1
                protected-mode no
                timeout 0
                tcp-keepalive 300
                loglevel notice
                databases 16
                save 900 1
                save 300 10
                save 60 10000
                maxmemory 256mb
                maxmemory-policy noeviction
                """.trimIndent()
            )
        }

        val mongoConf = File(etcDir, "mongod.conf")
        if (!mongoConf.exists()) {
            mongoConf.writeText(
                """
                # mongod.conf - MongoDB configuration file
                storage:
                  journal:
                    enabled: true
                  wiredTiger:
                    engineConfig:
                      cacheSizeGB: 0.25
                systemLog:
                  destination: file
                  logAppend: true
                net:
                  port: 27017
                  bindIp: 127.0.0.1
                processManagement:
                  timeZoneInfo: /usr/share/zoneinfo
                """.trimIndent()
            )
        }

        return mapOf(
            "mariadb" to myCnf,
            "redis" to redisConf,
            "mongodb" to mongoConf
        )
    }

    /**
     * Retrieves the absolute paths of the storage directories to inject into the Linux environment.
     */
    fun getStorageEnvVars(): Map<String, String> {
        val env = mutableMapOf<String, String>()
        val dirs = setupDatabaseDirectories()
        ensureConfigFilesExist()
        env["MYSQL_DATA_DIR"] = dirs["mysql_data"]?.absolutePath ?: ""
        env["REDIS_DATA_DIR"] = dirs["redis_data"]?.absolutePath ?: ""
        env["MONGO_DATA_DIR"] = dirs["mongo_data"]?.absolutePath ?: ""
        return env
    }
}
