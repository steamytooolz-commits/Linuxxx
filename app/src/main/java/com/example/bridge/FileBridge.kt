package com.example.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import com.example.core.UniversalDatabaseStudioManager
import com.google.gson.Gson
import java.io.File

/**
 * Native JavaScript interface exposed to CodeMirror 6 inside the WebView.
 * Handles file management (read, write, list, delete) and delegates database operations
 * to the centralized UniversalDatabaseStudioManager.
 */
open class FileBridge(
    private val root: File,
    private val context: Context? = null,
    private val onLog: (String) -> Unit = {}
) {

    private val gson = Gson()
    private val studioManager: UniversalDatabaseStudioManager? = context?.let {
        UniversalDatabaseStudioManager(it.applicationContext)
    }

    init {
        if (!root.exists()) {
            root.mkdirs()
        }
    }

    @JavascriptInterface
    fun readFile(path: String): String {
        return try {
            val target = if (path.isBlank()) root else File(root, path)
            if (target.exists() && target.isFile) {
                target.readText()
            } else {
                ""
            }
        } catch (e: Exception) {
            onLog("readFile error: ${e.message}")
            ""
        }
    }

    @JavascriptInterface
    fun writeFile(path: String, content: String) {
        try {
            val f = File(root, path)
            f.parentFile?.mkdirs()
            f.writeText(content)
            onLog("writeFile: $path (${content.length} chars)")
        } catch (e: Exception) {
            onLog("writeFile error: ${e.message}")
        }
    }

    @JavascriptInterface
    fun listDir(path: String): String {
        return try {
            val dir = if (path.isBlank()) root else File(root, path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val files = dir.listFiles()?.map {
                mapOf(
                    "name" to it.name,
                    "isDir" to it.isDirectory,
                    "size" to it.length()
                )
            } ?: emptyList()
            gson.toJson(files)
        } catch (e: Exception) {
            onLog("listDir error: ${e.message}")
            "[]"
        }
    }

    @JavascriptInterface
    fun deleteFile(path: String): Boolean {
        return try {
            val target = File(root, path)
            target.delete()
        } catch (e: Exception) {
            onLog("deleteFile error: ${e.message}")
            false
        }
    }

    @JavascriptInterface
    fun runQuery(type: String, query: String): String {
        val manager = studioManager ?: return gson.toJson(mapOf("error" to "Database manager not initialized"))
        return try {
            kotlinx.coroutines.runBlocking {
                when (type.lowercase()) {
                    "mariadb", "mysql", "sql" -> {
                        val result = manager.executeSqlQuery(query)
                        gson.toJson(
                            mapOf(
                                "status" to if (result.error == null) "success" else "error",
                                "database" to "mariadb",
                                "columns" to result.columns,
                                "rowCount" to result.rows.size,
                                "rows" to result.rows,
                                "affectedRows" to result.affectedRows,
                                "durationMs" to result.durationMs,
                                "error" to result.error
                            )
                        )
                    }
                    "redis" -> {
                        val result = manager.executeRedisCommand(query)
                        gson.toJson(
                            mapOf(
                                "status" to if (result.error == null) "success" else "error",
                                "database" to "redis",
                                "command" to result.command,
                                "output" to result.output,
                                "durationMs" to result.durationMs,
                                "error" to result.error
                            )
                        )
                    }
                    "mongodb", "mongo" -> {
                        val result = manager.executeMongoQuery("app_dev", query)
                        gson.toJson(
                            mapOf(
                                "status" to if (result.error == null) "success" else "error",
                                "database" to "mongodb",
                                "json" to result.outputJson,
                                "docCount" to result.documentCount,
                                "durationMs" to result.durationMs,
                                "error" to result.error
                            )
                        )
                    }
                    else -> gson.toJson(mapOf("error" to "Unsupported database type: $type"))
                }
            }
        } catch (e: Exception) {
            gson.toJson(
                mapOf(
                    "status" to "error",
                    "database" to type,
                    "error" to (e.message ?: e.toString())
                )
            )
        }
    }
}
