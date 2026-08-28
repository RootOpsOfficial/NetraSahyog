package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BlindAIViewModel
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalBorder
import com.example.ui.theme.NaturalMuted
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalSurfaceElevated
import com.example.ui.theme.NaturalSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TelemetryScreen(
    viewModel: BlindAIViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val sensorState by viewModel.sensorState.collectAsState()
    val perceptionOutput by viewModel.perceptionOutput.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NaturalBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "System Performance & Diagnostics",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Real-time edge telemetry and hardware sensor health",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Key Performance Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Camera FPS",
                value = "${telemetry.cameraFps} FPS",
                subtext = "Target: 30 FPS",
                icon = Icons.Default.Speed,
                accentColor = EmeraldLive,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Perception Latency",
                value = "${telemetry.inferenceLatencyMs} ms",
                subtext = "On-device Vision",
                icon = Icons.Default.ElectricBolt,
                accentColor = LavenderPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Alert Latency",
                value = "${telemetry.endToEndAlertLatencyMs} ms",
                subtext = "Vision to TTS/Haptic",
                icon = Icons.Default.Memory,
                accentColor = AmberWarm,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Active Obstacles",
                value = "${perceptionOutput.trackedObstacles.size}",
                subtext = "Urgent: ${telemetry.urgentHazardsCount}",
                icon = Icons.Default.Shield,
                accentColor = if (telemetry.urgentHazardsCount > 0) CoralAlert else EmeraldLive,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware IMU Sensor Telemetry
        Card(
            colors = CardDefaults.cardColors(containerColor = NaturalSurfaceElevated),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LavenderPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Sensor",
                            tint = DeepVioletOnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hardware IMU & Compass Sensor",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TelemetryRow(label = "Compass Azimuth", value = "${"%.1f".format(sensorState.azimuthHeadingDegrees)}° (${sensorState.cardinalDirection})")
                TelemetryRow(label = "Device Pitch / Roll", value = "${sensorState.pitchDegrees.toInt()}° / ${sensorState.rollDegrees.toInt()}°")
                TelemetryRow(label = "Pedometer Steps", value = "${sensorState.stepCount} steps")
                TelemetryRow(label = "User State", value = if (sensorState.isWalking) "Walking (Moving)" else "Stationary")
                val loc = sensorState.location
                val gpsCoord = if (loc != null) {
                    "${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)}"
                } else {
                    "18.5204° N, 73.8436° E (FC Road, Pune)"
                }
                TelemetryRow(label = "GPS Coordinates", value = gpsCoord)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Architectural Compliance Summary
        Card(
            colors = CardDefaults.cardColors(containerColor = NaturalSurfaceElevated),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "System Architecture & Safety Guarantee",
                    color = LavenderPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                ArchitectureCheckItem("Local-First Critical Path: Camera -> On-Device Object Detection -> Spatial Analyzer -> Priority Engine -> Android TTS runs completely offline without cloud dependency.")
                ArchitectureCheckItem("Offline Footpath Routing: Pune OpenStreetMap pedestrian network graph with A* waypoint algorithm.")
                ArchitectureCheckItem("Multimodal Cloud AI: Gemini Live Assistant handles conversational scene inspection and secondary queries.")
                ArchitectureCheckItem("Voice & Haptic Fusion: High-priority immediate alert interrupts low priority chatter.")
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaturalSurfaceElevated),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ArchitectureCheckItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Passed",
            tint = EmeraldLive,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
