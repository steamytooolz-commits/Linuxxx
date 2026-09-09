package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),        // Sky blue
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF0284C7),
    onPrimaryContainer = Color(0xFFF0F9FF),
    secondary = Color(0xFF10B981),      // Emerald green
    onSecondary = Color(0xFF030712),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFECFDF5),
    tertiary = Color(0xFFA78BFA),       // Cyber violet
    onTertiary = Color(0xFF030712),
    background = Color(0xFF090D16),     // Deep obsidian
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F172A),        // Slate 900
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B), // Slate 800
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

private val TerminalLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),        // Ocean blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF059669),      // Deep Emerald
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFF7C3AED),       // Royal violet
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),     // Clean Slate 50
    onBackground = Color(0xFF0F172A),   // Slate 900
    surface = Color(0xFFFFFFFF),        // Pure White card
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFCBD5E1),        // Slate 300
    outlineVariant = Color(0xFFE2E8F0)  // Slate 200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) TerminalDarkColorScheme else TerminalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

