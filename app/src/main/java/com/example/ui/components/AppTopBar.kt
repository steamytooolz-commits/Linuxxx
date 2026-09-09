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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

/**
 * Top App Bar with Navigation Drawer trigger, Screen title,
 * Live FGS indicator, Dark/Light Mode toggle, and quick action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    uiState: MainUiState,
    onOpenDrawer: () -> Unit,
    onToggleService: () -> Unit,
    onProbePorts: () -> Unit,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode

    TopAppBar(
        modifier = modifier.testTag("app_top_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF),
            navigationIconContentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            titleContentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            actionIconContentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
        ),
        navigationIcon = {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.testTag("open_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Drawer"
                )
            }
        },
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LINUX DB STACK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Live Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (uiState.isServiceRunning) Color(0x2622C55E) else Color(0x2664748B))
                            .border(
                                1.dp,
                                if (uiState.isServiceRunning) Color(0xFF22C55E) else Color(0xFF64748B),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isServiceRunning) Color(0xFF22C55E) else Color(0xFF94A3B8))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isServiceRunning) "FGS ONLINE" else "STOPPED",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isServiceRunning) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Text(
                    text = uiState.currentScreen.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        actions = {
            // Quick Probe Sockets Action
            IconButton(
                onClick = onProbePorts,
                modifier = Modifier.testTag("topbar_probe_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Probe Sockets",
                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                )
            }

            // Dark/Light Theme Switch Action
            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier.testTag("topbar_theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                    contentDescription = "Toggle Dark/Light Mode",
                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF475569)
                )
            }

            // Master Start / Stop Action
            IconButton(
                onClick = onToggleService,
                modifier = Modifier.testTag("topbar_toggle_service_button")
            ) {
                Icon(
                    imageVector = if (uiState.isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isServiceRunning) "Stop Stack" else "Start Stack",
                    tint = if (uiState.isServiceRunning) Color(0xFFEF4444) else Color(0xFF22C55E)
                )
            }
        }
    )
}
