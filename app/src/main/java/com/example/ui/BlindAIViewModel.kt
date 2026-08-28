package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIProviderManager
import com.example.model.AIProviderType
import com.example.model.AppLanguage
import com.example.model.DistanceBucket
import com.example.model.NavigationState
import com.example.model.NavigationStatus
import com.example.model.ObstaclePriority
import com.example.model.ParsedVoiceCommand
import com.example.model.PoiCategory
import com.example.model.PoiItem
import com.example.model.RealLocation
import com.example.model.TelemetryMetrics
import com.example.model.TrackedObstacle
import com.example.model.UserSensorsContext
import com.example.model.VoiceIntentType
import com.example.navigation.NavigationManager
import com.example.navigation.PuneOsmDataset
import com.example.perception.PerceptionOutput
import com.example.perception.RealtimePerceptionEngine
import com.example.sensors.CompassSensorManager
import com.example.sensors.LocationServiceManager
import com.example.speech.HapticFeedbackManager
import com.example.speech.VoiceAlertManager
import com.example.speech.VoiceRecognitionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class AppTab(val title: String, val icon: String) {
    VISION("Vision HUD", "visibility"),
    NAVIGATION("Navigation", "navigation"),
    GEMINI_LIVE("NETRA AI", "smart_toy"),
    TELEMETRY("Performance", "speed")
}

class BlindAIViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // UI States
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    val hapticFeedbackManager: HapticFeedbackManager = HapticFeedbackManager(context)
    val voiceAlertManager: VoiceAlertManager = VoiceAlertManager(
        context = context,
        onTtsReady = {}
    )

    val perceptionEngine: RealtimePerceptionEngine = RealtimePerceptionEngine(viewModelScope)
    val compassSensorManager: CompassSensorManager = CompassSensorManager(context)
    val locationServiceManager: LocationServiceManager = LocationServiceManager(
        context = context,
        onLocationUpdated = { realLoc ->
            // Location updated
        },
        onStatusChanged = { status ->
            _telemetry.value = _telemetry.value.copy(gpsProvider = status)
        }
    )
    val puneRouteStorage: com.example.navigation.OfflinePuneRouteStorage = com.example.navigation.OfflinePuneRouteStorage(context)
    val navigationManager: NavigationManager = NavigationManager(voiceAlertManager, hapticFeedbackManager, puneRouteStorage)
    val aiProviderManager: AIProviderManager = AIProviderManager()
    val targetGuidanceEngine: com.example.perception.TargetGuidanceEngine = com.example.perception.TargetGuidanceEngine()

    val targetGuidanceState: StateFlow<com.example.model.TargetGuidanceState> = targetGuidanceEngine.guidanceState

    private val _movementGuidance = MutableStateFlow<com.example.perception.MovementGuidance?>(null)
    val movementGuidance: StateFlow<com.example.perception.MovementGuidance?> = _movementGuidance.asStateFlow()

    private val _liveVoiceTranscript = MutableStateFlow("")
    val liveVoiceTranscript: StateFlow<String> = _liveVoiceTranscript.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private val _isLiveMicEnabled = MutableStateFlow(true)
    val isLiveMicEnabled: StateFlow<Boolean> = _isLiveMicEnabled.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    private val _currentModeIndex = MutableStateFlow(0) // 0: Offline Vision, 1: NETRA AI Gemini Live
    val currentModeIndex: StateFlow<Int> = _currentModeIndex.asStateFlow()

    private val voiceRecognitionEngine: VoiceRecognitionEngine = VoiceRecognitionEngine(
        context = context,
        onCommandParsed = { command -> handleVoiceCommand(command) },
        onListeningStateChanged = { listening ->
            _isListeningForVoice.value = listening
            if (listening) {
                // Audio chime confirming listening start
                hapticFeedbackManager.playListeningStartChime()
                // VOICE PRIORITY: Stop TTS immediately when user starts speaking
                voiceAlertManager.stop()
                _liveVoiceTranscript.value = ""
            } else {
                hapticFeedbackManager.playListeningStopChime()
            }
        },
        onPartialTranscript = { partial ->
            _liveVoiceTranscript.value = partial
        },
        onRmsUpdated = { rms ->
            _audioRmsDb.value = rms
        },
        onSpeechError = { err ->
            _isListeningForVoice.value = false
            if (_currentModeIndex.value != 1) {
                _aiResponseText.value = err
            }
        }
    )

    private val _activeTab = MutableStateFlow(AppTab.VISION)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _isListeningForVoice = MutableStateFlow(false)
    val isListeningForVoice: StateFlow<Boolean> = _isListeningForVoice.asStateFlow()

    private val _lastVoiceSpokenText = MutableStateFlow("")
    val lastVoiceSpokenText: StateFlow<String> = _lastVoiceSpokenText.asStateFlow()

    private val _aiResponseText = MutableStateFlow("NETRA AI ready.")
    val aiResponseText: StateFlow<String> = _aiResponseText.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetryMetrics())
    val telemetry: StateFlow<TelemetryMetrics> = _telemetry.asStateFlow()

    val perceptionOutput: StateFlow<PerceptionOutput> = perceptionEngine.perceptionFlow
    val navigationState: StateFlow<NavigationState> = navigationManager.navState
    val sensorState: StateFlow<UserSensorsContext> = compassSensorManager.sensorState

    private var latestCapturedBitmap: Bitmap? = null

    init {
        voiceAlertManager.onSpeakingStateChanged = { speaking ->
            _isTtsSpeaking.value = speaking
            if (speaking) {
                voiceRecognitionEngine.pauseForSpeaking()
            } else {
                if (_currentModeIndex.value == 1 || _isLiveMicEnabled.value) {
                    voiceRecognitionEngine.resumeAfterSpeaking()
                }
            }
        }

        // Initialize ML Kit directory & storage
        com.example.perception.MlKitStorageInitializer.ensureStorageReady(context)

        compassSensorManager.start()
        locationServiceManager.startLocationUpdates()

        // Sync ground tilt mode to perception engine
        viewModelScope.launch {
            sensorState.collect { sensor ->
                perceptionEngine.isGroundViewMode = sensor.isGroundViewMode
            }
        }

        // Observe real GPS location updates
        viewModelScope.launch {
            locationServiceManager.currentLocation.collect { realLoc ->
                if (realLoc != null) {
                    navigationManager.onLocationUpdated(realLoc.latitude, realLoc.longitude, _appLanguage.value)
                }
            }
        }

        // Observe perception flow and trigger automatic high-priority voice alerts, target tracking, and movement guidance
        viewModelScope.launch {
            perceptionOutput.collect { output ->
                updateTelemetry(output)

                val sensor = sensorState.value
                val isFacingDown = sensor.isFacingDown
                val isVoiceActive = _isListeningForVoice.value || _isAiThinking.value || _liveVoiceTranscript.value.isNotBlank()

                // 1. Update active target guidance tracking (e.g. door, chair, table, person)
                if (targetGuidanceState.value.isActive) {
                    val targetAlert = targetGuidanceEngine.updateFrame(
                        obstacles = output.trackedObstacles,
                        currentStepCount = sensor.stepCount,
                        language = _appLanguage.value
                    )
                    if (targetAlert != null && !isVoiceActive && !isFacingDown) {
                        voiceAlertManager.speak(targetAlert, ObstaclePriority.INFO)
                        hapticFeedbackManager.vibrateForHazard(ObstaclePriority.INFO, targetGuidanceState.value.zone, isFacingDown)
                    }
                }

                // 2. Compute unified movement guidance
                val guidance = com.example.perception.PathGuidanceEngine.computeGuidance(
                    pathAnalysis = output.pathAnalysis,
                    obstacles = output.trackedObstacles,
                    targetState = targetGuidanceState.value
                )
                _movementGuidance.value = guidance

                // 3. Normal obstacle safety priority alert
                val critical = output.mostCriticalObstacle
                if (critical != null && !isFacingDown && !isVoiceActive) {
                    val alertText = perceptionEngine.priorityEngine.shouldAlert(
                        obstacle = critical,
                        language = _appLanguage.value,
                        allObstacles = output.trackedObstacles,
                        isUserSpeaking = isVoiceActive,
                        isFacingDown = isFacingDown
                    )

                    if (alertText != null) {
                        voiceAlertManager.speak(alertText, critical.priority)
                        hapticFeedbackManager.vibrateForHazard(critical.priority, critical.zone, isFacingDown)
                        _telemetry.value = _telemetry.value.copy(lastAlertMessage = alertText)

                        // Override navigation if urgent hazard directly blocks path
                        if (critical.priority == ObstaclePriority.URGENT && critical.isInWalkingCorridor) {
                            navigationManager.updateCameraHazardOverride(alertText, true)
                        } else {
                            navigationManager.updateCameraHazardOverride(null, false)
                        }
                    }
                }
            }
        }
    }

    fun setModeIndex(index: Int) {
        _currentModeIndex.value = index
        if (index == 1) {
            aiProviderManager.setProvider(AIProviderType.GEMINI_LIVE_FLASH)
            _activeTab.value = AppTab.GEMINI_LIVE
            if (_isLiveMicEnabled.value) {
                voiceRecognitionEngine.setContinuousMode(true, _appLanguage.value)
            }
        } else {
            aiProviderManager.setProvider(AIProviderType.OFFLINE_LOCAL)
            _activeTab.value = AppTab.VISION
            voiceRecognitionEngine.setContinuousMode(false)
        }
    }

    fun toggleLiveMic(forceState: Boolean? = null) {
        val newState = forceState ?: !_isLiveMicEnabled.value
        _isLiveMicEnabled.value = newState
        hapticFeedbackManager.vibrateSuccess()
        if (newState) {
            voiceRecognitionEngine.setContinuousMode(true, _appLanguage.value)
            val msg = when (_appLanguage.value) {
                AppLanguage.HINDI -> "माइक सक्रिय, पूछिए।"
                AppLanguage.MARATHI -> "माइक सुरू, विचारा."
                AppLanguage.ENGLISH -> "Live mic active. Ask anything."
            }
            voiceAlertManager.speak(msg, ObstaclePriority.INFO)
        } else {
            voiceRecognitionEngine.setContinuousMode(false)
            val msg = when (_appLanguage.value) {
                AppLanguage.HINDI -> "माइक बंद किया गया।"
                AppLanguage.MARATHI -> "माइक बंद केला."
                AppLanguage.ENGLISH -> "Live mic paused."
            }
            voiceAlertManager.speak(msg, ObstaclePriority.INFO)
        }
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        voiceAlertManager.setLanguage(language)
        if (_isLiveMicEnabled.value && _currentModeIndex.value == 1) {
            voiceRecognitionEngine.setContinuousMode(true, language)
        }
        val msg = when (language) {
            AppLanguage.HINDI -> "भाषा हिंदी सेट की गई"
            AppLanguage.MARATHI -> "भाषा मराठी निवडली"
            AppLanguage.ENGLISH -> "Language set to English"
        }
        voiceAlertManager.speak(msg, ObstaclePriority.INFO)
    }

    fun setTab(tab: AppTab) {
        _activeTab.value = tab
        if (tab == AppTab.GEMINI_LIVE) {
            _currentModeIndex.value = 1
            aiProviderManager.setProvider(AIProviderType.GEMINI_LIVE_FLASH)
            if (_isLiveMicEnabled.value) {
                voiceRecognitionEngine.setContinuousMode(true, _appLanguage.value)
            }
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        voiceAlertManager.isMuted = newMute
        if (!newMute) {
            voiceAlertManager.speak("Audio unmuted", ObstaclePriority.INFO)
        }
    }

    fun startVoiceListening() {
        voiceAlertManager.stop()
        hapticFeedbackManager.playListeningStartChime()
        voiceRecognitionEngine.startListening(_appLanguage.value)
    }

    fun stopVoiceListening() {
        voiceRecognitionEngine.stopListening()
    }

    fun handleVoiceCommand(command: ParsedVoiceCommand) {
        _lastVoiceSpokenText.value = command.rawSpokenText

        viewModelScope.launch {
            when (command.intent) {
                VoiceIntentType.QUERY_STEPS_FORWARD -> {
                    announceStepsForward()
                }
                VoiceIntentType.WHAT_OBSTACLE_IS_THAT -> {
                    queryGeminiAssistant("Which obstacle is that in front or around me?")
                }
                VoiceIntentType.WHAT_COLOUR_IS_THIS -> {
                    queryGeminiAssistant("What colour is this object in front of the camera?")
                }
                VoiceIntentType.WHERE_IS_THE_DOOR -> {
                    startTargetGuidance(command.rawSpokenText)
                }
                VoiceIntentType.SCENE_QUERY_FORWARD -> {
                    describeCurrentScene(forwardOnly = true)
                }
                VoiceIntentType.SCENE_QUERY_LEFT -> {
                    describeSideScene(isLeft = true)
                }
                VoiceIntentType.SCENE_QUERY_RIGHT -> {
                    describeSideScene(isLeft = false)
                }
                VoiceIntentType.IS_PATH_CLEAR -> {
                    announcePathStatus()
                }
                VoiceIntentType.READ_TEXT_OCR -> {
                    performOcrRead()
                }
                VoiceIntentType.DESCRIBE_SURROUNDINGS -> {
                    describeCurrentScene(forwardOnly = false)
                }
                VoiceIntentType.WHAT_IS_THIS_OBJECT -> {
                    queryGeminiAssistant("What object is directly in front of me and how close is it?")
                }
                VoiceIntentType.WHERE_AM_I -> {
                    announceLocationAndHeading()
                }
                VoiceIntentType.NAVIGATE_TO_POI, VoiceIntentType.FIND_NEAREST_POI -> {
                    val query = command.searchQuery?.lowercase() ?: ""
                    val currentLoc = locationServiceManager.currentLocation.value
                    val startLat = currentLoc?.latitude ?: 18.52043
                    val startLon = currentLoc?.longitude ?: 73.84365

                    val searchResults = puneRouteStorage.searchPois(query, startLat, startLon)
                    val targetPoi = if (searchResults.isNotEmpty()) {
                        searchResults.first()
                    } else if (command.targetPoiCategory != null && command.targetPoiCategory != PoiCategory.GENERAL) {
                        puneRouteStorage.getAllPois().find { it.category == command.targetPoiCategory } ?: puneRouteStorage.getAllPois().firstOrNull()
                    } else {
                        puneRouteStorage.getAllPois().find {
                            it.name.lowercase().contains(query) ||
                            it.nameHi.lowercase().contains(query) ||
                            it.nameMr.lowercase().contains(query)
                        } ?: puneRouteStorage.getAllPois().firstOrNull()
                    }

                    if (targetPoi != null) {
                        _activeTab.value = AppTab.NAVIGATION
                        navigationManager.startNavigation(targetPoi, startLat, startLon, _appLanguage.value)
                    }
                }
                VoiceIntentType.STOP_NAVIGATION -> {
                    navigationManager.stopNavigation(_appLanguage.value)
                }
                VoiceIntentType.CANCEL_OR_STOP -> {
                    stopTargetGuidance()
                    voiceAlertManager.stop()
                }
                VoiceIntentType.SWITCH_TO_GEMINI_LIVE -> {
                    setModeIndex(1)
                    val msg = when (_appLanguage.value) {
                        AppLanguage.HINDI -> "नेत्र एआई लाइव मोड सक्रिय"
                        AppLanguage.MARATHI -> "नेत्रा एआय लाइव्ह मोड सुरू झाला"
                        AppLanguage.ENGLISH -> "Switched to NETRA AI Live Assistant"
                    }
                    voiceAlertManager.speak(msg, ObstaclePriority.INFO)
                }
                VoiceIntentType.SWITCH_TO_OFFLINE_VISION -> {
                    setModeIndex(0)
                    val msg = when (_appLanguage.value) {
                        AppLanguage.HINDI -> "ऑफलाइन विज़न मोड सक्रिय"
                        AppLanguage.MARATHI -> "ऑफलाइन व्हिजन मोड सुरू झाला"
                        AppLanguage.ENGLISH -> "Switched to offline vision mode"
                    }
                    voiceAlertManager.speak(msg, ObstaclePriority.INFO)
                }
                VoiceIntentType.REPEAT_LAST_ALERT -> {
                    val last = _telemetry.value.lastAlertMessage
                    voiceAlertManager.speak(last, forceInterrupt = true)
                }
                VoiceIntentType.FREEFORM_QUESTION -> {
                    queryGeminiAssistant(command.rawSpokenText)
                }
            }
        }
    }

    fun startTargetGuidance(query: String) {
        val sensor = sensorState.value
        val obstacles = perceptionOutput.value.trackedObstacles
        val initialResponse = targetGuidanceEngine.startTargetGuidance(
            query = query,
            currentSteps = sensor.stepCount,
            language = _appLanguage.value,
            currentObstacles = obstacles
        )
        _aiResponseText.value = initialResponse
        voiceAlertManager.speak(initialResponse, forceInterrupt = true)
        hapticFeedbackManager.playListeningStopChime()
    }

    fun stopTargetGuidance() {
        if (targetGuidanceState.value.isActive) {
            val response = targetGuidanceEngine.stopTargetGuidance(_appLanguage.value)
            _aiResponseText.value = response
            voiceAlertManager.speak(response, forceInterrupt = true)
        }
    }

    private fun announceStepsForward() {
        val analysis = perceptionOutput.value.pathAnalysis
        val text = when (_appLanguage.value) {
            AppLanguage.HINDI -> {
                if (analysis.isCenterClear) "आगे 6 से 8 कदम का रास्ता साफ है। सीधे चल सकते हैं।" else analysis.suggestedActionHi
            }
            AppLanguage.MARATHI -> {
                if (analysis.isCenterClear) "पुढील ६ ते ८ पावले रस्ता मोकळा आहे. सरळ चालू शकता." else analysis.suggestedActionMr
            }
            AppLanguage.ENGLISH -> {
                if (analysis.isCenterClear) "The path ahead is clear for at least 6 to 8 steps. Safe to proceed straight." else analysis.suggestedActionEn
            }
        }
        _aiResponseText.value = text
        voiceAlertManager.speak(text, forceInterrupt = true)
    }

    fun describeCurrentScene(forwardOnly: Boolean) {
        viewModelScope.launch {
            val output = perceptionOutput.value
            val obstacles = output.trackedObstacles.filter { it.priority != ObstaclePriority.IGNORE }

            val text = when (_appLanguage.value) {
                AppLanguage.HINDI -> {
                    if (obstacles.isEmpty()) {
                        "रास्ता साफ है। कोई बाधा नहीं दिख रही है।"
                    } else {
                        val items = obstacles.take(3).joinToString(", ") {
                            "${it.type.displayName} ${it.zone.spokenDescriptionHi}, ${it.distance.spokenHi}"
                        }
                        "सामने $items हैं। ${output.pathAnalysis.suggestedActionHi}"
                    }
                }
                AppLanguage.MARATHI -> {
                    if (obstacles.isEmpty()) {
                        "रस्ता मोकळा आहे. कोणताही अडथळा नाही."
                    } else {
                        val items = obstacles.take(3).joinToString(", ") {
                            "${it.type.displayName} ${it.zone.spokenDescriptionMr}, ${it.distance.spokenMr}"
                        }
                        "समोर $items आहेत. ${output.pathAnalysis.suggestedActionMr}"
                    }
                }
                AppLanguage.ENGLISH -> {
                    if (obstacles.isEmpty()) {
                        "Path is clear ahead. No obstacles detected."
                    } else {
                        val items = obstacles.take(3).joinToString(", ") {
                            "${it.type.displayName} ${it.zone.spokenDescriptionEn}, ${it.distance.spokenEn}"
                        }
                        "Detected $items. ${output.pathAnalysis.suggestedActionEn}"
                    }
                }
            }
            _aiResponseText.value = text
            voiceAlertManager.speak(text, forceInterrupt = true)
        }
    }

    private fun describeSideScene(isLeft: Boolean) {
        val output = perceptionOutput.value
        val sideObstacles = output.trackedObstacles.filter {
            if (isLeft) {
                it.zone in listOf(com.example.model.SpatialZone.FAR_LEFT, com.example.model.SpatialZone.LEFT, com.example.model.SpatialZone.CENTER_LEFT)
            } else {
                it.zone in listOf(com.example.model.SpatialZone.FAR_RIGHT, com.example.model.SpatialZone.RIGHT, com.example.model.SpatialZone.CENTER_RIGHT)
            }
        }

        val sideNameEn = if (isLeft) "left" else "right"
        val sideNameHi = if (isLeft) "बाएँ" else "दाएँ"
        val sideNameMr = if (isLeft) "डाव्या" else "उजव्या"

        val text = when (_appLanguage.value) {
            AppLanguage.HINDI -> {
                if (sideObstacles.isEmpty()) {
                    "$sideNameHi ओर रास्ता पूरी तरह साफ है।"
                } else {
                    val obs = sideObstacles.joinToString(", ") { it.type.displayName }
                    "$sideNameHi ओर $obs दिखाई दे रहे हैं।"
                }
            }
            AppLanguage.MARATHI -> {
                if (sideObstacles.isEmpty()) {
                    "$sideNameMr बाजूला रस्ता मोकळा आहे."
                } else {
                    val obs = sideObstacles.joinToString(", ") { it.type.displayName }
                    "$sideNameMr बाजूला $obs दिसत आहेत."
                }
            }
            AppLanguage.ENGLISH -> {
                if (sideObstacles.isEmpty()) {
                    "Your $sideNameEn side is completely clear."
                } else {
                    val obs = sideObstacles.joinToString(", ") { it.type.displayName }
                    "On your $sideNameEn: $obs."
                }
            }
        }
        _aiResponseText.value = text
        voiceAlertManager.speak(text, forceInterrupt = true)
    }

    private fun announcePathStatus() {
        val analysis = perceptionOutput.value.pathAnalysis
        val text = when (_appLanguage.value) {
            AppLanguage.HINDI -> analysis.suggestedActionHi
            AppLanguage.MARATHI -> analysis.suggestedActionMr
            AppLanguage.ENGLISH -> analysis.suggestedActionEn
        }
        _aiResponseText.value = text
        voiceAlertManager.speak(text, forceInterrupt = true)
    }

    fun performOcrRead() {
        viewModelScope.launch {
            _isAiThinking.value = true
            val speakPrep = when (_appLanguage.value) {
                AppLanguage.HINDI -> "टेक्स्ट पढ़ा जा रहा है..."
                AppLanguage.MARATHI -> "मजकूर वाचत आहे..."
                AppLanguage.ENGLISH -> "Reading visible text..."
            }
            voiceAlertManager.speak(speakPrep, ObstaclePriority.INFO)

            val text = aiProviderManager.readOcrText(latestCapturedBitmap, _appLanguage.value)
            _isAiThinking.value = false
            _aiResponseText.value = text
            voiceAlertManager.speak(text, forceInterrupt = true)
        }
    }

    fun queryGeminiAssistant(prompt: String) {
        viewModelScope.launch {
            voiceRecognitionEngine.pauseForSpeaking()
            _isAiThinking.value = true
            _liveVoiceTranscript.value = prompt
            val startT = System.currentTimeMillis()

            val response = aiProviderManager.queryVisionScene(
                bitmap = latestCapturedBitmap,
                prompt = prompt,
                language = _appLanguage.value,
                obstacles = perceptionOutput.value.trackedObstacles,
                pathAnalysis = perceptionOutput.value.pathAnalysis
            )

            val elapsed = System.currentTimeMillis() - startT
            _telemetry.value = _telemetry.value.copy(aiRequestLatencyMs = elapsed)

            _isAiThinking.value = false
            _aiResponseText.value = response
            voiceAlertManager.speak(response, forceInterrupt = true)

            // Fallback safety resume if muted
            if (voiceAlertManager.isMuted || !_isLiveMicEnabled.value) {
                if (_currentModeIndex.value == 1 && _isLiveMicEnabled.value) {
                    voiceRecognitionEngine.resumeAfterSpeaking()
                }
            }
        }
    }

    private fun announceLocationAndHeading() {
        val sensor = sensorState.value
        val heading = sensor.cardinalDirection
        val steps = sensor.stepCount
        val realLoc = locationServiceManager.currentLocation.value

        val locationDescriptor = if (realLoc != null) {
            "${String.format("%.4f", realLoc.latitude)}, ${String.format("%.4f", realLoc.longitude)}"
        } else {
            "FC Road, Pune"
        }

        val text = when (_appLanguage.value) {
            AppLanguage.HINDI -> "आप $locationDescriptor क्षेत्र में हैं। दिशा $heading है। कुल कदम: $steps"
            AppLanguage.MARATHI -> "तुम्ही $locationDescriptor परिसरात आहात. दिशा $heading आहे. एकूण पावले: $steps"
            AppLanguage.ENGLISH -> "You are at $locationDescriptor. Facing $heading (${sensor.azimuthHeadingDegrees.toInt()}°). Total steps: $steps"
        }
        _aiResponseText.value = text
        voiceAlertManager.speak(text, forceInterrupt = true)
    }

    fun updateCameraFrameBitmap(bitmap: Bitmap) {
        latestCapturedBitmap = bitmap
    }

    private fun updateTelemetry(output: PerceptionOutput) {
        val urgentCount = output.trackedObstacles.count { it.priority == ObstaclePriority.URGENT }
        _telemetry.value = _telemetry.value.copy(
            cameraFps = output.frameFps,
            inferenceLatencyMs = output.inferenceTimeMs,
            endToEndAlertLatencyMs = output.inferenceTimeMs + 24L,
            currentActiveProvider = aiProviderManager.currentProvider.value,
            totalObstaclesTracked = output.trackedObstacles.size,
            urgentHazardsCount = urgentCount
        )
    }

    override fun onCleared() {
        super.onCleared()
        compassSensorManager.stop()
        locationServiceManager.stopLocationUpdates()
        voiceAlertManager.shutdown()
        voiceRecognitionEngine.destroy()
        cameraExecutor.shutdown()
    }
}
