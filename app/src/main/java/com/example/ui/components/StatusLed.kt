package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PortInfo

/**
 * Canvas-drawn status LED representing a hardware server rack indicator.
 * Displays glowing green when the loopback socket accepts connections, or red when closed.
 */
@Composable
fun StatusLed(
    portInfo: PortInfo,
    modifier: Modifier = Modifier
) {
    val activeGreen = Color(0xFF22C55E)
    val inactiveRed = Color(0xFFEF4444)
    val glowColor = if (portInfo.isOpen) activeGreen else inactiveRed
    val coreColor = if (portInfo.isOpen) Color(0xFF86EFAC) else Color(0xFFFCA5A5)

    Box(
        modifier = modifier
            .testTag("status_led_card_${portInfo.port}")
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, if (portInfo.isOpen) Color(0x4022C55E) else Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // LED canvas indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(
                        modifier = Modifier
                            .size(12.dp)
                            .testTag("led_canvas_${portInfo.port}")
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2.2f

                        // Ambient glow ring
                        drawCircle(
                            color = glowColor.copy(alpha = 0.35f),
                            radius = radius * 1.5f,
                            center = center
                        )

                        // Core LED bulb
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, coreColor, glowColor),
                                center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = portInfo.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF1F5F9),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Port tag
                Text(
                    text = ":${portInfo.port}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }

            // Status label & latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (portInfo.isOpen) "ONLINE" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (portInfo.isOpen) Color(0xFF4ADE80) else Color(0xFF64748B),
                    fontSize = 10.sp
                )

                Text(
                    text = if (portInfo.isOpen && portInfo.latencyMs >= 0) "${portInfo.latencyMs}ms" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}

