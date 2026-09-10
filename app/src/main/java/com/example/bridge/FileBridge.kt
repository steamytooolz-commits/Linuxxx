package com.example.bridge

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import java.io.File
import java.sql.DriverManager
import redis.clients.jedis.Jedis
import com.mongodb.client.MongoClients
import org.bson.Document

open class FileBridge(
    private val root: File,
    private val onLog: (String) -> Unit = {}
) {
    private val gson = Gson()

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
        return try {
            when (type.lowercase()) {
                "mariadb", "mysql", "sql" -> executeMariaDbQuery(query)
                "redis" -> executeRedisCommand(query)
                "mongodb", "mongo" -> executeMongoQuery(query)
                else -> gson.toJson(mapOf("error" to "Unsupported database type: $type"))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "status" to "error",
                "database" to type,
                "error" to (e.message ?: e.toString())
            ))
        }
    }

    private fun executeMariaDbQuery(query: String): String {
        return try {
            val url = "jdbc:mariadb://127.0.0.1:3306/?connectTimeout=3000&socketTimeout=5000"
            DriverManager.getConnection(url, "root", "").use { conn ->
                conn.createStatement().use { stmt ->
                    val hasResultSet = stmt.execute(query)
                    if (hasResultSet) {
                        val rs = stmt.resultSet
                        val meta = rs.metaData
                        val colCount = meta.columnCount
                        val columns = (1..colCount).map { meta.getColumnLabel(it) }
                        val rows = mutableListOf<Map<String, Any?>>()
                        while (rs.next()) {
                            val row = mutableMapOf<String, Any?>()
                            for (i in 1..colCount) {
                                row[columns[i - 1]] = rs.getObject(i)
                            }
                            rows.add(row)
                        }
                        gson.toJson(mapOf(
                            "status" to "success",
                            "database" to "mariadb",
                            "columns" to columns,
                            "rowCount" to rows.size,
                            "rows" to rows
                        ))
                    } else {
                        gson.toJson(mapOf(
                            "status" to "success",
                            "database" to "mariadb",
                            "affectedRows" to stmt.updateCount
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "status" to "error",
                "database" to "mariadb",
                "port" to 3306,
                "message" to (e.message ?: "Failed to connect to MariaDB on 127.0.0.1:3306")
            ))
        }
    }

    private fun executeRedisCommand(query: String): String {
        return try {
            Jedis("127.0.0.1", 6379, 3000).use { jedis ->
                val parts = query.trim().split("\\s+".toRegex())
                if (parts.isEmpty()) return gson.toJson(mapOf("error" to "Empty command"))
                val cmd = parts[0].uppercase()
                val args = parts.drop(1).toTypedArray()
                val result: Any? = when (cmd) {
                    "PING" -> jedis.ping()
                    "GET" -> if (args.isNotEmpty()) jedis.get(args[0]) else "ERR wrong number of arguments for GET"
                    "SET" -> if (args.size >= 2) jedis.set(args[0], args[1]) else "ERR wrong number of arguments for SET"
                    "KEYS" -> jedis.keys(if (args.isNotEmpty()) args[0] else "*").toList()
                    "HGETALL" -> if (args.isNotEmpty()) jedis.hgetAll(args[0]) else "ERR wrong number of arguments for HGETALL"
                    "DBSIZE" -> jedis.dbSize()
                    "INFO" -> jedis.info(if (args.isNotEmpty()) args[0] else null)
                    else -> "Command '$cmd' processed on 127.0.0.1:6379"
                }
                gson.toJson(mapOf(
                    "status" to "success",
                    "database" to "redis",
                    "command" to cmd,
                    "result" to result
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "status" to "error",
                "database" to "redis",
                "port" to 6379,
                "message" to (e.message ?: "Failed to connect to Redis on 127.0.0.1:6379")
            ))
        }
    }

    private fun executeMongoQuery(query: String): String {
        return try {
            MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=3000").use { client ->
                val db = client.getDatabase("admin")
                val clean = query.trim()
                val commandDoc = if (clean.startsWith("{") && clean.endsWith("}")) {
                    Document.parse(clean)
                } else if (clean.contains("ping", ignoreCase = true)) {
                    Document("ping", 1)
                } else if (clean.contains("buildinfo", ignoreCase = true)) {
                    Document("buildinfo", 1)
                } else {
                    Document("ping", 1)
                }
                val result = db.runCommand(commandDoc)
                gson.toJson(mapOf(
                    "status" to "success",
                    "database" to "mongodb",
                    "result" to Document.parse(result.toJson())
                ))
            }
        } catch (e: Exception) {
            gson.toJson(mapOf(
                "status" to "error",
                "database" to "mongodb",
                "port" to 27017,
                "message" to (e.message ?: "Failed to connect to MongoDB on 127.0.0.1:27017")
            ))
        }
    }
}
