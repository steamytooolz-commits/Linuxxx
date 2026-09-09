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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.PortInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

@Composable
fun DiagnosticsScreen(
    uiState: MainUiState,
    onProbeAll: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode
    val coroutineScope = rememberCoroutineScope()
    var isProbing by remember { mutableStateOf(false) }
    var activeTcpListeners by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun refreshListeners() {
        coroutineScope.launch(Dispatchers.IO) {
            val ports = mutableListOf<Int>()
            val files = listOf(File("/proc/net/tcp"), File("/proc/net/tcp6"))
            for (f in files) {
                if (f.exists() && f.canRead()) {
                    try {
                        f.forEachLine { line ->
                            val tokens = line.trim().split(Regex("\\s+"))
                            if (tokens.size >= 4 && tokens[3].equals("0A", ignoreCase = true)) {
                                val localAddr = tokens[1]
                                val portHex = localAddr.substringAfterLast(":", "")
                                val portInt = portHex.toIntOrNull(16)
                                if (portInt != null && !ports.contains(portInt)) {
                                    ports.add(portInt)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            withContext(Dispatchers.Main) {
                activeTcpListeners = ports.sorted()
            }
        }
    }

    LaunchedEffect(uiState.isServiceRunning) {
        refreshListeners()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Diagnostics Banner
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF0284C7) else Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sensors,
                                contentDescription = null,
                                tint = if (isDark) Color.White else Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SOCKET DIAGNOSTICS & PROBE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Deep loopback socket reachability & kernel TCP listeners",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isProbing = true
                            onProbeAll()
                            refreshListeners()
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(600)
                                isProbing = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .testTag("run_full_socket_probe_button")
                    ) {
                        if (isProbing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Probing Sockets...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PROBE ALL PORTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onExecuteCommand("netstat -tlpn 2>/dev/null || ss -tlpn 2>/dev/null || cat /proc/net/tcp")
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Netstat CLI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }
                }
            }
        }

        // Database Daemon Socket Cards
        Text(
            text = "DATABASE SOCKET PORTS",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            letterSpacing = 1.sp
        )

        val daemonsList = listOf(
            Triple(3306, "MariaDB (MySQL)", "Standard relational SQL database socket"),
            Triple(6379, "Redis", "High-performance in-memory key-value data store"),
            Triple(27017, "MongoDB", "NoSQL document database engine with WiredTiger")
        )

        daemonsList.forEach { (port, name, desc) ->
            val portInfo = uiState.ports[port] ?: PortInfo(port, name)
            SocketDiagnosticCard(
                port = port,
                name = name,
                description = desc,
                portInfo = portInfo,
                isDarkMode = isDark,
                onTestIndividualPort = {
                    onExecuteCommand("echo '[PROBE] Connecting to 127.0.0.1:$port ($name)...'")
                    onProbeAll()
                }
            )
        }

        // Kernel TCP Listeners Card (/proc/net/tcp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KERNEL /proc/net/tcp LISTENERS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                    }

                    IconButton(
                        onClick = { refreshListeners() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (activeTcpListeners.isEmpty()) {
                    Text(
                        text = "No open TCP listeners detected on loopback. Start the database stack to open sockets.",
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeTcpListeners.forEach { p ->
                            val isDb = p == 3306 || p == 6379 || p == 27017
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDb) Color(0x2622C55E) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                    .border(
                                        1.dp,
                                        if (isDb) Color(0xFF22C55E) else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ":$p",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isDb) Color(0xFF22C55E) else if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
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
private fun SocketDiagnosticCard(
    port: Int,
    name: String,
    description: String,
    portInfo: PortInfo,
    isDarkMode: Boolean,
    onTestIndividualPort: () -> Unit
) {
    val isOpen = portInfo.isOpen

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOpen) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ":$port",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOpen) Color(0x2622C55E) else Color(0x1FEF4444))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isOpen) {
                            if (portInfo.latencyMs >= 0) "ONLINE (${portInfo.latencyMs}ms)" else "ONLINE"
                        } else "CLOSED",
                        color = if (isOpen) Color(0xFF22C55E) else Color(0xFFEF4444),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Endpoint: 127.0.0.1:$port",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .clickable(onClick = onTestIndividualPort)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Test Connection",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                }
            }
        }
    }
}
