package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BenchmarkResult
import com.example.core.DbHealth
import com.example.core.FullStackSeedResult
import com.example.core.MongoResult
import com.example.core.RedisKeyInfo
import com.example.core.RedisResult
import com.example.core.SqlResult
import com.example.ui.MainUiState
import com.example.ui.StudioTab

@Composable
fun UniversalStudioScreen(
    uiState: MainUiState,
    onTabSelected: (StudioTab) -> Unit,
    onEngineSelected: (String) -> Unit,
    onSqlQueryChange: (String) -> Unit,
    onRedisCommandChange: (String) -> Unit,
    onMongoQueryChange: (String) -> Unit,
    onExecuteQuery: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onSeedDemoData: () -> Unit,
    onPurgeDemoData: () -> Unit,
    onTestHealth: () -> Unit,
    onRunBenchmark: () -> Unit,
    onRefreshBrowser: () -> Unit,
    onSelectRedisKey: (String) -> Unit,
    onDeleteRedisKey: (String) -> Unit,
    onSelectSqlTable: (String) -> Unit,
    onSelectMongoCollection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        onTestHealth()
        onRefreshBrowser()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF030712) else Color(0xFFF8FAFC))
    ) {
        // Top Health Bar Banner
        HealthDiagnosticsBar(
            healthMap = uiState.studioHealthMap,
            isDark = isDark,
            onTestHealth = onTestHealth
        )

        // Navigation Tabs (Query Console, Data Browser, Polyglot Hub, Seeder & Bench)
        ScrollableTabRow(
            selectedTabIndex = uiState.studioTab.ordinal,
            containerColor = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF),
            contentColor = Color(0xFF38BDF8),
            edgePadding = 12.dp,
            modifier = Modifier.fillMaxWidth().testTag("studio_tab_row")
        ) {
            StudioTab.entries.forEach { tab ->
                val isSelected = uiState.studioTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) Color(0xFF38BDF8) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier.testTag("studio_tab_${tab.name.lowercase()}")
                )
            }
        }

        // Active Tab View Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (uiState.studioTab) {
                StudioTab.QUERY -> QueryConsoleView(
                    uiState = uiState,
                    isDark = isDark,
                    onEngineSelected = onEngineSelected,
                    onSqlQueryChange = onSqlQueryChange,
                    onRedisCommandChange = onRedisCommandChange,
                    onMongoQueryChange = onMongoQueryChange,
                    onExecuteQuery = onExecuteQuery
                )
                StudioTab.BROWSER -> DataBrowserView(
                    uiState = uiState,
                    isDark = isDark,
                    onRefresh = onRefreshBrowser,
                    onSelectRedisKey = onSelectRedisKey,
                    onDeleteRedisKey = onDeleteRedisKey,
                    onSelectSqlTable = onSelectSqlTable,
                    onSelectMongoCollection = onSelectMongoCollection
                )
                StudioTab.CODEGEN -> PolyglotHubView(
                    uiState = uiState,
                    isDark = isDark,
                    onLanguageSelected = onLanguageSelected,
                    onCopyCode = { code ->
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Copied code snippet to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
                StudioTab.SEEDER -> SeederBenchmarkView(
                    uiState = uiState,
                    isDark = isDark,
                    onSeedDemoData = onSeedDemoData,
                    onPurgeDemoData = onPurgeDemoData,
                    onRunBenchmark = onRunBenchmark
                )
            }
        }
    }
}

// ============================================================================
// 1. HEALTH DIAGNOSTICS BAR
// ============================================================================

@Composable
private fun HealthDiagnosticsBar(
    healthMap: Map<String, DbHealth>,
    isDark: Boolean,
    onTestHealth: () -> Unit
) {
    SurfaceCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                // MariaDB Chip
                val maria = healthMap["MariaDB"] ?: DbHealth("MariaDB", 3306, false, 0)
                EngineHealthPill("MariaDB", 3306, maria.isOnline, maria.latencyMs, Color(0xFF0284C7), isDark)

                // Redis Chip
                val redis = healthMap["Redis"] ?: DbHealth("Redis", 6379, false, 0)
                EngineHealthPill("Redis", 6379, redis.isOnline, redis.latencyMs, Color(0xFFEF4444), isDark)

                // MongoDB Chip
                val mongo = healthMap["MongoDB"] ?: DbHealth("MongoDB", 27017, false, 0)
                EngineHealthPill("MongoDB", 27017, mongo.isOnline, mongo.latencyMs, Color(0xFF10B981), isDark)
            }

            OutlinedButton(
                onClick = onTestHealth,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF38BDF8)
                ),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_test_db_health")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Ping", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ping All", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EngineHealthPill(
    name: String,
    port: Int,
    isOnline: Boolean,
    latencyMs: Long,
    brandColor: Color,
    isDark: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .border(1.dp, if (isOnline) brandColor.copy(alpha = 0.4f) else Color(0xFF475569), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$name:$port",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
        if (isOnline) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${latencyMs}ms",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF22C55E)
            )
        }
    }
}

// ============================================================================
// 2. QUERY CONSOLE VIEW
// ============================================================================

@Composable
private fun QueryConsoleView(
    uiState: MainUiState,
    isDark: Boolean,
    onEngineSelected: (String) -> Unit,
    onSqlQueryChange: (String) -> Unit,
    onRedisCommandChange: (String) -> Unit,
    onMongoQueryChange: (String) -> Unit,
    onExecuteQuery: () -> Unit
) {
    val activeEngine = uiState.studioEngine

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Engine Selector Chips (MariaDB, Redis, MongoDB)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EngineSelectButton("MariaDB (SQL)", "mariadb", activeEngine == "mariadb", Color(0xFF0284C7), isDark) { onEngineSelected("mariadb") }
            EngineSelectButton("Redis (CLI)", "redis", activeEngine == "redis", Color(0xFFEF4444), isDark) { onEngineSelected("redis") }
            EngineSelectButton("MongoDB (JSON)", "mongodb", activeEngine == "mongodb", Color(0xFF10B981), isDark) { onEngineSelected("mongodb") }
        }

        // Query Presets Chip Row
        QueryPresetsRow(
            engine = activeEngine,
            isDark = isDark,
            onSelectPreset = { preset ->
                when (activeEngine) {
                    "mariadb" -> onSqlQueryChange(preset)
                    "redis" -> onRedisCommandChange(preset)
                    "mongodb" -> onMongoQueryChange(preset)
                }
            }
        )

        // Query Input Box
        val currentInput = when (activeEngine) {
            "mariadb" -> uiState.studioSqlQueryInput
            "redis" -> uiState.studioRedisCommandInput
            else -> uiState.studioMongoQueryInput
        }

        OutlinedTextField(
            value = currentInput,
            onValueChange = {
                when (activeEngine) {
                    "mariadb" -> onSqlQueryChange(it)
                    "redis" -> onRedisCommandChange(it)
                    else -> onMongoQueryChange(it)
                }
            },
            placeholder = {
                Text(
                    text = when (activeEngine) {
                        "mariadb" -> "Enter SQL query (e.g. SELECT * FROM users;)"
                        "redis" -> "Enter Redis command (e.g. KEYS *, GET session:usr_1001_token)"
                        else -> "Enter MongoDB command or helper (e.g. find audit_logs, { \"ping\": 1 })"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("studio_query_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF),
                unfocusedContainerColor = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF),
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
            )
        )

        // Execution Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.studioStatusMessage.isNotBlank()) {
                Text(
                    text = uiState.studioStatusMessage,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = onExecuteQuery,
                enabled = !uiState.studioIsLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (activeEngine) {
                        "mariadb" -> Color(0xFF0284C7)
                        "redis" -> Color(0xFFEF4444)
                        else -> Color(0xFF10B981)
                    },
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("btn_execute_studio_query")
            ) {
                if (uiState.studioIsLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing...", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("▶ Run Query", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Query Results Card
        QueryResultCard(
            engine = activeEngine,
            sqlResult = uiState.studioSqlResult,
            redisResult = uiState.studioRedisResult,
            mongoResult = uiState.studioMongoResult,
            isDark = isDark
        )
    }
}

@Composable
private fun QueryPresetsRow(
    engine: String,
    isDark: Boolean,
    onSelectPreset: (String) -> Unit
) {
    val presets = when (engine) {
        "mariadb" -> listOf(
            "SELECT * FROM users LIMIT 10;",
            "SELECT * FROM products;",
            "SHOW TABLES;",
            "SHOW DATABASES;",
            "STATUS;"
        )
        "redis" -> listOf(
            "KEYS *",
            "DBSIZE",
            "INFO memory",
            "PING",
            "GET session:usr_1001_token",
            "HGETALL config:system"
        )
        else -> listOf(
            "find audit_logs",
            "find analytics_events",
            "collections",
            "{ \"ping\": 1 }",
            "{ \"buildinfo\": 1 }"
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(presets) { preset ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    .clickable { onSelectPreset(preset) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = preset,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QueryResultCard(
    engine: String,
    sqlResult: SqlResult,
    redisResult: RedisResult,
    mongoResult: MongoResult,
    isDark: Boolean
) {
    SurfaceCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXECUTION RESULT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                val duration = when (engine) {
                    "mariadb" -> sqlResult.durationMs
                    "redis" -> redisResult.durationMs
                    else -> mongoResult.durationMs
                }
                Text(
                    text = "${duration}ms",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF22C55E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (engine) {
                "mariadb" -> {
                    if (sqlResult.error != null) {
                        ErrorResultBox(sqlResult.error)
                    } else if (sqlResult.rows.isNotEmpty()) {
                        SqlDataTable(sqlResult.columns, sqlResult.rows, isDark)
                    } else {
                        Text(
                            text = "Query OK. Affected rows: ${sqlResult.affectedRows}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF22C55E)
                        )
                    }
                }
                "redis" -> {
                    if (redisResult.error != null) {
                        ErrorResultBox(redisResult.error)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = redisResult.output.ifBlank { "No output" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                        }
                    }
                }
                "mongodb" -> {
                    if (mongoResult.error != null) {
                        ErrorResultBox(mongoResult.error)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = mongoResult.outputJson.ifBlank { "{}" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SqlDataTable(
    columns: List<String>,
    rows: List<Map<String, Any?>>,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                .padding(vertical = 6.dp)
        ) {
            columns.forEach { col ->
                Text(
                    text = col,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier
                        .width(130.dp)
                        .padding(horizontal = 8.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFF475569))

        // Rows
        rows.take(50).forEachIndexed { index, row ->
            val rowBg = if (index % 2 == 0) {
                if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            } else {
                if (isDark) Color(0xFF090D16) else Color(0xFFF8FAFC)
            }
            Row(
                modifier = Modifier
                    .background(rowBg)
                    .padding(vertical = 6.dp)
            ) {
                columns.forEach { col ->
                    val value = row[col]?.toString() ?: "NULL"
                    Text(
                        text = value,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (value == "NULL") Color(0xFF64748B) else if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorResultBox(error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "ERROR: $error",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFFEF4444)
        )
    }
}

// ============================================================================
// 3. DATA BROWSER VIEW
// ============================================================================

@Composable
private fun DataBrowserView(
    uiState: MainUiState,
    isDark: Boolean,
    onRefresh: () -> Unit,
    onSelectRedisKey: (String) -> Unit,
    onDeleteRedisKey: (String) -> Unit,
    onSelectSqlTable: (String) -> Unit,
    onSelectMongoCollection: (String) -> Unit
) {
    var browserEngine by remember { mutableStateOf("redis") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Engine selector & Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EngineSelectButton("Redis Keys (${uiState.studioRedisKeys.size})", "redis", browserEngine == "redis", Color(0xFFEF4444), isDark) { browserEngine = "redis" }
                EngineSelectButton("MariaDB Tables (${uiState.studioSqlTables.size})", "mariadb", browserEngine == "mariadb", Color(0xFF0284C7), isDark) { browserEngine = "mariadb" }
                EngineSelectButton("Mongo Colls (${uiState.studioMongoCollections.size})", "mongodb", browserEngine == "mongodb", Color(0xFF10B981), isDark) { browserEngine = "mongodb" }
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
            }
        }

        when (browserEngine) {
            "redis" -> {
                if (uiState.studioRedisKeys.isEmpty()) {
                    EmptyBrowserState("No Redis keys found. Seed demo data or set keys in Query Console.", isDark)
                } else {
                    uiState.studioRedisKeys.forEach { keyInfo ->
                        RedisKeyCard(
                            keyInfo = keyInfo,
                            isDark = isDark,
                            onInspect = { onSelectRedisKey(keyInfo.key) },
                            onDelete = { onDeleteRedisKey(keyInfo.key) }
                        )
                    }
                }
            }
            "mariadb" -> {
                if (uiState.studioSqlTables.isEmpty()) {
                    EmptyBrowserState("No MariaDB tables in app_dev. Seed demo data or run CREATE TABLE.", isDark)
                } else {
                    uiState.studioSqlTables.forEach { table ->
                        SurfaceCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSqlTable(table) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF0284C7))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = table,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "SELECT * ▶",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            "mongodb" -> {
                if (uiState.studioMongoCollections.isEmpty()) {
                    EmptyBrowserState("No Mongo collections in app_dev. Seed demo data or insert documents.", isDark)
                } else {
                    uiState.studioMongoCollections.forEach { coll ->
                        SurfaceCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectMongoCollection(coll) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = coll,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "FIND 25 ▶",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RedisKeyCard(
    keyInfo: RedisKeyInfo,
    isDark: Boolean,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    SurfaceCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (keyInfo.type.lowercase()) {
                                    "string" -> Color(0xFF0284C7)
                                    "hash" -> Color(0xFF8B5CF6)
                                    "list" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = keyInfo.type.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = keyInfo.key,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                if (keyInfo.valuePreview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = keyInfo.valuePreview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onInspect, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Inspect", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyBrowserState(message: String, isDark: Boolean) {
    SurfaceCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// ============================================================================
// 4. POLYGLOT CODE HUB VIEW
// ============================================================================

@Composable
private fun PolyglotHubView(
    uiState: MainUiState,
    isDark: Boolean,
    onLanguageSelected: (String) -> Unit,
    onCopyCode: (String) -> Unit
) {
    val languages = listOf(
        "Node.js" to "nodejs",
        "Python" to "python",
        "Go" to "go",
        "Kotlin / Java" to "kotlin",
        "Rust" to "rust",
        ".env Config" to "env",
        "CLI / cURL" to "cli"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Language Selector Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(languages) { (label, key) ->
                val isSelected = uiState.studioSelectedLanguage == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF0284C7) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        .clickable { onLanguageSelected(key) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                }
            }
        }

        // Code Output Card
        SurfaceCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "READY-TO-SHIP BOILERPLATE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    Button(
                        onClick = { onCopyCode(uiState.studioGeneratedSnippet) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_copy_polyglot_code")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9))
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.studioGeneratedSnippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. SEEDER & BENCHMARK VIEW
// ============================================================================

@Composable
private fun SeederBenchmarkView(
    uiState: MainUiState,
    isDark: Boolean,
    onSeedDemoData: () -> Unit,
    onPurgeDemoData: () -> Unit,
    onRunBenchmark: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-DB 1-Click Seeder Hero Card
        SurfaceCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "1-Click Full-Stack Seeder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Seeds MariaDB users & products, Redis cache & sessions, and Mongo audit logs in one shot.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSeedDemoData,
                        enabled = !uiState.studioIsLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_seed_multi_db")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚡ Seed 3-DB Testbed", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onPurgeDemoData,
                        enabled = !uiState.studioIsLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("btn_purge_multi_db")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Purge", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Seed result details
                uiState.studioSeedResult?.let { res ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (res.success) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, if (res.success) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = res.message,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (res.success) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            if (res.success) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🐬 MariaDB: ${res.mariaDbUsers} Users, ${res.mariaDbProducts} Products | ⚡ Redis: ${res.redisKeys} Keys | 🍃 Mongo: ${res.mongoDocuments} Docs (${res.durationMs}ms)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Performance Benchmarker Card
        SurfaceCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Real-Time DB Benchmarking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Measures mobile throughput (ops/sec) and round-trip latency.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = onRunBenchmark,
                        enabled = !uiState.studioIsLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_run_benchmark")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Benchmark", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                uiState.studioBenchmarkResult?.let { bench ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BenchmarkMetricItem("Redis Throughput", "${bench.redisOpsPerSec.toInt()} ops/s", Color(0xFFEF4444), isDark, Modifier.weight(1f))
                        BenchmarkMetricItem("Redis Latency", "%.2f ms".format(bench.redisAvgLatencyMs), Color(0xFFF59E0B), isDark, Modifier.weight(1f))
                        BenchmarkMetricItem("MariaDB Query", "%.1f ms".format(bench.sqlQueryLatencyMs), Color(0xFF0284C7), isDark, Modifier.weight(1f))
                        BenchmarkMetricItem("MongoDB Ping", "%.1f ms".format(bench.mongoLatencyMs), Color(0xFF10B981), isDark, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BenchmarkMetricItem(
    label: String,
    value: String,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ============================================================================
// COMMON HELPERS
// ============================================================================

@Composable
private fun SurfaceCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
private fun EngineSelectButton(
    title: String,
    engine: String,
    isSelected: Boolean,
    brandColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) brandColor else if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF))
            .border(1.dp, if (isSelected) brandColor else if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("engine_chip_$engine")
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
        )
    }
}
