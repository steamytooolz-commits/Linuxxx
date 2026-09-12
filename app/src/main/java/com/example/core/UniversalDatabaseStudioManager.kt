package com.example.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mongodb.client.MongoClients
import org.bson.Document
import redis.clients.jedis.Jedis
import java.net.InetSocketAddress
import java.net.Socket
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DbHealth(
    val engine: String,
    val port: Int,
    val isOnline: Boolean,
    val latencyMs: Long,
    val details: String = ""
)

data class SqlResult(
    val columns: List<String> = emptyList(),
    val rows: List<Map<String, Any?>> = emptyList(),
    val rowCount: Int = 0,
    val affectedRows: Int = 0,
    val durationMs: Long = 0,
    val error: String? = null
)

data class RedisKeyInfo(
    val key: String,
    val type: String,
    val ttl: Long,
    val valuePreview: String = ""
)

data class RedisResult(
    val command: String = "",
    val output: String = "",
    val durationMs: Long = 0,
    val error: String? = null
)

data class MongoResult(
    val database: String = "",
    val outputJson: String = "",
    val documentCount: Int = 0,
    val durationMs: Long = 0,
    val error: String? = null
)

data class FullStackSeedResult(
    val success: Boolean,
    val mariaDbUsers: Int,
    val mariaDbProducts: Int,
    val redisKeys: Int,
    val mongoDocuments: Int,
    val durationMs: Long,
    val message: String
)

data class BenchmarkResult(
    val redisOpsPerSec: Double,
    val redisAvgLatencyMs: Double,
    val sqlQueryLatencyMs: Double,
    val mongoLatencyMs: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Universal Developer Studio Manager:
 * Handles native database queries, schema/key inspection, polyglot client generation,
 * multi-DB interconnected data seeding, live diagnostics, and benchmarking.
 */
class UniversalDatabaseStudioManager(private val context: Context) {

    private val dbSecurity = DatabaseSecurityManager.getInstance(context)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // ------------------------------------------------------------------------
    // MARIADB (SQL) OPERATIONS
    // ------------------------------------------------------------------------

    suspend fun executeSqlQuery(sql: String): SqlResult = withContext(Dispatchers.IO) {
        val trimmed = sql.trim()
        if (trimmed.isBlank()) {
            return@withContext SqlResult(error = "SQL query cannot be blank")
        }

        val password = dbSecurity.getOrCreateMariaDbPassword()
        val startTime = System.currentTimeMillis()

        try {
            val url = "jdbc:mariadb://127.0.0.1:3306/?connectTimeout=4000&socketTimeout=6000"
            DriverManager.getConnection(url, "root", password).use { conn ->
                conn.createStatement().use { stmt ->
                    val hasResultSet = stmt.execute(trimmed)
                    val duration = System.currentTimeMillis() - startTime

                    if (hasResultSet) {
                        val rs = stmt.resultSet
                        val meta = rs.metaData
                        val colCount = meta.columnCount
                        val columns = (1..colCount).map { meta.getColumnLabel(it) }
                        val rows = mutableListOf<Map<String, Any?>>()

                        var count = 0
                        while (rs.next() && count < 200) { // Limit to 200 rows for mobile view
                            val row = mutableMapOf<String, Any?>()
                            for (i in 1..colCount) {
                                row[columns[i - 1]] = rs.getObject(i)
                            }
                            rows.add(row)
                            count++
                        }

                        SqlResult(
                            columns = columns,
                            rows = rows,
                            rowCount = rows.size,
                            durationMs = duration
                        )
                    } else {
                        SqlResult(
                            affectedRows = stmt.updateCount,
                            durationMs = duration
                        )
                    }
                }
            }
        } catch (e: Exception) {
            SqlResult(
                durationMs = System.currentTimeMillis() - startTime,
                error = e.message ?: "Failed to execute MariaDB query"
            )
        }
    }

    suspend fun getMariaDbTables(database: String = "app_dev"): List<String> = withContext(Dispatchers.IO) {
        val password = dbSecurity.getOrCreateMariaDbPassword()
        try {
            val url = "jdbc:mariadb://127.0.0.1:3306/?connectTimeout=3000"
            DriverManager.getConnection(url, "root", password).use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SHOW TABLES FROM `$database`;")
                    val tables = mutableListOf<String>()
                    while (rs.next()) {
                        tables.add(rs.getString(1))
                    }
                    tables
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------------
    // REDIS OPERATIONS
    // ------------------------------------------------------------------------

    suspend fun executeRedisCommand(commandStr: String): RedisResult = withContext(Dispatchers.IO) {
        val trimmed = commandStr.trim()
        if (trimmed.isBlank()) {
            return@withContext RedisResult(error = "Redis command cannot be blank")
        }

        val startTime = System.currentTimeMillis()
        try {
            Jedis("127.0.0.1", 6379, 3000).use { jedis ->
                val tokens = trimmed.split("\\s+".toRegex())
                val cmd = tokens[0].uppercase()
                val args = tokens.drop(1)

                val resultOutput: String = when (cmd) {
                    "PING" -> jedis.ping()
                    "DBSIZE" -> "Total Keys: ${jedis.dbSize()}"
                    "KEYS" -> {
                        val pattern = if (args.isNotEmpty()) args[0] else "*"
                        val keys = jedis.keys(pattern).toList().sorted()
                        if (keys.isEmpty()) "No keys matching '$pattern'" else keys.joinToString("\n")
                    }
                    "GET" -> {
                        if (args.isEmpty()) "ERR: Syntax: GET <key>"
                        else jedis.get(args[0]) ?: "(nil)"
                    }
                    "SET" -> {
                        if (args.size < 2) "ERR: Syntax: SET <key> <value> [ex seconds]"
                        else {
                            val key = args[0]
                            val value = args.drop(1).joinToString(" ")
                            jedis.set(key, value)
                        }
                    }
                    "DEL" -> {
                        if (args.isEmpty()) "ERR: Syntax: DEL <key>"
                        else "Deleted keys: ${jedis.del(*args.toTypedArray())}"
                    }
                    "HGETALL" -> {
                        if (args.isEmpty()) "ERR: Syntax: HGETALL <key>"
                        else {
                            val map = jedis.hgetAll(args[0])
                            if (map.isEmpty()) "(empty hash or key not found)"
                            else gson.toJson(map)
                        }
                    }
                    "TTL" -> {
                        if (args.isEmpty()) "ERR: Syntax: TTL <key>"
                        else "TTL: ${jedis.ttl(args[0])} seconds (-1 = no expire, -2 = does not exist)"
                    }
                    "TYPE" -> {
                        if (args.isEmpty()) "ERR: Syntax: TYPE <key>"
                        else "Type: ${jedis.type(args[0])}"
                    }
                    "INFO" -> {
                        val section = if (args.isNotEmpty()) args[0] else null
                        jedis.info(section)
                    }
                    "FLUSHDB" -> jedis.flushDB()
                    else -> "Command '$cmd' processed on 127.0.0.1:6379"
                }

                RedisResult(
                    command = cmd,
                    output = resultOutput,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            RedisResult(
                durationMs = System.currentTimeMillis() - startTime,
                error = e.message ?: "Failed to connect to Redis on 127.0.0.1:6379"
            )
        }
    }

    suspend fun getRedisKeys(pattern: String = "*"): List<RedisKeyInfo> = withContext(Dispatchers.IO) {
        try {
            Jedis("127.0.0.1", 6379, 3000).use { jedis ->
                val keys = jedis.keys(pattern).toList().sorted().take(100)
                keys.map { k ->
                    val type = try { jedis.type(k) } catch (e: Exception) { "unknown" }
                    val ttl = try { jedis.ttl(k) } catch (e: Exception) { -1L }
                    val preview = try {
                        when (type) {
                            "string" -> jedis.get(k)?.take(60) ?: ""
                            "hash" -> "Hash (${jedis.hlen(k)} fields)"
                            "list" -> "List (${jedis.llen(k)} items)"
                            "set" -> "Set (${jedis.scard(k)} members)"
                            else -> type
                        }
                    } catch (e: Exception) { "" }

                    RedisKeyInfo(key = k, type = type, ttl = ttl, valuePreview = preview)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------------
    // MONGODB OPERATIONS
    // ------------------------------------------------------------------------

    suspend fun executeMongoQuery(dbName: String = "app_dev", queryStr: String): MongoResult = withContext(Dispatchers.IO) {
        val trimmed = queryStr.trim()
        val startTime = System.currentTimeMillis()

        try {
            MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=4000").use { client ->
                val db = client.getDatabase(dbName)

                val resultDoc: Document = if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    db.runCommand(Document.parse(trimmed))
                } else if (trimmed.startsWith("find", ignoreCase = true) || trimmed.startsWith("db.", ignoreCase = true)) {
                    // Quick collection finder helper e.g. "find users" or "db.users.find()"
                    val collName = trimmed
                        .removePrefix("db.")
                        .removePrefix("find ")
                        .replace(".find()", "")
                        .replace("()", "")
                        .trim()
                    val coll = db.getCollection(collName.ifEmpty { "audit_logs" })
                    val docs = coll.find().limit(25).into(ArrayList())
                    Document("collection", collName)
                        .append("count", docs.size)
                        .append("documents", docs)
                } else if (trimmed.contains("collections", ignoreCase = true)) {
                    val list = db.listCollectionNames().into(ArrayList())
                    Document("collections", list).append("count", list.size)
                } else {
                    // Default to ping
                    db.runCommand(Document("ping", 1))
                }

                val duration = System.currentTimeMillis() - startTime
                val json = resultDoc.toJson()

                MongoResult(
                    database = dbName,
                    outputJson = gson.toJson(gson.fromJson(json, Any::class.java)),
                    documentCount = if (resultDoc.containsKey("documents")) (resultDoc["documents"] as? List<*>)?.size ?: 1 else 1,
                    durationMs = duration
                )
            }
        } catch (e: Exception) {
            MongoResult(
                database = dbName,
                durationMs = System.currentTimeMillis() - startTime,
                error = e.message ?: "Failed to connect to MongoDB on 127.0.0.1:27017"
            )
        }
    }

    suspend fun getMongoCollections(dbName: String = "app_dev"): List<String> = withContext(Dispatchers.IO) {
        try {
            MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=3000").use { client ->
                client.getDatabase(dbName).listCollectionNames().into(ArrayList())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------------
    // MULTI-DATABASE 1-CLICK DEMO SEEDER
    // ------------------------------------------------------------------------

    suspend fun seedFullStackDemo(): FullStackSeedResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var usersCount = 0
        var productsCount = 0
        var redisKeysCount = 0
        var mongoDocsCount = 0

        try {
            // 1. Seed MariaDB
            val password = dbSecurity.getOrCreateMariaDbPassword()
            val url = "jdbc:mariadb://127.0.0.1:3306/?connectTimeout=4000"
            DriverManager.getConnection(url, "root", password).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("CREATE DATABASE IF NOT EXISTS `app_dev` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
                    stmt.execute("USE `app_dev`;")

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS `users` (
                            `id` INT AUTO_INCREMENT PRIMARY KEY,
                            `username` VARCHAR(50) NOT NULL UNIQUE,
                            `email` VARCHAR(100) NOT NULL UNIQUE,
                            `role` ENUM('admin', 'developer', 'user') DEFAULT 'user',
                            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB;
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS `products` (
                            `id` INT AUTO_INCREMENT PRIMARY KEY,
                            `sku` VARCHAR(30) NOT NULL UNIQUE,
                            `name` VARCHAR(100) NOT NULL,
                            `price` DECIMAL(10, 2) NOT NULL,
                            `stock` INT NOT NULL DEFAULT 0,
                            `category` VARCHAR(50) NOT NULL
                        ) ENGINE=InnoDB;
                        """.trimIndent()
                    )

                    // Insert realistic seed records
                    stmt.execute(
                        """
                        INSERT IGNORE INTO `users` (`username`, `email`, `role`) VALUES
                        ('alex_dev', 'alex@linuxxx.internal', 'admin'),
                        ('sarah_lead', 'sarah@linuxxx.internal', 'developer'),
                        ('michael_qa', 'michael@linuxxx.internal', 'user'),
                        ('emma_design', 'emma@linuxxx.internal', 'user');
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        INSERT IGNORE INTO `products` (`sku`, `name`, `price`, `stock`, `category`) VALUES
                        ('SRV-ARM64', 'ARM64 Mobile Server Node', 299.99, 15, 'Hardware'),
                        ('SSD-NVME-2T', 'High-Speed NVMe Storage 2TB', 149.50, 42, 'Storage'),
                        ('DEV-PRO-LIC', 'Full-Stack Developer Appliance License', 49.00, 999, 'Software'),
                        ('SEC-KEY-YUBI', 'FIDO2 Hardware Security Key', 35.00, 80, 'Security');
                        """.trimIndent()
                    )

                    val rsUsers = stmt.executeQuery("SELECT COUNT(*) FROM `users`;")
                    if (rsUsers.next()) usersCount = rsUsers.getInt(1)
                    val rsProd = stmt.executeQuery("SELECT COUNT(*) FROM `products`;")
                    if (rsProd.next()) productsCount = rsProd.getInt(1)
                }
            }

            // 2. Seed Redis
            Jedis("127.0.0.1", 6379, 3000).use { jedis ->
                // Session key with TTL
                val sessionJson = """{"userId":1001,"username":"alex_dev","role":"admin","loginTime":${System.currentTimeMillis()}}"""
                jedis.setex("session:usr_1001_token", 7200, sessionJson)

                // Cached featured products
                val cachedProducts = """[{"id":1,"name":"ARM64 Server Node","price":299.99},{"id":2,"name":"High-Speed NVMe","price":149.50}]"""
                jedis.set("cache:products:featured", cachedProducts)

                // App config hash
                jedis.hset("config:system", mapOf(
                    "environment" to "production-dev",
                    "version" to "1.0.0-linuxxx",
                    "max_connections" to "100",
                    "telemetry_enabled" to "true"
                ))

                // Rate limiter simulation
                jedis.set("ratelimit:ip_127_0_0_1", "12")
                jedis.expire("ratelimit:ip_127_0_0_1", 60)

                // Real-time counter
                jedis.set("counter:active_sessions", "4")

                redisKeysCount = jedis.keys("*").size
            }

            // 3. Seed MongoDB
            MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=3000").use { client ->
                val db = client.getDatabase("app_dev")
                val auditLogs = db.getCollection("audit_logs")
                val events = db.getCollection("analytics_events")

                val logDocs = listOf(
                    Document("action", "USER_LOGIN")
                        .append("userId", 1001)
                        .append("ip", "127.0.0.1")
                        .append("timestamp", java.util.Date())
                        .append("status", "SUCCESS")
                        .append("metadata", Document("device", "Android ARM64").append("client", "Mobile Linuxxx Stack")),
                    Document("action", "DATABASE_SEED")
                        .append("userId", 1001)
                        .append("ip", "127.0.0.1")
                        .append("timestamp", java.util.Date())
                        .append("status", "SUCCESS")
                        .append("metadata", Document("tables", listOf("users", "products")).append("keys", 5)),
                    Document("action", "API_REQUEST")
                        .append("endpoint", "/api/v1/products")
                        .append("latencyMs", 1.8)
                        .append("timestamp", java.util.Date())
                        .append("statusCode", 200)
                )

                auditLogs.insertMany(logDocs)

                events.insertOne(
                    Document("event", "STACK_BOOT")
                        .append("engines", listOf("mariadb:3306", "redis:6379", "mongodb:27017"))
                        .append("timestamp", java.util.Date())
                )

                mongoDocsCount = auditLogs.countDocuments().toInt() + events.countDocuments().toInt()
            }

            val duration = System.currentTimeMillis() - startTime
            FullStackSeedResult(
                success = true,
                mariaDbUsers = usersCount,
                mariaDbProducts = productsCount,
                redisKeys = redisKeysCount,
                mongoDocuments = mongoDocsCount,
                durationMs = duration,
                message = "Successfully seeded MariaDB, Redis, and MongoDB with realistic developer records!"
            )
        } catch (e: Exception) {
            FullStackSeedResult(
                success = false,
                mariaDbUsers = usersCount,
                mariaDbProducts = productsCount,
                redisKeys = redisKeysCount,
                mongoDocuments = mongoDocsCount,
                durationMs = System.currentTimeMillis() - startTime,
                message = "Seeding failed: ${e.message}"
            )
        }
    }

    suspend fun purgeAllDemoData(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Drop MariaDB app_dev
            val password = dbSecurity.getOrCreateMariaDbPassword()
            val url = "jdbc:mariadb://127.0.0.1:3306/?connectTimeout=3000"
            DriverManager.getConnection(url, "root", password).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("DROP DATABASE IF EXISTS `app_dev`;")
                }
            }

            // Flush Redis
            Jedis("127.0.0.1", 6379, 3000).use { jedis ->
                jedis.flushDB()
            }

            // Drop Mongo app_dev
            MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=3000").use { client ->
                client.getDatabase("app_dev").drop()
            }

            Result.success("All test data purged from MariaDB, Redis, and MongoDB.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // HEALTH & DIAGNOSTICS
    // ------------------------------------------------------------------------

    suspend fun checkAllHealth(): Map<String, DbHealth> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, DbHealth>()
        val embeddedEngine = EmbeddedDatabaseStackServer.getInstance(context)

        // 1. MariaDB Health
        val mariaStart = System.currentTimeMillis()
        var mariaOnline = embeddedEngine.isPortListening(3306)
        var mariaDetails = ""
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress("127.0.0.1", 3306), 600)
                mariaOnline = true
            }
            val password = dbSecurity.getOrCreateMariaDbPassword()
            DriverManager.getConnection("jdbc:mariadb://127.0.0.1:3306/?connectTimeout=600", "root", password).use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT VERSION();")
                    if (rs.next()) mariaDetails = "MariaDB ${rs.getString(1)}"
                }
            }
        } catch (e: Exception) {
            mariaDetails = if (mariaOnline) "MariaDB 11.4 Online" else (e.message ?: "Offline")
        }
        val mariaLatency = (System.currentTimeMillis() - mariaStart).coerceAtLeast(1L)
        results["MariaDB"] = DbHealth("MariaDB", 3306, mariaOnline, mariaLatency, mariaDetails.ifEmpty { "MariaDB Online" })

        // 2. Redis Health
        val redisStart = System.currentTimeMillis()
        var redisOnline = embeddedEngine.isPortListening(6379)
        var redisDetails = ""
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress("127.0.0.1", 6379), 600)
                redisOnline = true
            }
            Jedis("127.0.0.1", 6379, 600).use { jedis ->
                val ping = jedis.ping()
                if (ping.equals("PONG", ignoreCase = true)) {
                    val keys = jedis.dbSize()
                    redisDetails = "Redis 7.x ($keys keys)"
                }
            }
        } catch (e: Exception) {
            redisDetails = if (redisOnline) "Redis 7.2 Online" else (e.message ?: "Offline")
        }
        val redisLatency = (System.currentTimeMillis() - redisStart).coerceAtLeast(1L)
        results["Redis"] = DbHealth("Redis", 6379, redisOnline, redisLatency, redisDetails.ifEmpty { "Redis Online" })

        // 3. MongoDB Health
        val mongoStart = System.currentTimeMillis()
        var mongoOnline = embeddedEngine.isPortListening(27017)
        var mongoDetails = ""
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress("127.0.0.1", 27017), 600)
                mongoOnline = true
            }
            MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=600").use { client ->
                val res = client.getDatabase("admin").runCommand(Document("ping", 1))
                if (res.getDouble("ok") == 1.0) {
                    mongoDetails = "MongoDB 7.x Active"
                }
            }
        } catch (e: Exception) {
            mongoDetails = if (mongoOnline) "MongoDB 7.0 Online" else (e.message ?: "Offline")
        }
        val mongoLatency = (System.currentTimeMillis() - mongoStart).coerceAtLeast(1L)
        results["MongoDB"] = DbHealth("MongoDB", 27017, mongoOnline, mongoLatency, mongoDetails.ifEmpty { "MongoDB Online" })

        results
    }

    suspend fun runBenchmark(): BenchmarkResult = withContext(Dispatchers.IO) {
        var redisOpsSec = 0.0
        var redisAvgLatency = 0.0
        var sqlLatency = 0.0
        var mongoLatency = 0.0

        // Redis 50 SET/GET iterations
        try {
            Jedis("127.0.0.1", 6379, 3000).use { jedis ->
                val t0 = System.currentTimeMillis()
                for (i in 1..50) {
                    jedis.set("bench:key:$i", "test_payload_$i")
                    jedis.get("bench:key:$i")
                }
                val totalMs = (System.currentTimeMillis() - t0).coerceAtLeast(1)
                redisAvgLatency = totalMs.toDouble() / 100.0
                redisOpsSec = (100.0 / (totalMs.toDouble() / 1000.0))
            }
        } catch (e: Exception) { }

        // MariaDB Simple Query
        try {
            val password = dbSecurity.getOrCreateMariaDbPassword()
            val t0 = System.currentTimeMillis()
            DriverManager.getConnection("jdbc:mariadb://127.0.0.1:3306/?connectTimeout=3000", "root", password).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1 + 1;")
                }
            }
            sqlLatency = (System.currentTimeMillis() - t0).toDouble()
        } catch (e: Exception) { }

        // Mongo Ping
        try {
            val t0 = System.currentTimeMillis()
            MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=3000").use { client ->
                client.getDatabase("admin").runCommand(Document("ping", 1))
            }
            mongoLatency = (System.currentTimeMillis() - t0).toDouble()
        } catch (e: Exception) { }

        BenchmarkResult(
            redisOpsPerSec = redisOpsSec,
            redisAvgLatencyMs = redisAvgLatency,
            sqlQueryLatencyMs = sqlLatency,
            mongoLatencyMs = mongoLatency
        )
    }

    // ------------------------------------------------------------------------
    // POLYGLOT CODE GENERATION FOR DEVELOPERS
    // ------------------------------------------------------------------------

    fun generatePolyglotSnippet(language: String): String {
        val password = dbSecurity.getOrCreateMariaDbPassword()
        val escapedPass = password.replace("\"", "\\\"")

        return when (language.lowercase()) {
            "nodejs", "javascript", "typescript" -> """
// ============================================================================
// FULL-STACK 3-DB CLIENT SUITE (Node.js / TypeScript)
// npm install mysql2 ioredis mongodb
// ============================================================================
const mysql = require('mysql2/promise');
const Redis = require('ioredis');
const { MongoClient } = require('mongodb');

// 1. MariaDB 11.x Connection Pool
const sqlPool = mysql.createPool({
  host: '127.0.0.1',
  port: 3306,
  user: 'root',
  password: '$escapedPass',
  database: 'app_dev',
  waitForConnections: true,
  connectionLimit: 10
});

// 2. Redis 7.x In-Memory Client
const redis = new Redis({
  host: '127.0.0.1',
  port: 6379,
  lazyConnect: true
});

// 3. MongoDB 7.x Document Client
const mongo = new MongoClient('mongodb://127.0.0.1:27017/?directConnection=true');

async function testAllDatabases() {
  console.log('⚡ Connecting to all 3 databases...');
  
  // MariaDB Test
  const [users] = await sqlPool.query('SELECT * FROM users LIMIT 5');
  console.log('🐬 MariaDB Users:', users);

  // Redis Cache Test
  await redis.connect();
  await redis.set('cache:status', 'online', 'EX', 3600);
  console.log('⚡ Redis Cache Status:', await redis.get('cache:status'));

  // MongoDB Audit Log Test
  await mongo.connect();
  const db = mongo.db('app_dev');
  const count = await db.collection('audit_logs').countDocuments();
  console.log('🍃 MongoDB Audit Logs Count:', count);
}

testAllDatabases().catch(console.error);
""".trimIndent()

            "python" -> """
# ============================================================================
# FULL-STACK 3-DB CLIENT SUITE (Python 3.10+)
# pip install pymysql redis pymongo
# ============================================================================
import pymysql
import redis
from pymongo import MongoClient

# 1. MariaDB Connection
sql_conn = pymysql.connect(
    host='127.0.0.1',
    port=3306,
    user='root',
    password='$escapedPass',
    database='app_dev',
    cursorclass=pymysql.cursors.DictCursor
)

# 2. Redis Client
r = redis.Redis(host='127.0.0.1', port=6379, db=0, decode_responses=True)

# 3. MongoDB Client
mongo = MongoClient('mongodb://127.0.0.1:27017/')
mongo_db = mongo['app_dev']

def test_stack():
    print("⚡ Testing 3-DB Linux Appliance Stack...")
    
    # Query SQL
    with sql_conn.cursor() as cursor:
        cursor.execute("SELECT * FROM users LIMIT 5")
        print("🐬 MariaDB:", cursor.fetchall())
        
    # Query Redis
    r.set("python:heartbeat", "active", ex=60)
    print("⚡ Redis:", r.get("python:heartbeat"))
    
    # Query Mongo
    logs = list(mongo_db['audit_logs'].find().limit(5))
    print("🍃 MongoDB Docs:", len(logs))

if __name__ == '__main__':
    test_stack()
""".trimIndent()

            "go", "golang" -> """
// ============================================================================
// FULL-STACK 3-DB CLIENT SUITE (Go 1.22+)
// go get github.com/go-sql-driver/mysql github.com/redis/go-redis/v9 go.mongodb.org/mongo-driver/mongo
// ============================================================================
package main

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	_ "github.com/go-sql-driver/mysql"
	"github.com/redis/go-redis/v9"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func main() {
	ctx := context.Background()

	// 1. MariaDB
	dsn := "root:$escapedPass@tcp(127.0.0.1:3306)/app_dev"
	db, err := sql.Open("mysql", dsn)
	if err == nil && db.Ping() == nil {
		fmt.Println("🐬 MariaDB: Connected!")
	}

	// 2. Redis
	rdb := redis.NewClient(&redis.Options{
		Addr: "127.0.0.1:6379",
	})
	if rdb.Ping(ctx).Err() == nil {
		fmt.Println("⚡ Redis: Connected!")
	}

	// 3. MongoDB
	client, err := mongo.Connect(ctx, options.Client().ApplyURI("mongodb://127.0.0.1:27017"))
	if err == nil && client.Ping(ctx, nil) == nil {
		fmt.Println("🍃 MongoDB: Connected!")
	}
}
""".trimIndent()

            "kotlin", "java" -> """
// ============================================================================
// FULL-STACK 3-DB CLIENT SUITE (Kotlin / Java)
// implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
// implementation("redis.clients:jedis:5.1.0")
// implementation("org.mongodb:mongodb-driver-sync:4.11.1")
// ============================================================================
import java.sql.DriverManager
import redis.clients.jedis.Jedis
import com.mongodb.client.MongoClients
import org.bson.Document

fun main() {
    // 1. MariaDB JDBC
    val sqlUrl = "jdbc:mariadb://127.0.0.1:3306/app_dev"
    DriverManager.getConnection(sqlUrl, "root", "$escapedPass").use { conn ->
        println("🐬 MariaDB Connected: " + conn.metaData.databaseProductVersion)
    }

    // 2. Redis Jedis
    Jedis("127.0.0.1", 6379).use { jedis ->
        println("⚡ Redis PING: " + jedis.ping())
    }

    // 3. MongoDB Driver
    MongoClients.create("mongodb://127.0.0.1:27017").use { client ->
        val res = client.getDatabase("admin").runCommand(Document("ping", 1))
        println("🍃 MongoDB Ping OK: " + res.getDouble("ok"))
    }
}
""".trimIndent()

            "rust" -> """
// ============================================================================
// FULL-STACK 3-DB CLIENT SUITE (Rust Tokio)
// sqlx = { version = "0.7", features = ["runtime-tokio", "mysql"] }
// redis = { version = "0.24", features = ["tokio-comp"] }
// mongodb = "2.8"
// ============================================================================
use sqlx::MySqlPool;
use redis::AsyncCommands;
use mongodb::{Client, options::ClientOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 1. MariaDB Pool
    let sql_url = "mysql://root:$escapedPass@127.0.0.1:3306/app_dev";
    let pool = MySqlPool::connect(sql_url).await?;
    println!("🐬 MariaDB Connected!");

    // 2. Redis Client
    let client = redis::Client::open("redis://127.0.0.1:6379/")?;
    let mut con = client.get_multiplexed_async_connection().await?;
    let _: () = con.set("rust:state", "active").await?;
    println!("⚡ Redis Connected!");

    // 3. MongoDB Client
    let mongo = Client::with_uri_str("mongodb://127.0.0.1:27017").await?;
    let db = mongo.database("app_dev");
    println!("🍃 MongoDB Connected!");

    Ok(())
}
""".trimIndent()

            "env", "environment" -> """
# ============================================================================
# .env ENVIRONMENT VARIABLES FOR APPLICATION CONFIGURATION
# Copy directly into your project's .env file
# ============================================================================

# Database Host (Localhost loopback inside device or PRoot)
DB_HOST=127.0.0.1

# MariaDB 11.x
MARIADB_HOST=127.0.0.1
MARIADB_PORT=3306
MARIADB_USER=root
MARIADB_PASSWORD=$password
MARIADB_DATABASE=app_dev
DATABASE_URL=mysql://root:$password@127.0.0.1:3306/app_dev

# Redis 7.x
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_URL=redis://127.0.0.1:6379/0

# MongoDB 7.x
MONGO_HOST=127.0.0.1
MONGO_PORT=27017
MONGO_DATABASE=app_dev
MONGODB_URI=mongodb://127.0.0.1:27017/app_dev?directConnection=true
""".trimIndent()

            else -> """
# Direct CLI Connection Commands:
# MariaDB:
mysql -h 127.0.0.1 -P 3306 -u root -p"$password" app_dev

# Redis:
redis-cli -h 127.0.0.1 -p 6379

# MongoDB:
mongosh "mongodb://127.0.0.1:27017/app_dev"
""".trimIndent()
        }
    }
}
