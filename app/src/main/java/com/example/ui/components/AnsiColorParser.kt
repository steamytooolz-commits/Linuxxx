package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * ANSI Color & Style parser for Termux / Linux CLI console streams.
 * Parses standard 16-color ANSI escape sequences (\u001B[...m) into Compose AnnotatedStrings
 * with guaranteed high-contrast color mapping for dark terminal screens.
 */
object AnsiColorParser {

    private val ANSI_REGEX = Regex("\u001B\\[([0-9;]*)m")

    fun parseAnsiToAnnotatedString(text: String, defaultColor: Color): AnnotatedString {
        if (!text.contains("\u001B[")) {
            return buildAnnotatedString {
                pushStyle(SpanStyle(color = defaultColor))
                append(text)
                pop()
            }
        }

        return buildAnnotatedString {
            var currentIndex = 0
            var currentColor = defaultColor
            var isBold = false

            val matches = ANSI_REGEX.findAll(text)
            for (match in matches) {
                val matchStart = match.range.first
                val matchEnd = match.range.last + 1

                if (matchStart > currentIndex) {
                    val rawSubstring = text.substring(currentIndex, matchStart)
                    val style = SpanStyle(
                        color = currentColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                    )
                    pushStyle(style)
                    append(rawSubstring)
                    pop()
                }

                // Process ANSI codes
                val codesStr = match.groupValues[1]
                val codes = if (codesStr.isEmpty()) listOf(0) else codesStr.split(";").mapNotNull { it.toIntOrNull() }

                for (code in codes) {
                    when (code) {
                        0 -> {
                            // Reset to crisp default
                            currentColor = defaultColor
                            isBold = false
                        }
                        1 -> isBold = true
                        22 -> isBold = false
                        // Standard Foreground Colors (30-37) - Adjusted for high dark-terminal contrast
                        30 -> currentColor = Color(0xFF94A3B8) // Black/Dark Gray -> Bright Slate
                        31 -> currentColor = Color(0xFFF87171) // Red -> Bright Red
                        32 -> currentColor = Color(0xFF4ADE80) // Green -> Bright Green
                        33 -> currentColor = Color(0xFFFACC15) // Yellow -> Bright Yellow
                        34 -> currentColor = Color(0xFF60A5FA) // Blue -> Bright Sky Blue
                        35 -> currentColor = Color(0xFFC084FC) // Magenta -> Bright Purple
                        36 -> currentColor = Color(0xFF38BDF8) // Cyan -> Bright Cyan
                        37 -> currentColor = Color(0xFFF8FAFC) // White -> High Contrast White
                        39 -> currentColor = defaultColor      // Default FG
                        // High Intensity Foreground Colors (90-97)
                        90 -> currentColor = Color(0xFFCBD5E1) // Bright Black (Gray)
                        91 -> currentColor = Color(0xFFFCA5A5) // Bright Red
                        92 -> currentColor = Color(0xFF86EFAC) // Bright Green
                        93 -> currentColor = Color(0xFFFDE047) // Bright Yellow
                        94 -> currentColor = Color(0xFF93C5FD) // Bright Blue
                        95 -> currentColor = Color(0xFFE9D5FF) // Bright Magenta
                        96 -> currentColor = Color(0xFF7DD3FC) // Bright Cyan
                        97 -> currentColor = Color(0xFFFFFFFF) // Bright White
                    }
                }

                currentIndex = matchEnd
            }

            if (currentIndex < text.length) {
                val remaining = text.substring(currentIndex)
                val style = SpanStyle(
                    color = currentColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                )
                pushStyle(style)
                append(remaining)
                pop()
            }
        }
    }
}

