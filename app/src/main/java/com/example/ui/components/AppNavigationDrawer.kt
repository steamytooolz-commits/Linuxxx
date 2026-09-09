package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainUiState

/**
 * Modern Material 3 Navigation Drawer with Server Overview, Screen Routing,
 * Dark/Light Mode Switcher, and Master Database Stack Controls.
 */
@Composable
fun AppDrawerContent(
    uiState: MainUiState,
    onSelectScreen: (AppScreen) -> Unit,
    onToggleService: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode

    Surface(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        color = if (isDark) Color(0xFF090D16) else Color(0xFFF8FAFC),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 14.dp)
        ) {
            // Server Identity Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF))
                    .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF0284C7) else Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "LINUX DB STACK",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Alpine • MariaDB • Redis • Mongo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Host and Service Status Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "IP: 127.0.0.1",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (uiState.isServiceRunning) Color(0x2622C55E) else Color(0x2664748B))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isServiceRunning) Color(0xFF22C55E) else Color(0xFF94A3B8))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isServiceRunning) "ONLINE" else "STOPPED",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isServiceRunning) Color(0xFF22C55E) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Master Start / Stop Button in Drawer
                    Button(
                        onClick = {
                            onToggleService()
                            onCloseDrawer()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isServiceRunning) Color(0xFFDC2626) else Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("drawer_toggle_service_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isServiceRunning) "STOP ENGINES" else "START ENGINES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NAVIGATION & MODULES",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Navigation Drawer Items
            val navItems = listOf(
                DrawerNavItem(AppScreen.PLAYGROUND, "Code Playground", Icons.Default.Code, "Node.js, Python, SQL & script runner"),
                DrawerNavItem(AppScreen.PREVIEW, "Live Web Preview", Icons.Default.OpenInBrowser, "Embedded browser for localhost servers"),
                DrawerNavItem(AppScreen.STUDIO, "Database Studio", Icons.Default.Storage, "SQL, Redis & Mongo query workbench"),
                DrawerNavItem(AppScreen.TERMINAL, "Terminal Console", Icons.Default.Terminal, "Interactive bash & real-time logs"),
                DrawerNavItem(AppScreen.FILES, "Workspace & Configs", Icons.Default.Folder, "Acode file explorer & live editor"),
                DrawerNavItem(AppScreen.PROCESSES, "Process Monitor", Icons.Default.Memory, "POSIX PIDs, task manager & kill controls"),
                DrawerNavItem(AppScreen.PACKAGES, "Package Hub & Node", Icons.Default.Inventory2, "Install npm, pip, git & packages"),
                DrawerNavItem(AppScreen.SYSTEM, "Alpine Linux Rootfs", Icons.Default.FolderZip, "Standalone Alpine Linux aarch64 environment"),
                DrawerNavItem(AppScreen.DAEMONS, "Database Daemons", Icons.Default.Storage, "MariaDB, Redis & Mongo matrix"),
                DrawerNavItem(AppScreen.TUNING, "Engine Tuning", Icons.Default.Tune, "InnoDB, WiredTiger & buffer sliders"),
                DrawerNavItem(AppScreen.DIAGNOSTICS, "Socket Diagnostics", Icons.Default.Sensors, "Deep loopback socket tester")
            )

            navItems.forEach { item ->
                val isSelected = uiState.currentScreen == item.screen
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) {
                                if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            } else {
                                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) {
                                    if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                } else {
                                    if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                }
                            )
                            Text(
                                text = item.subtitle,
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        }
                    },
                    selected = isSelected,
                    onClick = {
                        onSelectScreen(item.screen)
                        onCloseDrawer()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                        unselectedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_nav_${item.screen.route}")
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dark / Light Mode Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleDarkMode)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                        contentDescription = "Theme Mode",
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isDark) "Dark Theme" else "Light Theme",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isDark) "Obsidian console mode" else "DevOps clean mode",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    }
                }

                Switch(
                    checked = isDark,
                    onCheckedChange = { onToggleDarkMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0F172A),
                        uncheckedThumbColor = Color(0xFF0284C7),
                        uncheckedTrackColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }
    }
}

private data class DrawerNavItem(
    val screen: AppScreen,
    val title: String,
    val icon: ImageVector,
    val subtitle: String
)
