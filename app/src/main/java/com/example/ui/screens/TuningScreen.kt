package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import kotlin.math.roundToInt

@Composable
fun TuningScreen(
    uiState: MainUiState,
    onWiredTigerChange: (Int) -> Unit,
    onInnodbChange: (Int) -> Unit,
    onProbeIntervalChange: (Int) -> Unit,
    onMaxLogBufferChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiState.isDarkMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tuning Overview Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF0284C7) else Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DATABASE HARDWARE & ENGINE TUNING",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Optimize memory allocations and daemon process limits for Android",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                Text(
                    text = "MEMORY PROFILE PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Eco Preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .clickable {
                                onWiredTigerChange(128)
                                onInnodbChange(64)
                                onProbeIntervalChange(6)
                                onMaxLogBufferChange(200)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ECO (Low RAM)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isDark) Color(0xFF4ADE80) else Color(0xFF059669))
                            Text("192MB Total", fontSize = 9.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        }
                    }

                    // Balanced Preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF0284C7) else Color(0xFFE0F2FE))
                            .clickable {
                                onWiredTigerChange(256)
                                onInnodbChange(128)
                                onProbeIntervalChange(4)
                                onMaxLogBufferChange(400)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BALANCED", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isDark) Color.White else Color(0xFF0284C7))
                            Text("384MB Total", fontSize = 9.sp, color = if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1))
                        }
                    }

                    // Performance Preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .clickable {
                                onWiredTigerChange(512)
                                onInnodbChange(256)
                                onProbeIntervalChange(2)
                                onMaxLogBufferChange(800)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PERFORMANCE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED))
                            Text("768MB Total", fontSize = 9.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        }
                    }
                }
            }
        }

        // Slider 1: MongoDB WiredTiger Cache
        TuningSliderCard(
            title = "MongoDB WiredTiger Cache Size",
            currentValueText = "${uiState.wiredTigerCacheSizeMb} MB",
            value = uiState.wiredTigerCacheSizeMb.toFloat(),
            valueRange = 64f..1024f,
            steps = 14,
            description = "RAM allocated to MongoDB WiredTiger storage engine before flushing to SQLite / local storage.",
            isDarkMode = isDark,
            accentColor = Color(0xFF38BDF8),
            onValueChange = { onWiredTigerChange(it.roundToInt()) }
        )

        // Slider 2: MariaDB InnoDB Buffer Pool
        TuningSliderCard(
            title = "MariaDB InnoDB Buffer Pool Size",
            currentValueText = "${uiState.innodbBufferPoolMb} MB",
            value = uiState.innodbBufferPoolMb.toFloat(),
            valueRange = 32f..512f,
            steps = 14,
            description = "Main memory caching layer for MySQL tables, indices, and active row data.",
            isDarkMode = isDark,
            accentColor = Color(0xFF4ADE80),
            onValueChange = { onInnodbChange(it.roundToInt()) }
        )

        // Slider 3: Socket Probe Frequency
        TuningSliderCard(
            title = "Socket Probe Polling Frequency",
            currentValueText = "${uiState.probeIntervalSec} seconds",
            value = uiState.probeIntervalSec.toFloat(),
            valueRange = 1f..10f,
            steps = 8,
            description = "Background interval for probing TCP ports 3306, 6379, and 27017 without exhausting OS sockets.",
            isDarkMode = isDark,
            accentColor = Color(0xFFFBBF24),
            onValueChange = { onProbeIntervalChange(it.roundToInt()) }
        )

        // Slider 4: Terminal Log Buffer
        TuningSliderCard(
            title = "Terminal Console Buffer Limit",
            currentValueText = "${uiState.maxLogBufferSize} lines",
            value = uiState.maxLogBufferSize.toFloat(),
            valueRange = 100f..1000f,
            steps = 8,
            description = "Maximum number of stdout lines retained in live interactive memory.",
            isDarkMode = isDark,
            accentColor = Color(0xFFA78BFA),
            onValueChange = { onMaxLogBufferChange(it.roundToInt()) }
        )
    }
}

@Composable
private fun TuningSliderCard(
    title: String,
    currentValueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    description: String,
    isDarkMode: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentValueText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 10.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
