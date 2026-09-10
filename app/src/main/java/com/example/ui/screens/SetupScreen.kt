package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.SetupStep
import com.example.core.UbuntuRootfsManager
import com.example.service.DatabaseStackService
import com.example.ui.MainUiState
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    uiState: MainUiState,
    onSetupCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rootfsManager = remember { UbuntuRootfsManager(context) }
    val setupState by rootfsManager.setupState.collectAsState()

    var statusLogs by remember { mutableStateOf(listOf("Ready to configure Linuxxx Appliance.")) }
    var isRunningSetup by remember { mutableStateOf(false) }

    fun addLog(msg: String) {
        statusLogs = (statusLogs + msg).takeLast(10)
    }

    val isReady = rootfsManager.isEnvironmentReady()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐧", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Linuxxx Appliance",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Rootless Ubuntu ARM64 • MariaDB • Redis • MongoDB",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Setup Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENVIRONMENT STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isReady) Color(0x2210B981) else Color(0x22F59E0B)
                    ) {
                        Text(
                            text = if (isReady) "READY" else "UNINITIALIZED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { setupState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("setup_progress_bar"),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B)
                )

                Text(
                    text = setupState.description,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace
                )

                // Sequence checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SetupStepRow("1. Deploy PRoot ARM64 binary", isReady || setupState.progress >= 0.1f)
                    SetupStepRow("2. Download Ubuntu 24.04 ARM64 rootfs (.tar.xz)", isReady || setupState.progress >= 0.55f)
                    SetupStepRow("3. Extract rootfs (verify /bin/bash)", isReady || setupState.progress >= 0.9f)
                    SetupStepRow("4. Install start-all.sh & db configs", isReady || setupState.progress >= 0.95f)
                    SetupStepRow("5. Start DatabaseStackService (:3306, :6379, :27017)", isReady && uiState.isServiceRunning)
                }

                // Live status logs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "INSTALLER LOGS",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                    statusLogs.forEach { log ->
                        Text(
                            text = "> $log",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Action Buttons
        Button(
            onClick = {
                isRunningSetup = true
                coroutineScope.launch {
                    addLog("Starting Linuxxx environment setup...")
                    val result = rootfsManager.installStack { msg ->
                        addLog(msg)
                    }
                    if (result.isSuccess) {
                        addLog("Stack installed! Starting background service...")
                        DatabaseStackService.start(context)
                        addLog("DatabaseStackService active.")
                        onSetupCompleted()
                    } else {
                        addLog("Setup error: ${result.exceptionOrNull()?.message}")
                    }
                    isRunningSetup = false
                }
            },
            enabled = !isRunningSetup,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_run_setup"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White
            )
        ) {
            if (isRunningSetup) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Initializing...", fontFamily = FontFamily.Monospace)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isReady) "Restart / Verify Stack" else "Start First-Launch Setup",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedButton(
            onClick = {
                DatabaseStackService.start(context)
                onSetupCompleted()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_open_workspace"),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
        ) {
            Text(
                text = "Open CodeMirror 6 Workspace",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SetupStepRow(title: String, isDone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isDone) Color(0xFF10B981) else Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Text(
            text = title,
            fontSize = 11.sp,
            color = if (isDone) Color(0xFFE2E8F0) else Color(0xFF64748B),
            fontFamily = FontFamily.Monospace
        )
    }
}
