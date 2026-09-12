package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.UniversalDatabaseStudioManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UniversalDatabaseStudioManagerTest {

    private lateinit var context: Context
    private lateinit var manager: UniversalDatabaseStudioManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = UniversalDatabaseStudioManager(context)
    }

    @Test
    fun `generatePolyglotSnippet produces valid Node js client with all 3 databases`() {
        val snippet = manager.generatePolyglotSnippet("nodejs")
        assertNotNull(snippet)
        assertTrue("Snippet must include mysql2", snippet.contains("mysql2/promise"))
        assertTrue("Snippet must include ioredis", snippet.contains("ioredis"))
        assertTrue("Snippet must include mongodb", snippet.contains("mongodb"))
        assertTrue("Snippet must configure MariaDB port 3306", snippet.contains("3306"))
        assertTrue("Snippet must configure Redis port 6379", snippet.contains("6379"))
        assertTrue("Snippet must configure MongoDB port 27017", snippet.contains("27017"))
    }

    @Test
    fun `generatePolyglotSnippet produces valid Python client with all 3 databases`() {
        val snippet = manager.generatePolyglotSnippet("python")
        assertNotNull(snippet)
        assertTrue("Snippet must include pymysql", snippet.contains("pymysql"))
        assertTrue("Snippet must include redis", snippet.contains("redis.Redis"))
        assertTrue("Snippet must include MongoClient", snippet.contains("MongoClient"))
        assertTrue("Snippet must test MariaDB connection", snippet.contains("3306"))
        assertTrue("Snippet must test Redis connection", snippet.contains("6379"))
        assertTrue("Snippet must test Mongo connection", snippet.contains("27017"))
    }

    @Test
    fun `generatePolyglotSnippet produces valid Go client with all 3 databases`() {
        val snippet = manager.generatePolyglotSnippet("go")
        assertNotNull(snippet)
        assertTrue("Snippet must include database/sql", snippet.contains("database/sql"))
        assertTrue("Snippet must include go-redis", snippet.contains("github.com/redis/go-redis/v9"))
        assertTrue("Snippet must include mongo-driver", snippet.contains("go.mongodb.org/mongo-driver"))
    }

    @Test
    fun `generatePolyglotSnippet produces valid Rust client with tokio and async`() {
        val snippet = manager.generatePolyglotSnippet("rust")
        assertNotNull(snippet)
        assertTrue("Snippet must include tokio main", snippet.contains("#[tokio::main]"))
        assertTrue("Snippet must include sqlx", snippet.contains("sqlx::MySqlPool"))
        assertTrue("Snippet must include redis", snippet.contains("redis::Client"))
        assertTrue("Snippet must include mongodb", snippet.contains("Client::with_uri_str"))
    }

    @Test
    fun `generatePolyglotSnippet produces valid env configuration file`() {
        val snippet = manager.generatePolyglotSnippet("env")
        assertNotNull(snippet)
        assertTrue("Env must contain MARIADB_PORT=3306", snippet.contains("3306"))
        assertTrue("Env must contain REDIS_PORT=6379", snippet.contains("6379"))
        assertTrue("Env must contain MONGO_PORT=27017", snippet.contains("27017"))
        assertTrue("Env must contain MARIADB_PASSWORD", snippet.contains("MARIADB_PASSWORD"))
    }

    @Test
    fun `generatePolyglotSnippet produces CLI curl commands`() {
        val snippet = manager.generatePolyglotSnippet("cli")
        assertNotNull(snippet)
        assertTrue("CLI must contain mysql command", snippet.contains("mysql -h 127.0.0.1 -P 3306"))
        assertTrue("CLI must contain redis-cli command", snippet.contains("redis-cli -h 127.0.0.1 -p 6379"))
        assertTrue("CLI must contain mongosh command", snippet.contains("mongosh \"mongodb://127.0.0.1:27017/app_dev\""))
    }

    @Test
    fun `checkAllHealth returns 3 engines without throwing when ports are closed`() = runBlocking {
        val healthMap = manager.checkAllHealth()
        assertEquals("Health map must track all 3 engines", 3, healthMap.size)
        assertTrue("Health map must contain MariaDB", healthMap.containsKey("MariaDB"))
        assertTrue("Health map must contain Redis", healthMap.containsKey("Redis"))
        assertTrue("Health map must contain MongoDB", healthMap.containsKey("MongoDB"))

        assertEquals(3306, healthMap["MariaDB"]?.port)
        assertEquals(6379, healthMap["Redis"]?.port)
        assertEquals(27017, healthMap["MongoDB"]?.port)
    }

    @Test
    fun `executeSqlQuery gracefully handles query execution without unhandled crash`() = runBlocking {
        val result = manager.executeSqlQuery("SELECT 1;")
        assertNotNull(result)
        assertTrue("Must return valid SqlResult with rows, columns or handled error", result.error != null || result.rows.isNotEmpty() || result.columns.isNotEmpty())
    }

    @Test
    fun `executeRedisCommand gracefully handles command execution without unhandled crash`() = runBlocking {
        val result = manager.executeRedisCommand("PING")
        assertNotNull(result)
        assertTrue("Must return valid RedisResult with output or handled error", result.error != null || result.output.isNotEmpty())
    }

    @Test
    fun `executeMongoQuery gracefully handles query execution without unhandled crash`() = runBlocking {
        val result = manager.executeMongoQuery("app_dev", "{ \"ping\": 1 }")
        assertNotNull(result)
        assertTrue("Must return valid MongoResult with outputJson or handled error", result.error != null || result.outputJson.isNotEmpty())
    }

    @Test
    fun `runBenchmark executes gracefully and returns object even when offline`() = runBlocking {
        val benchmark = manager.runBenchmark()
        assertNotNull(benchmark)
        assertTrue("Ops per sec should be >= 0", benchmark.redisOpsPerSec >= 0)
    }
}
