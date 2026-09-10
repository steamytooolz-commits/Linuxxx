package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen

/**
 * Modern M3 Navigation Bar for fast mobile switching between primary screens.
 */
@Composable
fun AppBottomBar(
    currentScreen: AppScreen,
    isDarkMode: Boolean,
    onSelectScreen: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(AppScreen.CODEMIRROR, "Editor", Icons.Filled.Edit),
        BottomNavItem(AppScreen.DAEMONS, "Daemons", Icons.Filled.Settings),
        BottomNavItem(AppScreen.TERMINAL, "Terminal", Icons.Filled.Terminal),
        BottomNavItem(AppScreen.SETUP, "Setup", Icons.Filled.Refresh)
    )

    NavigationBar(
        modifier = modifier.testTag("app_bottom_bar"),
        containerColor = if (isDarkMode) Color(0xFF090D16) else Color(0xFFFFFFFF),
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selected = isSelected,
                onClick = { onSelectScreen(item.screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDarkMode) Color.White else Color.White,
                    selectedTextColor = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    indicatorColor = if (isDarkMode) Color(0xFF0284C7) else Color(0xFF0284C7),
                    unselectedIconColor = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                    unselectedTextColor = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                ),
                modifier = Modifier.testTag("bottom_nav_${item.screen.route}")
            )
        }
    }
}

private data class BottomNavItem(
    val screen: AppScreen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
