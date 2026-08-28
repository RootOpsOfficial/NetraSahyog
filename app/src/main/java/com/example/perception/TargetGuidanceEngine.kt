package com.example.perception

import com.example.model.AppLanguage
import com.example.model.DistanceBucket
import com.example.model.ObstacleType
import com.example.model.SpatialZone
import com.example.model.TargetGuidanceState
import com.example.model.TrackedObstacle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Dedicated engine for Target Guidance Mode ("Where is the door?", "Guide me to the chair", "Find the table").
 * Tracks the target across frames, maintains step counters, and provides actionable direction updates.
 */
class TargetGuidanceEngine {

    private val _guidanceState = MutableStateFlow(TargetGuidanceState())
    val guidanceState: StateFlow<TargetGuidanceState> = _guidanceState.asStateFlow()

    private var targetTypeQuery: ObstacleType? = null
    private var targetRawQuery: String = ""
    private var initialStepCount = 0
    private var lastSpokenStepCount = -1
    private var consecutiveLostFrames = 0
    private var lastGuidanceSpokenTimeMs = 0L

    fun startTargetGuidance(
        query: String,
        currentSteps: Int,
        language: AppLanguage,
        currentObstacles: List<TrackedObstacle>
    ): String {
        targetRawQuery = query
        initialStepCount = currentSteps
        lastSpokenStepCount = -1
        consecutiveLostFrames = 0

        // Determine target obstacle type from user query
        val lower = query.lowercase()
        targetTypeQuery = when {
            lower.contains("door") || lower.contains("entrance") || lower.contains("gate") || lower.contains("entry") -> ObstacleType.DOOR
            lower.contains("chair") || lower.contains("seat") -> ObstacleType.CHAIR
            lower.contains("table") || lower.contains("desk") -> ObstacleType.TABLE
            lower.contains("person") || lower.contains("someone") || lower.contains("human") -> ObstacleType.PERSON
            lower.contains("stair") || lower.contains("step") -> ObstacleType.STAIRS
            lower.contains("bed") -> ObstacleType.BED
            lower.contains("sofa") || lower.contains("couch") -> ObstacleType.SOFA
            lower.contains("laptop") || lower.contains("computer") -> ObstacleType.LAPTOP
            lower.contains("phone") || lower.contains("mobile") -> ObstacleType.PHONE
            lower.contains("bag") || lower.contains("backpack") -> ObstacleType.BAG
            lower.contains("bottle") || lower.contains("cup") -> ObstacleType.BOTTLE
            lower.contains("dog") || lower.contains("cat") -> ObstacleType.DOG
            lower.contains("pole") || lower.contains("pillar") -> ObstacleType.POLE
            else -> ObstacleType.DOOR
        }

        val matching = currentObstacles.filter { it.type == targetTypeQuery }
            .minByOrNull { it.approximateMeters }

        val targetNameEn = targetTypeQuery?.spokenNameEn ?: "target"
        val targetNameHi = targetTypeQuery?.spokenNameHi ?: "लक्ष्य"
        val targetNameMr = targetTypeQuery?.spokenNameMr ?: "लक्ष्य"

        return if (matching != null) {
            val steps = max(1, Math.round(matching.approximateMeters / 0.65f))
            _guidanceState.value = TargetGuidanceState(
                isActive = true,
                targetQuery = query,
                targetType = targetTypeQuery,
                targetLabel = targetNameEn,
                confidence = matching.confidence,
                zone = matching.zone,
                approximateSteps = steps,
                approximateMeters = matching.approximateMeters,
                isVisible = true,
                lastSeenTimeMs = System.currentTimeMillis(),
                startStepCount = currentSteps,
                remainingSteps = steps
            )
            generateInitialTargetResponse(matching, steps, language)
        } else {
            _guidanceState.value = TargetGuidanceState(
                isActive = true,
                targetQuery = query,
                targetType = targetTypeQuery,
                targetLabel = targetNameEn,
                isVisible = false,
                startStepCount = currentSteps,
                guidanceInstructionEn = "Scanning for $targetNameEn...",
                guidanceInstructionHi = "$targetNameHi को खोजा जा रहा है...",
                guidanceInstructionMr = "$targetNameMr शोधत आहे..."
            )
            when (language) {
                AppLanguage.HINDI -> "कैमरे के सामने $targetNameHi खोजा जा रहा है। कैमरा सीधा रखें।"
                AppLanguage.MARATHI -> "कॅमेऱ्यासमोर $targetNameMr शोधत आहे. कॅमेरा सरळ ठेवा."
                AppLanguage.ENGLISH -> "Scanning for $targetNameEn. Please hold phone forward."
            }
        }
    }

    fun stopTargetGuidance(language: AppLanguage): String {
        _guidanceState.value = TargetGuidanceState(isActive = false)
        targetTypeQuery = null
        return when (language) {
            AppLanguage.HINDI -> "लक्ष्य मार्गदर्शन समाप्त।"
            AppLanguage.MARATHI -> "लक्ष्य मार्गदर्शन थांबवले."
            AppLanguage.ENGLISH -> "Target guidance stopped."
        }
    }

    fun updateFrame(
        obstacles: List<TrackedObstacle>,
        currentStepCount: Int,
        language: AppLanguage
    ): String? {
        val state = _guidanceState.value
        if (!state.isActive || targetTypeQuery == null) return null

        val target = obstacles.filter { it.type == targetTypeQuery }
            .minByOrNull { it.approximateMeters }

        val targetNameEn = targetTypeQuery?.spokenNameEn ?: "target"
        val targetNameHi = targetTypeQuery?.spokenNameHi ?: "लक्ष्य"
        val targetNameMr = targetTypeQuery?.spokenNameMr ?: "लक्ष्य"

        if (target != null) {
            consecutiveLostFrames = 0
            val steps = max(1, Math.round(target.approximateMeters / 0.65f))
            val isClose = target.approximateMeters <= 1.1f || steps <= 1

            val (instEn, instHi, instMr) = when {
                isClose -> Triple(
                    "$targetNameEn is directly ahead, very close.",
                    "$targetNameHi बिल्कुल सामने, बहुत पास है।",
                    "$targetNameMr थेट समोर, खूप जवळ आहे."
                )
                target.zone in listOf(SpatialZone.FAR_LEFT, SpatialZone.LEFT) -> Triple(
                    "$targetNameEn is on your left. Move slightly left.",
                    "$targetNameHi बाईं ओर है। थोड़ा बाएँ चलें।",
                    "$targetNameMr डावीकडे आहे. थोडे डावीकडे चला."
                )
                target.zone in listOf(SpatialZone.FAR_RIGHT, SpatialZone.RIGHT) -> Triple(
                    "$targetNameEn is on your right. Move slightly right.",
                    "$targetNameHi दाईं ओर है। थोड़ा दाएँ चलें।",
                    "$targetNameMr उजवीकडे आहे. थोडे उजवीकडे चला."
                )
                else -> Triple(
                    "$targetNameEn ahead. Walk straight for about $steps steps.",
                    "$targetNameHi आगे है। सीधे लगभग $steps कदम चलें।",
                    "$targetNameMr पुढे आहे. सरळ सुमारे $steps पावले चला."
                )
            }

            _guidanceState.value = state.copy(
                confidence = target.confidence,
                zone = target.zone,
                approximateSteps = steps,
                approximateMeters = target.approximateMeters,
                isVisible = true,
                lastSeenTimeMs = System.currentTimeMillis(),
                remainingSteps = steps,
                guidanceInstructionEn = instEn,
                guidanceInstructionHi = instHi,
                guidanceInstructionMr = instMr,
                isReached = isClose
            )

            // Spoken progression update when step count changes or when target is reached
            val now = System.currentTimeMillis()
            val stepDiff = currentStepCount - initialStepCount
            if ((now - lastGuidanceSpokenTimeMs > 4500L && (steps != lastSpokenStepCount || isClose)) || isClose) {
                lastSpokenStepCount = steps
                lastGuidanceSpokenTimeMs = now
                return when (language) {
                    AppLanguage.HINDI -> instHi
                    AppLanguage.MARATHI -> instMr
                    AppLanguage.ENGLISH -> instEn
                }
            }
        } else {
            consecutiveLostFrames++
            if (consecutiveLostFrames > 12 && state.isVisible) {
                _guidanceState.value = state.copy(
                    isVisible = false,
                    guidanceInstructionEn = "$targetNameEn is not currently visible.",
                    guidanceInstructionHi = "$targetNameHi अभी दिखाई नहीं दे रहा है।",
                    guidanceInstructionMr = "$targetNameMr सध्या दिसत नाही आहे."
                )
                val now = System.currentTimeMillis()
                if (now - lastGuidanceSpokenTimeMs > 5000L) {
                    lastGuidanceSpokenTimeMs = now
                    return when (language) {
                        AppLanguage.HINDI -> "$targetNameHi अब दिखाई नहीं दे रहा है। धीरे घूमें।"
                        AppLanguage.MARATHI -> "$targetNameMr आता दिसत नाही आहे. हळू फिरा."
                        AppLanguage.ENGLISH -> "I can no longer see the $targetNameEn."
                    }
                }
            }
        }

        return null
    }

    private fun generateInitialTargetResponse(
        target: TrackedObstacle,
        steps: Int,
        language: AppLanguage
    ): String {
        val targetNameEn = target.type.spokenNameEn
        val targetNameHi = target.type.spokenNameHi
        val targetNameMr = target.type.spokenNameMr
        val zoneEn = target.zone.spokenDescriptionEn
        val zoneHi = target.zone.spokenDescriptionHi
        val zoneMr = target.zone.spokenDescriptionMr

        return when (language) {
            AppLanguage.HINDI -> "$targetNameHi $zoneHi है, लगभग $steps कदम की दूरी पर।"
            AppLanguage.MARATHI -> "$targetNameMr $zoneMr आहे, सुमारे $steps पावलांच्या अंतरावर."
            AppLanguage.ENGLISH -> "$targetNameEn is $zoneEn, approximately $steps steps ahead."
        }
    }
}
