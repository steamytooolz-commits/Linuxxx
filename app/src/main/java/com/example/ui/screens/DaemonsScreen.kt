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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.ui.MainUiState
import com.example.ui.components.DatabaseDaemonTable

@Composable
fun DaemonsScreen(
    uiState: MainUiState,
    onToggleService: () -> Unit,
    onProbePorts: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Stack Action Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DATABASE SERVICE CONTROL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Foremost Android lifecycle background supervisor",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }

                    // Live Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.isServiceRunning) Color(0x2622C55E) else Color(0x2664748B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isServiceRunning) Color(0xFF22C55E) else Color(0xFF94A3B8))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isServiceRunning) "ACTIVE" else "INACTIVE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (uiState.isServiceRunning) Color(0xFF22C55E) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleService,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isServiceRunning) Color(0xFFDC2626) else Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .testTag("daemons_screen_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isServiceRunning) "STOP ENTIRE STACK" else "START ENTIRE STACK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = onProbePorts,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("daemons_screen_probe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Probe",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }
                }
            }
        }

        // Full Daemon Process Matrix Table
        DatabaseDaemonTable(
            ports = uiState.ports,
            isServiceRunning = uiState.isServiceRunning,
            onProbeClick = onProbePorts
        )

        // Individual Daemon Command Triggers
        Text(
            text = "INDIVIDUAL DAEMON CLI CONTROLS",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            letterSpacing = 1.sp
        )

        val individualDaemons = listOf(
            DaemonControlItem(
                name = "MariaDB / MySQL",
                port = 3306,
                startCmd = "mysqld_safe --datadir=\$MYSQL_DATA_DIR --port=3306 --innodb_buffer_pool_size=${uiState.innodbBufferPoolMb}M &",
                stopCmd = "pkill mysqld",
                restartCmd = "pkill mysqld; sleep 1; mysqld_safe --datadir=\$MYSQL_DATA_DIR --port=3306 --innodb_buffer_pool_size=${uiState.innodbBufferPoolMb}M &",
                checkCmd = "mysqladmin ping -u root"
            ),
            DaemonControlItem(
                name = "Redis In-Memory",
                port = 6379,
                startCmd = "redis-server \$PREFIX/etc/redis.conf --dir \$REDIS_DATA_DIR &",
                stopCmd = "redis-cli shutdown || pkill redis-server",
                restartCmd = "(redis-cli shutdown || pkill redis-server); sleep 1; redis-server \$PREFIX/etc/redis.conf --dir \$REDIS_DATA_DIR &",
                checkCmd = "redis-cli ping"
            ),
            DaemonControlItem(
                name = "MongoDB NoSQL",
                port = 27017,
                startCmd = "mongod --dbpath \$MONGO_DATA_DIR --port 27017 --wiredTigerCacheSizeGB ${(uiState.wiredTigerCacheSizeMb / 1024f)} &",
                stopCmd = "mongod --shutdown || pkill mongod",
                restartCmd = "(mongod --shutdown || pkill mongod); sleep 1; mongod --dbpath \$MONGO_DATA_DIR --port 27017 --wiredTigerCacheSizeGB ${(uiState.wiredTigerCacheSizeMb / 1024f)} &",
                checkCmd = "mongosh --eval 'db.runCommand({ping:1})' 2>/dev/null"
            )
        )

        individualDaemons.forEach { daemon ->
            val isRunning = uiState.ports[daemon.port]?.isOpen == true

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = daemon.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ":${daemon.port}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (isRunning) "ONLINE" else "OFFLINE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isRunning) Color(0xFF22C55E) else Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onExecuteCommand(daemon.startCmd) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(
                                text = "Start",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF059669)
                            )
                        }

                        OutlinedButton(
                            onClick = { onExecuteCommand(daemon.stopCmd) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(
                                text = "Stop",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                            )
                        }

                        OutlinedButton(
                            onClick = { onExecuteCommand(daemon.restartCmd) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(
                                text = "Restart",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                            )
                        }

                        OutlinedButton(
                            onClick = { onExecuteCommand(daemon.checkCmd) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(
                                text = "Ping",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }
        }

        // Live Configuration File Editor Card
        ConfigurationEditorCard(
            prefixPath = uiState.prefixPath,
            isDark = isDark,
            onExecuteCommand = onExecuteCommand
        )
    }
}

@Composable
private fun ConfigurationEditorCard(
    prefixPath: String,
    isDark: Boolean,
    onExecuteCommand: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val configMap = remember(prefixPath) {
        mapOf(
            "MariaDB (my.cnf)" to File("$prefixPath/etc/my.cnf"),
            "Redis (redis.conf)" to File("$prefixPath/etc/redis.conf"),
            "MongoDB (mongod.conf)" to File("$prefixPath/etc/mongod.conf")
        )
    }

    var selectedConfigKey by remember { mutableStateOf("MariaDB (my.cnf)") }
    val currentFile = configMap[selectedConfigKey] ?: File("$prefixPath/etc/my.cnf")

    var contentText by remember(currentFile.absolutePath) { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadContent() {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            if (!currentFile.exists()) {
                try {
                    currentFile.parentFile?.mkdirs()
                    currentFile.createNewFile()
                } catch (_: Exception) {}
            }
            val text = try { currentFile.readText() } catch (e: Exception) { "# Error reading file: ${e.message}" }
            withContext(Dispatchers.Main) {
                contentText = text
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentFile.absolutePath) {
        loadContent()
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIGURATION EDITOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val success = try {
                                currentFile.writeText(contentText)
                                true
                            } catch (e: Exception) {
                                false
                            }
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    statusMsg = "Saved: ${currentFile.name}"
                                    onExecuteCommand("echo 'Configuration saved to ${currentFile.name}'")
                                } else {
                                    statusMsg = "Error saving configuration"
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Config", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Config Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                configMap.keys.forEach { key ->
                    val isSelected = selectedConfigKey == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF0284C7) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            .clickable {
                                selectedConfigKey = key
                                statusMsg = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = key,
                            color = if (isSelected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (statusMsg != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMsg ?: "",
                    color = Color(0xFF10B981),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color(0xFF0B101E) else Color(0xFFF8FAFC),
                    unfocusedContainerColor = if (isDark) Color(0xFF0B101E) else Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

private data class DaemonControlItem(
    val name: String,
    val port: Int,
    val startCmd: String,
    val stopCmd: String,
    val restartCmd: String,
    val checkCmd: String
)
