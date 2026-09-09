package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainUiState

/**
 * Developer Code Playground supporting Node.js, Python, MariaDB SQL, Redis, and Shell scripts.
 * Includes interactive templates, quick keyboard accessory row, live runner, and embedded output logs.
 */
@Composable
fun PlaygroundScreen(
    uiState: MainUiState,
    onCodeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRunScript: (code: String, language: String) -> Unit,
    onStopScript: () -> Unit,
    onNavigateToScreen: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode
    val clipboardManager = LocalClipboardManager.current
    var selectedTemplateIndex by remember { mutableStateOf(0) }

    val templates = listOf(
        PlaygroundTemplate(
            name = "Node.js Web API",
            lang = "javascript",
            icon = "⚡",
            code = """
// Node.js HTTP Web Server & JSON API
const http = require('http');

const PORT = 3000;
const server = http.createServer((req, res) => {
  res.writeHead(200, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*'
  });

  const responseData = {
    status: 'online',
    runtime: 'Node.js on Android (Termux Userland)',
    endpoint: req.url,
    timestamp: new Date().toISOString(),
    databases: {
      mariadb: '127.0.0.1:3306',
      redis: '127.0.0.1:6379',
      mongodb: '127.0.0.1:27017'
    },
    message: 'Hello from your Node.js Developer Code Playground!'
  };

  res.end(JSON.stringify(responseData, null, 2));
});

server.listen(PORT, '127.0.0.1', () => {
  console.log('🚀 Node.js server running live at http://127.0.0.1:' + PORT);
  console.log('✨ Open the Live Web Preview tab to test your API!');
});
""".trimIndent()
        ),
        PlaygroundTemplate(
            name = "Node DB Client",
            lang = "javascript",
            icon = "🗄️",
            code = """
// Node.js Localhost Database Connector Test
const net = require('net');

const ports = [
  { name: 'MariaDB', port: 3306 },
  { name: 'Redis', port: 6379 },
  { name: 'MongoDB', port: 27017 }
];

console.log('🔍 Probing local database daemons from Node.js runtime...');

ports.forEach(item => {
  const start = Date.now();
  const socket = new net.Socket();
  socket.setTimeout(400);

  socket.connect(item.port, '127.0.0.1', () => {
    const latency = Date.now() - start;
    console.log('✔ [' + item.name + '] socket 127.0.0.1:' + item.port + ' is OPEN (' + latency + 'ms latency)');
    socket.destroy();
  });

  socket.on('error', (err) => {
    console.log('✖ [' + item.name + '] socket 127.0.0.1:' + item.port + ' closed or unreachable');
  });
});
""".trimIndent()
        ),
        PlaygroundTemplate(
            name = "Python 3 Server",
            lang = "python",
            icon = "🐍",
            code = """
# Python 3 Lightweight HTTP Server & API
import http.server
import socketserver
import json
from datetime import datetime

PORT = 8000

class PlaygroundHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        payload = {
            "status": "success",
            "runtime": "Python 3 on Linux Sandbox",
            "server_time": datetime.now().isoformat(),
            "port": PORT,
            "message": "Python Playground API is serving requests!"
        }
        self.wfile.write(json.dumps(payload, indent=2).encode('utf-8'))

with socketserver.TCPServer(("127.0.0.1", PORT), PlaygroundHandler) as httpd:
    print("🐍 Python server serving at http://127.0.0.1:" + str(PORT))
    httpd.serve_forever()
""".trimIndent()
        ),
        PlaygroundTemplate(
            name = "MariaDB / SQL",
            lang = "sql",
            icon = "🐬",
            code = """
-- MariaDB / MySQL Schema & Query Playground
CREATE DATABASE IF NOT EXISTS playground_db;
USE playground_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (username, email) VALUES 
('developer', 'dev@example.local'),
('admin', 'admin@example.local');

SELECT * FROM users;
SHOW TABLES;
""".trimIndent()
        ),
        PlaygroundTemplate(
            name = "Redis In-Memory",
            lang = "redis",
            icon = "🔑",
            code = """
# Redis Key-Value & Cache Playground
SET user:1001:name "DevPlayground"
SET user:1001:tier "pro"
HSET session:android token "abc123xyz" ip "127.0.0.1" expires 3600
INCR analytics:views
KEYS *
GET user:1001:name
HGETALL session:android
""".trimIndent()
        ),
        PlaygroundTemplate(
            name = "Shell Benchmark",
            lang = "shell",
            icon = "🐚",
            code = """
#!/system/bin/sh
echo "=== LINUX USERLAND PLAYGROUND BENCHMARK ==="
echo "Host: " `uname -a`
echo "Uptime: " `uptime`
echo "--- Storage & Partitions ---"
df -h
echo "--- Active Processes ---"
ps -ef 2>/dev/null | grep -E 'node|python|mysql|redis|mongo' || ps | head -n 15
echo "=== READY ==="
""".trimIndent()
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Toolbar: Template Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEachIndexed { index, template ->
                val isSelected = selectedTemplateIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) {
                                if (isDark) Color(0xFF0284C7) else Color(0xFF0284C7)
                            } else {
                                if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTemplateIndex = index
                            onLanguageChange(template.lang)
                            onCodeChange(template.code)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = template.icon, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Code Editor Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                // Editor Title & Action Header
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
                                .background(if (uiState.playgroundIsRunning) Color(0xFF22C55E) else Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CODE EDITOR (${uiState.playgroundLanguage.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.playgroundCode))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                onCodeChange(templates[selectedTemplateIndex].code)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Code",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Editable Code Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF030712) else Color(0xFFF8FAFC))
                        .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    BasicTextField(
                        value = uiState.playgroundCode,
                        onValueChange = onCodeChange,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                        ),
                        cursorBrush = SolidColor(if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("playground_code_editor")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Keyboard Bar for coding shortcuts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickKeys = listOf(
                        "TAB" to "  ",
                        "{" to "{}",
                        "(" to "()",
                        "[" to "[]",
                        ";" to ";",
                        "\"" to "\"\"",
                        "'" to "''",
                        "const" to "const ",
                        "log" to "console.log()",
                        "async" to "async ",
                        "def" to "def ",
                        "import" to "import "
                    )

                    quickKeys.forEach { (label, insertText) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                .clickable {
                                    onCodeChange(uiState.playgroundCode + insertText)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }
        }

        // Runner Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (uiState.playgroundIsRunning) {
                        onStopScript()
                    } else {
                        onRunScript(uiState.playgroundCode, uiState.playgroundLanguage)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.playgroundIsRunning) Color(0xFFDC2626) else Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1.4f)
                    .height(44.dp)
                    .testTag("playground_run_button")
            ) {
                if (uiState.playgroundIsRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP SCRIPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RUN PLAYGROUND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Live Web Preview Button (instant jump to embedded browser)
            OutlinedButton(
                onClick = { onNavigateToScreen(AppScreen.PREVIEW) },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
                    .testTag("playground_open_preview_button")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Live Preview",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                )
            }
        }

        // Live Output & Console Logs
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OUTPUT & LOGS",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                    }

                    if (uiState.playgroundServerPort > 0) {
                        Text(
                            text = "Port :${uiState.playgroundServerPort}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF030712) else Color(0xFFF8FAFC))
                        .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (uiState.playgroundOutput.isEmpty()) {
                            "// Click 'RUN PLAYGROUND' above to execute this code."
                        } else {
                            uiState.playgroundOutput
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = if (uiState.playgroundOutput.isEmpty()) {
                            if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        } else {
                            if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A)
                        }
                    )
                }
            }
        }
    }
}

private data class PlaygroundTemplate(
    val name: String,
    val lang: String,
    val icon: String,
    val code: String
)
