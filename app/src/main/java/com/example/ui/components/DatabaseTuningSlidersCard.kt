package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Interactive Hardware & Database Tuning Sliders component:
 * 1. MongoDB WiredTiger Cache Size (64MB - 1024MB)
 * 2. MariaDB InnoDB Buffer Pool (32MB - 512MB)
 * 3. Loopback Socket Probe Polling Interval (1s - 10s)
 * 4. Terminal Console Max Buffer Lines (100 - 1000 lines)
 */
@Composable
fun DatabaseTuningSlidersCard(
    wiredTigerMb: Int,
    onWiredTigerChange: (Int) -> Unit,
    innodbMb: Int,
    onInnodbChange: (Int) -> Unit,
    probeIntervalSec: Int,
    onProbeIntervalChange: (Int) -> Unit,
    maxLogBuffer: Int,
    onMaxLogBufferChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("database_tuning_sliders_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tuning Sliders",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "RESOURCE & ENGINE TUNING",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = Color(0xFFF1F5F9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Mongo ${wiredTigerMb}MB • MariaDB ${innodbMb}MB • Socket ${probeIntervalSec}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF94A3B8)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    // Slider 1: MongoDB WiredTiger Cache
                    SliderItem(
                        label = "MongoDB WiredTiger Cache Size",
                        valueFormatted = "${wiredTigerMb} MB",
                        subtext = "Allocated internal WiredTiger engine cache",
                        value = wiredTigerMb.toFloat(),
                        valueRange = 64f..1024f,
                        steps = 14,
                        sliderTag = "slider_wiredtiger_cache",
                        accentColor = Color(0xFF4ADE80),
                        onValueChange = { onWiredTigerChange(it.roundToInt()) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slider 2: MariaDB InnoDB Buffer Pool
                    SliderItem(
                        label = "MariaDB InnoDB Buffer Pool Size",
                        valueFormatted = "${innodbMb} MB",
                        subtext = "Dedicated memory pool for caching tables & indexes",
                        value = innodbMb.toFloat(),
                        valueRange = 32f..512f,
                        steps = 14,
                        sliderTag = "slider_innodb_buffer",
                        accentColor = Color(0xFF38BDF8),
                        onValueChange = { onInnodbChange(it.roundToInt()) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slider 3: Socket Probe Polling Interval
                    SliderItem(
                        label = "TCP Socket Probe Polling Interval",
                        valueFormatted = "${probeIntervalSec}s",
                        subtext = "Frequency of automated loopback health checks",
                        value = probeIntervalSec.toFloat(),
                        valueRange = 3f..30f,
                        steps = 8,
                        sliderTag = "slider_probe_interval",
                        accentColor = Color(0xFFF59E0B),
                        onValueChange = { onProbeIntervalChange(it.roundToInt()) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slider 4: Terminal Log Buffer Depth
                    SliderItem(
                        label = "Terminal Console Buffer Limit",
                        valueFormatted = "${maxLogBuffer} lines",
                        subtext = "Maximum retained active log lines in UI scroll memory",
                        value = maxLogBuffer.toFloat(),
                        valueRange = 100f..1000f,
                        steps = 8,
                        sliderTag = "slider_log_buffer",
                        accentColor = Color(0xFFA78BFA),
                        onValueChange = { onMaxLogBufferChange(it.roundToInt()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderItem(
    label: String,
    valueFormatted: String,
    subtext: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    sliderTag: String,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF1F5F9),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = valueFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(sliderTag)
        )
    }
}
