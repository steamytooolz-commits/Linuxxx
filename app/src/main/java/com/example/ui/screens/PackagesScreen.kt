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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState

@Composable
fun PackagesScreen(
    uiState: MainUiState,
    onInstallPackage: (String) -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode
    var customPackageName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Package Hub Header
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF0284C7) else Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DEVELOPER PACKAGE HUB & RUNTIMES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Install Node.js, Python, Git, and developer CLI tools into Linux userland",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Package Install Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF030712) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = customPackageName,
                            onValueChange = { customPackageName = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            ),
                            cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (customPackageName.isEmpty()) {
                                    Text(
                                        text = "Package name (e.g. nodejs, git, curl, htop)...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            if (customPackageName.isNotBlank()) {
                                onInstallPackage(customPackageName)
                                customPackageName = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Developer Runtimes One-Tap Install Cards
        Text(
            text = "ESSENTIAL RUNTIMES & TOOLS",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            letterSpacing = 1.sp
        )

        val runtimePackages = listOf(
            RuntimeItem(
                name = "Node.js & npm",
                tag = "⚡ JS Runtime",
                packageName = "nodejs npm",
                desc = "V8 JavaScript engine with npm package ecosystem for Express, APIs & scripts.",
                testCmd = "node --version && npm --version"
            ),
            RuntimeItem(
                name = "Python 3 & pip",
                tag = "🐍 Python",
                packageName = "python3 py3-pip",
                desc = "Python 3 interpreter with pip package manager for FastAPI, Flask & analytics.",
                testCmd = "python3 --version"
            ),
            RuntimeItem(
                name = "Git Version Control",
                tag = "🐙 Git",
                packageName = "git",
                desc = "Clone repositories, manage branches, and sync project workspaces.",
                testCmd = "git --version"
            ),
            RuntimeItem(
                name = "cURL & Wget",
                tag = "🌐 Network",
                packageName = "curl wget",
                desc = "Command-line HTTP/REST client for querying local endpoints and remote APIs.",
                testCmd = "curl --version"
            ),
            RuntimeItem(
                name = "SQLite 3 Engine",
                tag = "🗄️ Database",
                packageName = "sqlite",
                desc = "Embedded serverless SQL database engine for local data storage.",
                testCmd = "sqlite3 --version"
            ),
            RuntimeItem(
                name = "Neofetch & Htop",
                tag = "📊 System Telemetry",
                packageName = "neofetch htop",
                desc = "System information display and terminal interactive process viewer.",
                testCmd = "neofetch 2>/dev/null || uname -a"
            )
        )

        runtimePackages.forEach { item ->
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
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.tag,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.desc,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onInstallPackage(item.packageName) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install / Update", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { onExecuteCommand(item.testCmd) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test CLI", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                        }
                    }
                }
            }
        }

        // How Termux Userland Works Card
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HOW TERMUX / LINUX USERLAND WORKS ON ANDROID",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val explanationPoints = listOf(
                    "1. Rootless Userland" to "Android is based on the Linux kernel. Applications have their own Linux UID (e.g. u0_a123) and can execute native ELF binaries in their private data folder without root permissions.",
                    "2. Custom \$PREFIX Path" to "Standard Linux expects /usr/bin. Termux and our stack relocate \$PREFIX to /data/data/<package>/files/usr, setting \$PATH and \$LD_LIBRARY_PATH so Node.js, Python, MariaDB and Redis find their libraries.",
                    "3. Bionic Libc & Sockets" to "Binaries link with Android's Bionic C library and standard Linux system calls (fork, execve, socket, bind, listen) on 127.0.0.1 loopback.",
                    "4. Foremost Execution" to "Foreground services keep the Linux daemons alive in the background without Android OS battery-optimization killers terminating active server threads."
                )

                explanationPoints.forEach { (heading, body) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = heading,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                        Text(
                            text = body,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

private data class RuntimeItem(
    val name: String,
    val tag: String,
    val packageName: String,
    val desc: String,
    val testCmd: String
)
