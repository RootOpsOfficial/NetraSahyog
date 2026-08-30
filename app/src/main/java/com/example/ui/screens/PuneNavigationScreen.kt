package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.NavigationProviderType
import com.example.model.NavigationStatus
import com.example.model.PoiCategory
import com.example.model.PoiItem
import com.example.model.TurnDirection
import com.example.ui.BlindAIViewModel
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepAlertRed
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PuneNavigationScreen(
    viewModel: BlindAIViewModel,
    modifier: Modifier = Modifier
) {
    val navState by viewModel.navigationState.collectAsState()
    val sensorState by viewModel.sensorState.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PoiCategory?>(null) }
    var poisList by remember { mutableStateOf<List<PoiItem>>(emptyList()) }

    LaunchedEffect(searchQuery, selectedCategory) {
        val userLoc = viewModel.locationServiceManager.currentLocation.value
        val lat = userLoc?.latitude ?: 18.52043
        val lon = userLoc?.longitude ?: 73.84365

        if (searchQuery.trim().length >= 2) {
            val searched = viewModel.navigationManager.searchDestinations(searchQuery.trim(), lat, lon)
            poisList = if (selectedCategory == null) {
                searched
            } else {
                searched.filter { it.category == selectedCategory }
            }
        } else {
            val all = viewModel.navigationManager.getAllAvailablePois()
            poisList = all.filter { poi ->
                val matchesCategory = selectedCategory == null || poi.category == selectedCategory
                matchesCategory
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NaturalBackground)
            .padding(16.dp)
    ) {
        // Location & Sensor Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NaturalSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LavenderPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Heading",
                        tint = DeepVioletOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (navState.providerType == NavigationProviderType.GOOGLE_MAPS_LIVE) "Google Maps Live Walk Navigation" else "Anchor: FC Road, Pune (Pedestrian)",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Facing ${sensorState.cardinalDirection} (${sensorState.azimuthHeadingDegrees.toInt()}°)  •  ${sensorState.stepCount} Steps  •  ${if (navState.providerType == NavigationProviderType.GOOGLE_MAPS_LIVE) "Online GMaps API" else "OSM Offline"}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Navigation or POI Selector
        if (navState.status == NavigationStatus.NAVIGATING ||
            navState.status == NavigationStatus.HAZARD_DETECTED ||
            navState.status == NavigationStatus.ARRIVED ||
            navState.status == NavigationStatus.OFF_ROUTE_RECALCULATING
        ) {
            ActiveNavigationCard(navState = navState, language = language, viewModel = viewModel)
        } else {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = when (language) {
                            AppLanguage.HINDI -> "स्थान या श्रेणी खोजें..."
                            AppLanguage.MARATHI -> "स्थान किंवा श्रेणी शोधा..."
                            AppLanguage.ENGLISH -> "Search Pune destinations..."
                        },
                        color = TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = LavenderPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LavenderPrimary,
                    unfocusedBorderColor = NaturalBorder,
                    focusedContainerColor = NaturalSurfaceElevated,
                    unfocusedContainerColor = NaturalSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("destination_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All (${poisList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LavenderPrimary,
                        selectedLabelColor = DeepVioletOnPrimary,
                        containerColor = NaturalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedCategory == PoiCategory.PHARMACY,
                    onClick = { selectedCategory = if (selectedCategory == PoiCategory.PHARMACY) null else PoiCategory.PHARMACY },
                    label = { Text("Pharmacy") },
                    leadingIcon = { Icon(Icons.Default.LocalPharmacy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LavenderPrimary,
                        selectedLabelColor = DeepVioletOnPrimary,
                        containerColor = NaturalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedCategory == PoiCategory.HOSPITAL,
                    onClick = { selectedCategory = if (selectedCategory == PoiCategory.HOSPITAL) null else PoiCategory.HOSPITAL },
                    label = { Text("Hospital") },
                    leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LavenderPrimary,
                        selectedLabelColor = DeepVioletOnPrimary,
                        containerColor = NaturalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedCategory == PoiCategory.BUS_STOP,
                    onClick = { selectedCategory = if (selectedCategory == PoiCategory.BUS_STOP) null else PoiCategory.BUS_STOP },
                    label = { Text("Transit") },
                    leadingIcon = { Icon(Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LavenderPrimary,
                        selectedLabelColor = DeepVioletOnPrimary,
                        containerColor = NaturalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedCategory == PoiCategory.COLLEGE,
                    onClick = { selectedCategory = if (selectedCategory == PoiCategory.COLLEGE) null else PoiCategory.COLLEGE },
                    label = { Text("College") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LavenderPrimary,
                        selectedLabelColor = DeepVioletOnPrimary,
                        containerColor = NaturalSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (language) {
                    AppLanguage.HINDI -> "गंतव्य चुनें (${poisList.size} उपलब्ध)"
                    AppLanguage.MARATHI -> "गंतव्य निवडा (${poisList.size} उपलब्ध)"
                    AppLanguage.ENGLISH -> "Destinations (${poisList.size} Available)"
                },
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(poisList) { poi ->
                    PoiSelectionCard(poi = poi, language = language, onSelect = {
                        val currentLoc = viewModel.locationServiceManager.currentLocation.value
                        val startLat = currentLoc?.latitude ?: 18.52043
                        val startLon = currentLoc?.longitude ?: 73.84365
                        viewModel.navigationManager.startNavigation(poi, startLat, startLon, language = language)
                    })
                }
            }
        }
    }
}

@Composable
fun ActiveNavigationCard(
    navState: com.example.model.NavigationState,
    language: AppLanguage,
    viewModel: BlindAIViewModel
) {
    val destination = navState.currentDestination
    val currentStep = navState.currentStep
    val sensorState by viewModel.sensorState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Camera Hazard Interruption Alert in Coral
        AnimatedVisibility(visible = navState.isCameraHazardBlocking) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CoralAlert),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("hazard_nav_override_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepAlertRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Obstacle alert",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = navState.currentHazardAlert ?: "Obstacle in walking path. Stop before proceeding.",
                        color = DarkCoralText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Off route alert
        AnimatedVisibility(visible = navState.isOffRoute) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AmberWarm.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarm),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = AmberWarm)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recalculating pedestrian route...",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Destination Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NaturalSurfaceElevated),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val destName = when (language) {
                        AppLanguage.HINDI -> destination?.nameHi ?: ""
                        AppLanguage.MARATHI -> destination?.nameMr ?: ""
                        AppLanguage.ENGLISH -> destination?.name ?: ""
                    }
                    Text(
                        text = destName,
                        color = LavenderPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Remaining: ${navState.remainingDistanceMeters}m  •  Step ${navState.currentStepIndex + 1}/${navState.segments.size}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.navigationManager.stopNavigation(language) },
                    modifier = Modifier.testTag("btn_stop_nav")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Navigation",
                        tint = CoralAlert
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pedestrian Schematic Map Canvas
        Card(
            colors = CardDefaults.cardColors(containerColor = NaturalSurfaceHighlight),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PedestrianRouteCanvas(
                    stepIndex = navState.currentStepIndex,
                    totalSteps = navState.segments.size,
                    headingDegrees = sensorState.azimuthHeadingDegrees,
                    hasHazard = navState.isCameraHazardBlocking
                )
                Text(
                    text = "Tactile Footpath Corridor Map",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Turn Instruction Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NaturalSurfaceHighlight),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("turn_instruction_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val isArrived = navState.status == NavigationStatus.ARRIVED
                val icon = when (currentStep?.instruction) {
                    TurnDirection.ARRIVED -> Icons.Default.CheckCircle
                    TurnDirection.CROSSING -> Icons.Default.Warning
                    TurnDirection.STAIRS -> Icons.Default.Warning
                    else -> Icons.AutoMirrored.Filled.DirectionsWalk
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (isArrived) EmeraldLive.copy(alpha = 0.2f) else LavenderPrimary.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (isArrived) EmeraldLive else LavenderPrimary,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Turn Direction",
                        tint = if (isArrived) EmeraldLive else LavenderPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val instructionText = when (language) {
                    AppLanguage.HINDI -> currentStep?.instruction?.spokenHi ?: ""
                    AppLanguage.MARATHI -> currentStep?.instruction?.spokenMr ?: ""
                    AppLanguage.ENGLISH -> currentStep?.instruction?.spokenEn ?: ""
                }

                Text(
                    text = instructionText,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentStep?.streetOrFootpathName ?: "",
                    color = LavenderPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (currentStep != null && currentStep.distanceMeters > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "For ${currentStep.distanceMeters} meters (~${(currentStep.distanceMeters / 0.75).toInt()} steps)",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.navigationManager.advanceToNextStep(language) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LavenderPrimary,
                    contentColor = DeepVioletOnPrimary
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("btn_advance_step")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Next Step",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    val step = navState.currentStep
                    if (step != null) {
                        viewModel.voiceAlertManager.speak(
                            "${step.instruction.spokenEn} on ${step.streetOrFootpathName} for ${step.distanceMeters} meters",
                            forceInterrupt = true
                        )
                    }
                },
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Repeat speech",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
fun PedestrianRouteCanvas(
    stepIndex: Int,
    totalSteps: Int,
    headingDegrees: Float,
    hasHazard: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val paddingX = 40f
        val startY = h * 0.75f
        val midX1 = w * 0.35f
        val midY1 = h * 0.75f
        val midX2 = w * 0.65f
        val midY2 = h * 0.35f
        val endX = w - paddingX
        val endY = h * 0.35f

        // Draw tactile sidewalk lane
        val path = Path().apply {
            moveTo(paddingX, startY)
            lineTo(midX1, midY1)
            lineTo(midX2, midY2)
            lineTo(endX, endY)
        }

        // Draw sidewalk outline
        drawPath(
            path = path,
            color = Color(0xFF333842),
            style = Stroke(width = 24f, cap = StrokeCap.Round)
        )

        // Draw dashed tactile yellow line
        drawPath(
            path = path,
            color = Color(0xFFFACC15).copy(alpha = 0.5f),
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )
        )

        // Draw zebra crossing pattern in middle section
        val crossingColor = Color(0xFF94A3B8).copy(alpha = 0.6f)
        for (i in 0..4) {
            val progress = i / 4f
            val cx = midX1 + (midX2 - midX1) * progress
            val cy = midY1 + (midY2 - midY1) * progress
            drawLine(
                color = crossingColor,
                start = Offset(cx - 8f, cy + 10f),
                end = Offset(cx + 8f, cy - 10f),
                strokeWidth = 3f
            )
        }

        // Active completed route progress
        val activeProgress = if (totalSteps > 0) ((stepIndex + 0.5f) / totalSteps).coerceIn(0.1f, 0.95f) else 0.5f
        val userX = paddingX + (endX - paddingX) * activeProgress
        val userY = startY + (endY - startY) * activeProgress

        // Draw destination pin at end
        drawCircle(
            color = Color(0xFF10B981),
            radius = 12f,
            center = Offset(endX, endY)
        )
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = Offset(endX, endY)
        )

        // Draw User Compass FOV cone
        val rad = Math.toRadians((headingDegrees - 90).toDouble())
        val fovLength = 35f
        val fovAngle1 = rad - Math.toRadians(25.0)
        val fovAngle2 = rad + Math.toRadians(25.0)

        val conePath = Path().apply {
            moveTo(userX, userY)
            lineTo(
                userX + (fovLength * cos(fovAngle1)).toFloat(),
                userY + (fovLength * sin(fovAngle1)).toFloat()
            )
            lineTo(
                userX + (fovLength * cos(fovAngle2)).toFloat(),
                userY + (fovLength * sin(fovAngle2)).toFloat()
            )
            close()
        }
        drawPath(
            path = conePath,
            color = Color(0xFF818CF8).copy(alpha = 0.35f)
        )

        // Draw User Circle
        val userColor = if (hasHazard) Color(0xFFEF4444) else Color(0xFF818CF8)
        drawCircle(
            color = userColor,
            radius = 14f,
            center = Offset(userX, userY)
        )
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(userX, userY)
        )
    }
}

@Composable
fun PoiSelectionCard(
    poi: PoiItem,
    language: AppLanguage,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaturalSurfaceElevated),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("poi_card_${poi.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            val icon = when (poi.category) {
                PoiCategory.PHARMACY -> Icons.Default.LocalPharmacy
                PoiCategory.HOSPITAL -> Icons.Default.LocalHospital
                PoiCategory.BUS_STOP -> Icons.Default.DirectionsBus
                PoiCategory.RAILWAY_STATION -> Icons.Default.Train
                PoiCategory.COLLEGE -> Icons.Default.School
                else -> Icons.Default.Navigation
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NaturalMuted,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = poi.name,
                        tint = LavenderPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val displayName = when (language) {
                    AppLanguage.HINDI -> poi.nameHi
                    AppLanguage.MARATHI -> poi.nameMr
                    AppLanguage.ENGLISH -> poi.name
                }
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = poi.address,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Select",
                tint = TextTertiary
            )
        }
    }
}

