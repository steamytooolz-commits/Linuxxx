package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TerminalLogItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ultra-high contrast Terminal Console for real Linux userland interactive commands and process streams.
 */
@Composable
fun TerminalConsole(
    logs: List<TerminalLogItem>,
    isAutoScroll: Boolean,
    onToggleAutoScroll: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    onExecuteCommand: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    var commandInput by remember { mutableStateOf("") }
    
    // Multi-tab and filtering state
    var selectedTab by remember { mutableStateOf("bash") }
    val commandHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }

    val quickCommands = listOf(
        "uname -a",
        "whoami",
        "pwd",
        "node -v",
        "python3 --version",
        "mariadb --version",
        "redis-cli ping",
        "ls -la \$PREFIX/bin",
        "env | sort",
        "ps -ef",
        "df -h",
        "apk info"
    )

    val keyBarItems = listOf(
        "↑", "↓", "CTRL", "ALT", "TAB", "ESC", "|", "/", "-", "~", "grep", "CLEAR"
    )

    // Filtered log list depending on selected tab
    val filteredLogs = remember(logs, selectedTab) {
        when (selectedTab) {
            "mariadb" -> logs.filter { it.tag in listOf("MYSQL", "CLI", "OUT", "ERROR") && (it.message.contains("mysql", ignoreCase = true) || it.message.contains("mariadb", ignoreCase = true) || it.tag == "MYSQL") }
            "redis" -> logs.filter { it.tag in listOf("REDIS", "CLI", "OUT", "ERROR") && (it.message.contains("redis", ignoreCase = true) || it.tag == "REDIS") }
            "mongo" -> logs.filter { it.tag in listOf("MONGO", "CLI", "OUT", "ERROR") && (it.message.contains("mongo", ignoreCase = true) || it.tag == "MONGO") }
            "errors" -> logs.filter { it.isError || it.tag.contains("ERR") }
            else -> logs
        }
    }

    // Auto-scroll to latest log if enabled
    LaunchedEffect(filteredLogs.size, isAutoScroll) {
        if (isAutoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    fun submitCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        if (commandHistory.isEmpty() || commandHistory.last() != trimmed) {
            commandHistory.add(trimmed)
        }
        historyIndex = -1
        onExecuteCommand(trimmed)
        commandInput = ""
    }

    Column(
        modifier = modifier
            .testTag("terminal_output_box")
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF030712)) // Pure dark slate/pitch black
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        // Terminal Window Titlebar with Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Window Control Dots
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "LINUX USERLAND SHELL // \$PREFIX",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${filteredLogs.size} lines",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = { onToggleAutoScroll(!isAutoScroll) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_autoscroll_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Auto Scroll",
                            tint = if (isAutoScroll) Color(0xFF38BDF8) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Console",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Multi-tab Shell Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    "bash" to "Main Shell",
                    "mariadb" to "MariaDB Logs",
                    "redis" to "Redis Stream",
                    "mongo" to "Mongo Logs",
                    "errors" to "Errors Only"
                )
                items(tabs) { tab ->
                    val key = tab.first
                    val title = tab.second
                    val isTabSelected = selectedTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTabSelected) Color(0xFF1E293B) else Color.Transparent)
                            .border(1.dp, if (isTabSelected) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(6.dp))
                            .clickable { selectedTab = key }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isTabSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Terminal Log Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF030712))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "root@android-userland:~# [$selectedTab]",
                        color = Color(0xFF4ADE80),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "100% Real Linux Shell Active. Type any command below or tap quick chips to execute.",
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { item ->
                        TerminalLogRow(item = item, timeStr = timeFormat.format(Date(item.timestamp)))
                    }
                }
            }
        }

        // Interactive Command Bar & Key Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Terminal Key Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(keyBarItems) { key ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (key == "CLEAR") Color(0xFF991B1B) else Color(0xFF1E293B))
                            .clickable {
                                when (key) {
                                    "CLEAR" -> onClearLogs()
                                    "TAB" -> commandInput += "  "
                                    "↑" -> {
                                        if (commandHistory.isNotEmpty()) {
                                            val newIdx = if (historyIndex == -1) commandHistory.size - 1 else (historyIndex - 1).coerceAtLeast(0)
                                            historyIndex = newIdx
                                            commandInput = commandHistory[newIdx]
                                        }
                                    }
                                    "↓" -> {
                                        if (commandHistory.isNotEmpty() && historyIndex != -1) {
                                            val newIdx = historyIndex + 1
                                            if (newIdx < commandHistory.size) {
                                                historyIndex = newIdx
                                                commandInput = commandHistory[newIdx]
                                            } else {
                                                historyIndex = -1
                                                commandInput = ""
                                            }
                                        }
                                    }
                                    "CTRL", "ALT", "ESC" -> {}
                                    else -> commandInput += key
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = key,
                            color = if (key == "CLEAR") Color(0xFFFECACA) else Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick Command Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickCommands) { cmd ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                submitCommand(cmd)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cmd,
                            color = Color(0xFF86EFAC), // Bright Lime
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$",
                    color = Color(0xFF4ADE80),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = {
                        Text(
                            text = "Run shell command (e.g., node -v, python3, ls)...",
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { submitCommand(commandInput) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFF8FAFC),
                        cursorColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cli_command_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { submitCommand(commandInput) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB))
                        .testTag("cli_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Run Command",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalLogRow(
    item: TerminalLogItem,
    timeStr: String
) {
    val tagColor = when (item.tag) {
        "CLI", "IN" -> Color(0xFF4ADE80)      // Bright Lime Green
        "OUT" -> Color(0xFFF8FAFC)            // Bright Pure White
        "MYSQL" -> Color(0xFF38BDF8)          // Bright Cyan
        "REDIS" -> Color(0xFFFB7185)          // Bright Rose
        "MONGO" -> Color(0xFF34D399)          // Bright Emerald
        "STACK" -> Color(0xFFFBBF24)          // Bright Amber
        "BOOTSTRAP" -> Color(0xFFC084FC)      // Bright Purple
        "NET" -> Color(0xFF38BDF8)            // Bright Sky
        "PLAYGROUND" -> Color(0xFFFACC15)     // Bright Yellow
        "ERROR", "STACK-ERR", "PLAYGROUND-ERR" -> Color(0xFFF87171) // Bright Red
        else -> Color(0xFFCBD5E1)             // High Contrast Slate White
    }

    // Default message text color is strictly high contrast white/light slate (never black or dark blue!)
    val textColor = when {
        item.isError -> Color(0xFFFCA5A5)
        item.tag == "CLI" -> Color(0xFF4ADE80)
        item.tag == "OUT" -> Color(0xFFF8FAFC)
        else -> Color(0xFFE2E8F0)
    }

    val parsedAnnotatedMessage = remember(item.message, textColor) {
        AnsiColorParser.parseAnsiToAnnotatedString(item.message, textColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = timeStr,
            color = Color(0xFF64748B), // Slate gray timestamp
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Tag badge
        Text(
            text = "[${item.tag}]",
            color = tagColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Message text with ANSI formatting support and EXPLICIT high contrast color
        Text(
            text = parsedAnnotatedMessage,
            color = textColor, // Ensures explicit high contrast on dark canvas
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

