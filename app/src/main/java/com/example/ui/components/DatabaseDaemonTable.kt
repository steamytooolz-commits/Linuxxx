package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.PortInfo

data class DaemonRowData(
    val name: String,
    val binary: String,
    val port: Int,
    val defaultPath: String,
    val status: String,
    val latency: String,
    val isRunning: Boolean
)

/**
 * Responsive Daemon Matrix & Port Mapping Table with high readability and server-grade contrast.
 */
@Composable
fun DatabaseDaemonTable(
    ports: Map<Int, PortInfo>,
    isServiceRunning: Boolean,
    onProbeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val daemons = listOf(
        DaemonRowData(
            name = "MariaDB (MySQL)",
            binary = "mysqld_safe",
            port = 3306,
            defaultPath = "home/mysql_data",
            status = if (ports[3306]?.isOpen == true) "ONLINE" else if (isServiceRunning) "STARTING" else "STOPPED",
            latency = ports[3306]?.let { if (it.isOpen && it.latencyMs >= 0) "${it.latencyMs}ms" else "-" } ?: "-",
            isRunning = ports[3306]?.isOpen == true
        ),
        DaemonRowData(
            name = "Redis In-Memory",
            binary = "redis-server",
            port = 6379,
            defaultPath = "home/redis_data",
            status = if (ports[6379]?.isOpen == true) "ONLINE" else if (isServiceRunning) "STARTING" else "STOPPED",
            latency = ports[6379]?.let { if (it.isOpen && it.latencyMs >= 0) "${it.latencyMs}ms" else "-" } ?: "-",
            isRunning = ports[6379]?.isOpen == true
        ),
        DaemonRowData(
            name = "MongoDB NoSQL",
            binary = "mongod",
            port = 27017,
            defaultPath = "home/mongo_data",
            status = if (ports[27017]?.isOpen == true) "ONLINE" else if (isServiceRunning) "STARTING" else "STOPPED",
            latency = ports[27017]?.let { if (it.isOpen && it.latencyMs >= 0) "${it.latencyMs}ms" else "-" } ?: "-",
            isRunning = ports[27017]?.isOpen == true
        )
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("database_daemon_table")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Table Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Daemon Table",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DAEMON PROCESS MATRIX",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFFF1F5F9),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0284C7))
                            .clickable(onClick = onProbeClick)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Sensors,
                                contentDescription = "Probe Sockets Now",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Probe",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "127.0.0.1",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERVICE",
                    modifier = Modifier.weight(1.3f),
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "PORT",
                    modifier = Modifier.weight(0.7f),
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "DATA PATH",
                    modifier = Modifier.weight(1.2f),
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "STATUS",
                    modifier = Modifier.weight(0.8f),
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rows
            daemons.forEachIndexed { index, daemon ->
                DaemonTableRow(daemon = daemon)
                if (index < daemons.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF1E293B))
                    )
                }
            }
        }
    }
}

@Composable
private fun DaemonTableRow(daemon: DaemonRowData) {
    val statusColor = if (daemon.isRunning) Color(0xFF4ADE80) else Color(0xFF94A3B8)
    val statusBg = if (daemon.isRunning) Color(0x2622C55E) else Color(0x1F64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Service column with status bullet
        Row(
            modifier = Modifier.weight(1.3f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (daemon.isRunning) Color(0xFF22C55E) else Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = daemon.name,
                    color = Color(0xFFF1F5F9),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Text(
                    text = daemon.binary,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }

        // Port column
        Text(
            text = "${daemon.port}",
            modifier = Modifier.weight(0.7f),
            color = Color(0xFF38BDF8),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        // Storage Path column
        Text(
            text = daemon.defaultPath,
            modifier = Modifier.weight(1.2f),
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        // Status Badge column
        Box(
            modifier = Modifier
                .weight(0.8f)
                .clip(RoundedCornerShape(6.dp))
                .background(statusBg)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (daemon.isRunning && daemon.latency != "-") daemon.latency else daemon.status,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}
