package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.BlindAIViewModel
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepAlertRed
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LavenderHover
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalBorder
import com.example.ui.theme.NaturalMuted
import com.example.ui.theme.NaturalSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun GeminiLiveScreen(
    viewModel: BlindAIViewModel,
    modifier: Modifier = Modifier
) {
    val aiResponse by viewModel.aiResponseText.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val isListening by viewModel.isListeningForVoice.collectAsState()
    val isSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val isLiveMicEnabled by viewModel.isLiveMicEnabled.collectAsState()
    val liveTranscript by viewModel.liveVoiceTranscript.collectAsState()
    val audioRmsDb by viewModel.audioRmsDb.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    // Smooth animated speech scale based on audio volume
    val normalizedRms = ((audioRmsDb + 2f) / 10f).coerceIn(0f, 1.5f)
    val animatedVoiceScale by animateFloatAsState(
        targetValue = if (isListening) 1f + (normalizedRms * 0.45f) else 1f,
        animationSpec = tween(120),
        label = "voice_rms_scale"
    )

    // Infinite breathing rotation animations for the Live Orb
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_live_effects")
    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // LAYER 1: Full-Screen Dark Vision Scrim over live camera
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD0F172A),
                            Color(0xBB0F172A),
                            Color(0xF50F172A)
                        )
                    )
                )
        )

        // LAYER 2: Main Interactive Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Vision Active Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NaturalSurfaceElevated.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldLive)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE CAMERA ACTIVE",
                            color = EmeraldLive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // AI Model Indicator Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NaturalSurfaceElevated.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "NETRA AI",
                            tint = LavenderPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NETRA AI",
                            color = LavenderPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // CENTER: Interactive Gemini Live Multimodal Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .clickable {
                            viewModel.toggleLiveMic()
                        }
                        .testTag("gemini_live_orb")
                ) {
                    // Outer reactive sound wave rings
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(animatedVoiceScale * orbPulse)
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f

                        // Outer halo ring
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isSpeaking) LavenderPrimary.copy(alpha = 0.4f)
                                    else if (isListening) EmeraldLive.copy(alpha = 0.35f)
                                    else if (isThinking) AmberWarm.copy(alpha = 0.35f)
                                    else Color(0x3364748B),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )

                        // Orbiting accent ring
                        drawCircle(
                            color = if (isSpeaking) LavenderPrimary.copy(alpha = waveAlpha)
                            else if (isListening) EmeraldLive.copy(alpha = waveAlpha)
                            else if (isThinking) AmberWarm.copy(alpha = waveAlpha)
                            else NaturalBorder,
                            radius = radius * 0.78f,
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // Inner Glowing Core Orb
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isSpeaking) {
                                        listOf(LavenderHover, LavenderPrimary, Color(0xFF4C1D95))
                                    } else if (isListening) {
                                        listOf(Color(0xFF6EE7B7), EmeraldLive, Color(0xFF064E3B))
                                    } else if (isThinking) {
                                        listOf(Color(0xFFFDE047), AmberWarm, Color(0xFF78350F))
                                    } else {
                                        listOf(NaturalMuted, Color(0xFF334155), Color(0xFF0F172A))
                                    }
                                )
                            )
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        LavenderPrimary,
                                        EmeraldLive,
                                        AmberWarm,
                                        LavenderPrimary
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (!isLiveMicEnabled) Icons.Default.MicOff
                                else if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp
                                else if (isThinking) Icons.Default.AutoAwesome
                                else if (isListening) Icons.Default.GraphicEq
                                else Icons.Default.Hearing,
                                contentDescription = "Gemini Live Status",
                                tint = DeepVioletOnPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time State Label
                val stateText = when {
                    !isLiveMicEnabled -> "Mic paused • Tap to resume listening"
                    isThinking -> "NETRA AI is analyzing camera scene..."
                    isSpeaking -> "NETRA AI is speaking..."
                    isListening && liveTranscript.isNotBlank() -> "Listening: \"$liveTranscript\""
                    isListening -> "Listening to you... Ask anything you see"
                    else -> "Live mic active • Speak anytime"
                }

                val stateColor = when {
                    !isLiveMicEnabled -> TextTertiary
                    isThinking -> AmberWarm
                    isSpeaking -> LavenderPrimary
                    isListening -> EmeraldLive
                    else -> TextSecondary
                }

                Text(
                    text = stateText,
                    color = stateColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // BOTTOM SECTION: Conversational Live Response Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NaturalSurfaceElevated.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_live_conversation_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Spoken transcript header
                    if (liveTranscript.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "User speech",
                                tint = EmeraldLive,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You asked:",
                                color = EmeraldLive,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "\"$liveTranscript\"",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Gemini Answer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Gemini Live",
                            tint = LavenderPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NETRA AI Response:",
                            color = LavenderPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (telemetry.aiRequestLatencyMs > 0) {
                            Text(
                                text = "${telemetry.aiRequestLatencyMs}ms",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isThinking) "Seeing through camera and preparing response..." else aiResponse,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BOTTOM CONTROL: Hands-Free Live Mic Bar
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isLiveMicEnabled) NaturalSurfaceElevated else CoralAlert.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = if (isLiveMicEnabled) EmeraldLive else CoralAlert
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clickable {
                        viewModel.toggleLiveMic()
                    }
                    .testTag("btn_toggle_live_mic")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isLiveMicEnabled) EmeraldLive else CoralAlert)
                    ) {
                        Icon(
                            imageVector = if (isLiveMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Live Mic Toggle",
                            tint = DeepVioletOnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isLiveMicEnabled) "ALWAYS-ON LIVE MIC ACTIVE" else "LIVE MIC IS PAUSED",
                            color = if (isLiveMicEnabled) EmeraldLive else CoralAlert,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isLiveMicEnabled) "Hands-free voice active • Speak naturally" else "Tap to resume continuous voice listening",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
