package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState

@Composable
fun DatabaseStudioScreen(
    uiState: MainUiState,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode
    var selectedDbEngine by remember { mutableStateOf("SQL") } // SQL, REDIS, MONGO
    var queryInput by remember { mutableStateOf("SHOW DATABASES;") }

    val presetQueries = mapOf(
        "SQL" to listOf("SHOW DATABASES;", "SELECT 1 + 1 AS test_result;", "SHOW TABLES IN information_schema;", "SELECT version();"),
        "REDIS" to listOf("redis-cli PING", "redis-cli INFO server", "redis-cli SET app_key 'hello_linux'", "redis-cli KEYS *"),
        "MONGO" to listOf("mongosh --eval 'db.stats()'", "mongosh --eval 'show dbs'", "mongosh --eval 'db.version()'")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Engine Selector Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DATABASE STUDIO & QUERY WORKBENCH",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Execute live queries against MariaDB, Redis, and MongoDB",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Engine Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SQL" to "MariaDB SQL", "REDIS" to "Redis CLI", "MONGO" to "MongoDB Shell").forEach { (engine, label) ->
                        val isSelected = selectedDbEngine == engine
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) (if (isDark) Color(0xFF0284C7) else Color(0xFFE0F2FE)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)))
                                .clickable {
                                    selectedDbEngine = engine
                                    queryInput = presetQueries[engine]?.firstOrNull() ?: ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF0284C7)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                            )
                        }
                    }
                }
            }
        }

        // Query Workbench Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "QUERY EDITOR ($selectedDbEngine)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Presets
                Text(
                    text = "Quick Queries:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                presetQueries[selectedDbEngine]?.forEach { preset ->
                    Text(
                        text = "• $preset",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { queryInput = preset }
                            .padding(vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val fullCmd = when (selectedDbEngine) {
                            "SQL" -> "mariadb -e \"$queryInput\" || mysql -e \"$queryInput\""
                            "REDIS" -> if (queryInput.startsWith("redis-cli")) queryInput else "redis-cli $queryInput"
                            "MONGO" -> if (queryInput.startsWith("mongosh")) queryInput else "mongosh --eval \"$queryInput\""
                            else -> queryInput
                        }
                        onExecuteCommand(fullCmd)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "EXECUTE IN TERMINAL WORKBENCH", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
