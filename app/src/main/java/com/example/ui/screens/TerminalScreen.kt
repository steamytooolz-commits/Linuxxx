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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState
import com.example.ui.components.AnsiColorParser
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    uiState: MainUiState,
    onStartInteractiveShell: (String) -> Unit,
    onSendInteractiveInput: (String) -> Unit,
    onSendControlSignal: (String) -> Unit,
    onKillInteractiveSession: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode
    val terminalLogs = uiState.logs.filter {
        it.tag in listOf("IN", "OUT", "SHELL", "SHELL-ERR", "CLI", "CLI-ERR", "APPLIANCE", "SERVICE", "PROOT", "CONTAINER", "REDIS", "MARIADB", "MONGODB")
    }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var cmdInput by remember { mutableStateOf("") }

    // Auto-start shell if not active
    LaunchedEffect(Unit) {
        if (!uiState.isInteractiveSessionActive) {
            onStartInteractiveShell("")
        }
    }

    // Auto-scroll logic
    LaunchedEffect(terminalLogs.size) {
        if (uiState.isAutoScrollEnabled && terminalLogs.isNotEmpty()) {
            lazyListState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Interactive Shell Connection Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INTERACTIVE LINUX CONSOLE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = if (uiState.isInteractiveSessionActive) {
                                "Connected: ubuntu [PID: ${uiState.activeSessionPid}]"
                            } else {
                                "Inactive: Connect to launch sandbox bash shell"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Live Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.isInteractiveSessionActive) Color(0x2622C55E) else Color(0x2664748B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isInteractiveSessionActive) Color(0xFF22C55E) else Color(0xFF94A3B8))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isInteractiveSessionActive) "ACTIVE" else "CLOSED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (uiState.isInteractiveSessionActive) Color(0xFF22C55E) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!uiState.isInteractiveSessionActive) {
                        Button(
                            onClick = { onStartInteractiveShell("") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("terminal_screen_start_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START BASH CONSOLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Button(
                            onClick = onKillInteractiveSession,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("terminal_screen_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TERMINATE SESSION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onClearLogs,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(0.5f)
                            .height(40.dp)
                            .testTag("terminal_screen_clear_button")
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Terminal Output Screen Viewport
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF020617) // Pure pitch black obsidian console
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF1E293B) else Color(0xFF334155)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .testTag("terminal_log_output")
                ) {
                    if (terminalLogs.isEmpty()) {
                        item {
                            Text(
                                text = "$ bash --version\nGNU bash, version 5.2.26-release (aarch64-unknown-linux-gnu)\n\nClick 'START BASH CONSOLE' above to open interactive bash prompt inside your rootless Ubuntu 24.04 PRoot Container.\n\nYou can run commands like:\n - apt update && apt install curl\n - mariadb -u root\n - redis-cli ping\n - mongosh\n",
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        items(terminalLogs, key = { it.id }) { logItem ->
                            val defaultColor = when (logItem.tag) {
                                "IN" -> Color(0xFF38BDF8)
                                "SHELL-ERR", "CLI-ERR" -> Color(0xFFEF4444)
                                "SHELL" -> Color(0xFF10B981)
                                "CLI" -> Color(0xFF60A5FA)
                                "APPLIANCE", "SERVICE" -> Color(0xFFA855F7)
                                "MARIADB" -> Color(0xFFF59E0B)
                                "REDIS" -> Color(0xFFEF4444)
                                "MONGODB" -> Color(0xFF22C55E)
                                else -> Color(0xFFE2E8F0)
                            }
                            val annotatedText = AnsiColorParser.parseAnsiToAnnotatedString(
                                logItem.message,
                                defaultColor
                            )
                            Text(
                                text = annotatedText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Command Chips
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickCmds = listOf(
                "status",
                "redis-cli PING",
                "redis-cli KEYS *",
                "mariadb -e \"SHOW TABLES;\"",
                "mariadb -e \"SELECT * FROM users;\"",
                "mongosh",
                "seed",
                "ps",
                "ls -la",
                "help"
            )
            items(quickCmds) { qCmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        .clickable { onSendInteractiveInput(qCmd) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = qCmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                }
            }
        }

        // Auxiliary Keyboard Panel (Ctrl+C, Ctrl+D, Tab, Clear, etc)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val keys = listOf("CTRL_C", "CTRL_D", "TAB")
            keys.forEach { key ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        .clickable { onSendControlSignal(key) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (key) {
                            "CTRL_C" -> "Ctrl+C"
                            "CTRL_D" -> "Ctrl+D"
                            else -> "Tab ↹"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )
                }
            }
        }

        // Interactive Command Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = cmdInput,
                onValueChange = { cmdInput = it },
                placeholder = {
                    Text(
                        text = "Enter terminal command...",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("terminal_command_input"),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
                    unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (cmdInput.isNotBlank()) {
                            onSendInteractiveInput(cmdInput)
                            cmdInput = ""
                        }
                    }
                ),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (cmdInput.isNotBlank()) {
                        onSendInteractiveInput(cmdInput)
                        cmdInput = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0284C7))
                    .testTag("terminal_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
