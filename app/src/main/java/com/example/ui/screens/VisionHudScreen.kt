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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.NavigationProviderType
import com.example.model.NavigationStatus
import com.example.model.ObstaclePriority
import com.example.model.ObstacleType
import com.example.navigation.PuneOsmDataset
import com.example.ui.BlindAIViewModel
import com.example.ui.components.WalkableCorridorOverlay
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.AmberText
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepAlertRed
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.LavenderHover
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
fun VisionHudScreen(
    viewModel: BlindAIViewModel,
    modifier: Modifier = Modifier
) {
    val perceptionOutput by viewModel.perceptionOutput.collectAsState()
    val isListening by viewModel.isListeningForVoice.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val aiResponseText by viewModel.aiResponseText.collectAsState()
    val navState by viewModel.navigationState.collectAsState()
    val sensorState by viewModel.sensorState.collectAsState()

    val pathAnalysis = perceptionOutput.pathAnalysis
    val obstacles = perceptionOutput.trackedObstacles

    var selectedPoiIndex by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Natural Tones Frame Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, NaturalBorder, RoundedCornerShape(28.dp))
        ) {
            // 2. Walkable Corridor & Obstacle Bounding Box Overlay
            WalkableCorridorOverlay(
                obstacles = obstacles,
                pathAnalysis = pathAnalysis
            )

            // 3. FLOATING MINI OSM FOOTPATH WINDOW (Top-Right Camera Corner)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NaturalSurfaceElevated.copy(alpha = 0.93f),
                border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 12.dp)
                    .width(170.dp)
                    .clickable {
                        // Quick cycle destination or start routing
                        selectedPoiIndex = (selectedPoiIndex + 1) % PuneOsmDataset.PUNE_POIS.size
                        val nextPoi = PuneOsmDataset.PUNE_POIS[selectedPoiIndex]
                        viewModel.navigationManager.startNavigation(nextPoi, language = language)
                    }
                    .testTag("floating_osm_mini_card")
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(LavenderPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = "Footpath",
                                    tint = DeepVioletOnPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (navState.providerType == NavigationProviderType.GOOGLE_MAPS_LIVE) "Live GMaps" else "Footpath",
                                color = LavenderPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Compass Direction Needle
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Heading",
                            tint = EmeraldLive,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(sensorState.azimuthHeadingDegrees)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val activePoi = navState.currentDestination ?: PuneOsmDataset.PUNE_POIS[selectedPoiIndex]
                    val poiName = when (language) {
                        AppLanguage.HINDI -> if (activePoi.nameHi.isNotBlank()) activePoi.nameHi else activePoi.name
                        AppLanguage.MARATHI -> if (activePoi.nameMr.isNotBlank()) activePoi.nameMr else activePoi.name
                        AppLanguage.ENGLISH -> activePoi.name
                    }

                    Text(
                        text = poiName,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    val remainingDist = if (navState.status == NavigationStatus.NAVIGATING) {
                        "${navState.remainingDistanceMeters}m"
                    } else {
                        "${activePoi.distanceMeters}m"
                    }

                    Text(
                        text = "$remainingDist • Walking Route",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 1
                    )

                    if (navState.status == NavigationStatus.NAVIGATING && navState.currentStep != null) {
                        val step = navState.currentStep
                        val stepGuide = when {
                            step?.instructionText?.isNotBlank() == true -> step.instructionText
                            language == AppLanguage.HINDI -> step?.instruction?.spokenHi
                            language == AppLanguage.MARATHI -> step?.instruction?.spokenMr
                            else -> step?.instruction?.spokenEn
                        } ?: "Follow footpath"

                        Text(
                            text = stepGuide,
                            color = LavenderPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 4. Top Path Status Banner & Obstacle Badges
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 12.dp, start = 14.dp, end = 14.dp)
        ) {
            val targetState by viewModel.targetGuidanceState.collectAsState()
            val movementGuidance by viewModel.movementGuidance.collectAsState()

            // Active Target Guidance Banner (e.g. Door, Chair, Person, Table)
            if (targetState.isActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldLive.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldLive),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .testTag("target_guidance_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EmeraldLive)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Target Guidance",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GUIDING TO: ${targetState.targetQuery.uppercase()}",
                                color = EmeraldLive,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            val guideText = when (language) {
                                AppLanguage.HINDI -> "दूरी: ~${targetState.remainingSteps} कदम (${targetState.zone.spokenDescriptionHi})"
                                AppLanguage.MARATHI -> "अंतर: ~${targetState.remainingSteps} पावले (${targetState.zone.spokenDescriptionMr})"
                                AppLanguage.ENGLISH -> "Target: ${"%.1f".format(targetState.approximateMeters)}m (~${targetState.remainingSteps} steps) ${targetState.zone.spokenDescriptionEn}"
                            }
                            Text(
                                text = guideText,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = { viewModel.stopTargetGuidance() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Target",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            val statusText = when (language) {
                AppLanguage.HINDI -> pathAnalysis.suggestedActionHi
                AppLanguage.MARATHI -> pathAnalysis.suggestedActionMr
                AppLanguage.ENGLISH -> pathAnalysis.suggestedActionEn
            }

            val isUrgent = pathAnalysis.statusLevel == ObstaclePriority.URGENT
            val isWarning = pathAnalysis.statusLevel == ObstaclePriority.WARNING

            // Natural Tones Status Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isUrgent -> CoralAlert
                        isWarning -> AmberWarm
                        else -> NaturalSurfaceHighlight.copy(alpha = 0.92f)
                    }
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isUrgent || isWarning) Color.Transparent else NaturalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_banner_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isUrgent -> DeepAlertRed
                                    isWarning -> AmberText
                                    else -> LavenderPrimary
                                }
                            )
                    ) {
                        Icon(
                            imageVector = if (isUrgent || isWarning) Icons.Default.Warning else Icons.Default.SpatialAudio,
                            contentDescription = "Status Icon",
                            tint = when {
                                isUrgent || isWarning -> Color.White
                                else -> DeepVioletOnPrimary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrgent) "CRITICAL ALERT" else if (isWarning) "ATTENTION" else "PATH STATUS",
                            color = when {
                                isUrgent -> DarkCoralText
                                isWarning -> AmberText
                                else -> LavenderPrimary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = statusText,
                            color = when {
                                isUrgent -> DeepAlertRed
                                isWarning -> AmberText
                                else -> TextPrimary
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Detected Obstacle Chips with Person Labeling
            if (obstacles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(obstacles.filter { it.priority != ObstaclePriority.IGNORE }) { obs ->
                        val isObsUrgent = obs.priority == ObstaclePriority.URGENT
                        val personBadgeText = if (obs.type == ObstacleType.PERSON) {
                            "PERSON (${obs.zone.label})"
                        } else {
                            "${obs.type.displayName.uppercase()} (${obs.zone.label})"
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isObsUrgent) CoralAlert else LavenderPrimary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder)
                        ) {
                            Text(
                                text = "$personBadgeText • ${"%.1f".format(obs.approximateMeters)}m",
                                color = if (isObsUrgent) DarkCoralText else DeepVioletOnPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Bottom Voice-First Controls & Status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live AI Speech Bubble
            if (aiResponseText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NaturalSurfaceHighlight.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("ai_response_bubble")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(LavenderPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SpatialAudio,
                                contentDescription = "AI Voice",
                                tint = DeepVioletOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = aiResponseText,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Primary Voice-First Action Bar (Large, Accessible Tap-to-Speak Button + Stop TTS Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isListening) viewModel.stopVoiceListening() else viewModel.startVoiceListening()
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) CoralAlert else LavenderPrimary,
                        contentColor = if (isListening) DeepAlertRed else DeepVioletOnPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("main_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isListening) "Listening... (Tap to Finish)" else "Tap to Speak / Ask Anything",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NaturalMuted,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
                    modifier = Modifier
                        .size(58.dp)
                        .clickable { viewModel.voiceAlertManager.stop() }
                        .testTag("btn_stop_audio")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(LavenderPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Micro Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FPS: ${telemetry.cameraFps}",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "LATENCY: ${telemetry.inferenceLatencyMs}ms",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "OFFLINE VISION ACTIVE",
                    color = LavenderPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable {
                        viewModel.setTab(com.example.ui.AppTab.GEMINI_LIVE)
                    }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NaturalSurfaceHighlight.copy(alpha = 0.90f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = LavenderPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

