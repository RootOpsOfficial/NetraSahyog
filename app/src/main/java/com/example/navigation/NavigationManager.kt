package com.example.navigation

import com.example.model.AppLanguage
import com.example.model.NavigationState
import com.example.model.NavigationStatus
import com.example.model.PoiItem
import com.example.model.TurnDirection
import com.example.speech.HapticFeedbackManager
import com.example.speech.VoiceAlertManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationManager(
    private val voiceAlertManager: VoiceAlertManager,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val puneRouteStorage: OfflinePuneRouteStorage? = null
) {

    private val router = OfflinePedestrianRouter()

    private val _navState = MutableStateFlow(NavigationState())
    val navState: StateFlow<NavigationState> = _navState.asStateFlow()

    private var lastOffRouteSpokenTimeMs = 0L

    suspend fun searchDestinations(query: String, userLat: Double, userLon: Double): List<PoiItem> {
        return puneRouteStorage?.searchPois(query, userLat, userLon) ?: emptyList()
    }

    suspend fun getAllAvailablePois(): List<PoiItem> {
        return puneRouteStorage?.getAllPois() ?: PuneOsmDataset.PUNE_POIS
    }

    fun startNavigation(
        destination: PoiItem,
        startLat: Double = 18.52043,
        startLon: Double = 73.84365,
        language: AppLanguage = AppLanguage.ENGLISH
    ) {
        val segments = router.calculateRoute(startLat, startLon, destination)
        val totalDistance = segments.sumOf { it.distanceMeters }
        val polyline = segments.flatMap { it.polylinePoints.ifEmpty { listOf(Pair(it.startLat, it.startLon), Pair(it.endLat, it.endLon)) } }

        _navState.value = NavigationState(
            status = NavigationStatus.NAVIGATING,
            currentDestination = destination,
            totalRouteDistanceMeters = totalDistance,
            remainingDistanceMeters = totalDistance,
            currentStepIndex = 0,
            currentStep = segments.firstOrNull(),
            nextStep = segments.getOrNull(1),
            segments = segments,
            routePolyline = polyline,
            isOffRoute = false
        )

        speakCurrentInstruction(language)
        hapticFeedbackManager.vibrateForNavigationTurn(false)
    }

    fun onLocationUpdated(userLat: Double, userLon: Double, language: AppLanguage) {
        val current = _navState.value
        if (current.status != NavigationStatus.NAVIGATING) return
        val currentStep = current.currentStep ?: return
        val destination = current.currentDestination ?: return

        val distToEndOfStep = OfflinePedestrianRouter.calculateDistanceMeters(userLat, userLon, currentStep.endLat, currentStep.endLon)
        val distToDest = OfflinePedestrianRouter.calculateDistanceMeters(userLat, userLon, destination.latitude, destination.longitude)

        // Check if user reached end of step
        if (distToEndOfStep <= 10 && current.currentStepIndex < current.segments.size - 1) {
            advanceToNextStep(language)
            return
        }

        // Check if arrived at destination
        if (distToDest <= 12) {
            _navState.value = current.copy(
                status = NavigationStatus.ARRIVED,
                remainingDistanceMeters = 0,
                currentStep = null,
                nextStep = null
            )
            speakArrival(language)
            hapticFeedbackManager.vibrateSuccess()
            return
        }

        // Check for off-route (> 40 meters from current step path)
        val distToStepStart = OfflinePedestrianRouter.calculateDistanceMeters(userLat, userLon, currentStep.startLat, currentStep.startLon)
        val isOff = distToStepStart > (currentStep.distanceMeters + 35) && distToEndOfStep > 35

        if (isOff && !current.isOffRoute) {
            val now = System.currentTimeMillis()
            if (now - lastOffRouteSpokenTimeMs > 10000L) {
                lastOffRouteSpokenTimeMs = now
                _navState.value = current.copy(
                    isOffRoute = true,
                    status = NavigationStatus.OFF_ROUTE_RECALCULATING
                )
                val offRouteMsg = when (language) {
                    AppLanguage.HINDI -> "आप रास्ते से भटक गए हैं। नया रास्ता बनाया जा रहा है।"
                    AppLanguage.MARATHI -> "तुम्ही रस्त्यावरून भरकटला आहात. नवीन मार्ग शोधत आहे."
                    AppLanguage.ENGLISH -> "You are off route. Recalculating route..."
                }
                voiceAlertManager.speak(offRouteMsg, forceInterrupt = true)
                // Recalculate
                startNavigation(destination, userLat, userLon, language)
            }
        }
    }

    fun advanceToNextStep(language: AppLanguage = AppLanguage.ENGLISH) {
        val currentState = _navState.value
        if (currentState.status != NavigationStatus.NAVIGATING) return

        val nextIndex = currentState.currentStepIndex + 1
        if (nextIndex >= currentState.segments.size) {
            _navState.value = currentState.copy(
                status = NavigationStatus.ARRIVED,
                remainingDistanceMeters = 0,
                currentStep = null,
                nextStep = null
            )
            speakArrival(language)
            hapticFeedbackManager.vibrateSuccess()
            return
        }

        val nextStep = currentState.segments[nextIndex]
        val remainingDist = currentState.segments.drop(nextIndex).sumOf { it.distanceMeters }

        _navState.value = currentState.copy(
            currentStepIndex = nextIndex,
            currentStep = nextStep,
            nextStep = currentState.segments.getOrNull(nextIndex + 1),
            remainingDistanceMeters = remainingDist
        )

        val isLeft = nextStep.instruction in listOf(TurnDirection.LEFT, TurnDirection.SLIGHT_LEFT)
        hapticFeedbackManager.vibrateForNavigationTurn(isLeft)
        speakCurrentInstruction(language)
    }

    fun stopNavigation(language: AppLanguage = AppLanguage.ENGLISH) {
        _navState.value = NavigationState(status = NavigationStatus.IDLE)
        val msg = when (language) {
            AppLanguage.HINDI -> "नेविगेशन समाप्त किया गया"
            AppLanguage.MARATHI -> "नेव्हिगेशन थांबवले आहे"
            AppLanguage.ENGLISH -> "Navigation stopped"
        }
        voiceAlertManager.speak(msg, forceInterrupt = true)
    }

    fun updateCameraHazardOverride(hazardSpokenAlert: String?, isUrgent: Boolean) {
        val current = _navState.value
        if (current.status != NavigationStatus.NAVIGATING && current.status != NavigationStatus.HAZARD_DETECTED) return

        if (hazardSpokenAlert != null && isUrgent) {
            _navState.value = current.copy(
                status = NavigationStatus.HAZARD_DETECTED,
                isCameraHazardBlocking = true,
                currentHazardAlert = hazardSpokenAlert
            )
        } else if (hazardSpokenAlert == null && current.status == NavigationStatus.HAZARD_DETECTED) {
            _navState.value = current.copy(
                status = NavigationStatus.NAVIGATING,
                isCameraHazardBlocking = false,
                currentHazardAlert = null
            )
        }
    }

    private fun speakCurrentInstruction(language: AppLanguage) {
        val step = _navState.value.currentStep ?: return
        val dest = _navState.value.currentDestination ?: return

        val instructionText = when (language) {
            AppLanguage.HINDI -> {
                when (step.instruction) {
                    TurnDirection.ARRIVED -> "आप ${dest.nameHi} पहुँच गए हैं"
                    TurnDirection.CROSSING -> "सावधान! आगे पैदल क्रॉसिंग है। ${step.distanceMeters} मीटर"
                    TurnDirection.STAIRS -> "आगे सीढ़ियाँ हैं। ध्यान से चलें। ${step.distanceMeters} मीटर"
                    else -> "${step.instruction.spokenHi}, ${step.streetOrFootpathName}, ${step.distanceMeters} मीटर"
                }
            }
            AppLanguage.MARATHI -> {
                when (step.instruction) {
                    TurnDirection.ARRIVED -> "तुम्ही ${dest.nameMr} येथे पोहोचला आहात"
                    TurnDirection.CROSSING -> "सावधान! पुढे पादचारी क्रॉसिंग आहे. ${step.distanceMeters} मीटर"
                    TurnDirection.STAIRS -> "पुढे पायऱ्या आहेत. काळजीपूर्वक चाला. ${step.distanceMeters} मीटर"
                    else -> "${step.instruction.spokenMr}, ${step.streetOrFootpathName}, ${step.distanceMeters} मीटर"
                }
            }
            AppLanguage.ENGLISH -> {
                when (step.instruction) {
                    TurnDirection.ARRIVED -> "You have arrived at ${dest.name}"
                    TurnDirection.CROSSING -> "Attention: Pedestrian crossing ahead for ${step.distanceMeters} meters"
                    TurnDirection.STAIRS -> "Stairs ahead in ${step.distanceMeters} meters. Step carefully"
                    else -> "${step.instruction.spokenEn} on ${step.streetOrFootpathName} for ${step.distanceMeters} meters"
                }
            }
        }
        voiceAlertManager.speak(instructionText, forceInterrupt = true)
    }

    private fun speakArrival(language: AppLanguage) {
        val dest = _navState.value.currentDestination ?: return
        val msg = when (language) {
            AppLanguage.HINDI -> "बधाई! आप ${dest.nameHi} पहुँच गए हैं"
            AppLanguage.MARATHI -> "अभिनंदन! तुम्ही ${dest.nameMr} येथे पोहोचला आहात"
            AppLanguage.ENGLISH -> "Destination reached: ${dest.name}"
        }
        voiceAlertManager.speak(msg, forceInterrupt = true)
    }
}
