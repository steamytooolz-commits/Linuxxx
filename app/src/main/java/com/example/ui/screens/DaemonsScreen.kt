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
                startCmd = "mysqld_safe --datadir=\$HOME/mysql_data --port=3306 &",
                stopCmd = "pkill mysqld",
                checkCmd = "mysqladmin ping -u root"
            ),
            DaemonControlItem(
                name = "Redis In-Memory",
                port = 6379,
                startCmd = "redis-server \$PREFIX/etc/redis.conf &",
                stopCmd = "redis-cli shutdown || pkill redis-server",
                checkCmd = "redis-cli ping"
            ),
            DaemonControlItem(
                name = "MongoDB NoSQL",
                port = 27017,
                startCmd = "mongod --dbpath \$HOME/mongo_data --port 27017 &",
                stopCmd = "mongod --shutdown || pkill mongod",
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onExecuteCommand(daemon.startCmd) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
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
                            onClick = { onExecuteCommand(daemon.checkCmd) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
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
    }
}

private data class DaemonControlItem(
    val name: String,
    val port: Int,
    val startCmd: String,
    val stopCmd: String,
    val checkCmd: String
)
