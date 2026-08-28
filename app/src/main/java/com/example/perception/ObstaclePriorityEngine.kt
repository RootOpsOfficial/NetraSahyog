package com.example.perception

import com.example.model.AppLanguage
import com.example.model.DistanceBucket
import com.example.model.ObstaclePriority
import com.example.model.ObstacleType
import com.example.model.SpatialZone
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis
import kotlin.math.abs
import kotlin.math.max

data class AnnouncedObstacleRecord(
    val trackId: Int,
    val type: ObstacleType,
    val zone: SpatialZone,
    var lastAnnouncedDistanceMeters: Float,
    var lastAnnouncedTimeMs: Long,
    var hasAnnouncedUrgent: Boolean = false
)

class ObstaclePriorityEngine {

    private var lastSpokenMessage = ""
    private var lastSpokenTimeMs = 0L
    private var lastSpokenPriority = ObstaclePriority.IGNORE

    // Temporary memory of announced obstacles to prevent repeating alerts when user is stationary
    private val announcedTracks = mutableMapOf<Int, AnnouncedObstacleRecord>()
    private val spatialAnnouncedMap = mutableMapOf<String, AnnouncedObstacleRecord>()

    fun evaluatePriority(obstacle: TrackedObstacle): ObstaclePriority {
        val inCorridor = obstacle.isInWalkingCorridor
        val isVeryNear = obstacle.distance == DistanceBucket.VERY_NEAR
        val isNear = obstacle.distance == DistanceBucket.NEAR
        val isVehicle = obstacle.type in listOf(
            ObstacleType.VEHICLE,
            ObstacleType.CAR,
            ObstacleType.MOTORCYCLE,
            ObstacleType.BUS,
            ObstacleType.TRUCK,
            ObstacleType.BICYCLE
        )

        return when {
            // URGENT: Approaching vehicle, very close in corridor (<1.2m), stairs directly ahead, or wall
            (isVehicle && (inCorridor || obstacle.isApproaching)) -> ObstaclePriority.URGENT
            (inCorridor && isVeryNear) -> ObstaclePriority.URGENT
            (obstacle.type == ObstacleType.STAIRS && isVeryNear) -> ObstaclePriority.URGENT
            (obstacle.type == ObstacleType.WALL && isVeryNear) -> ObstaclePriority.URGENT

            // WARNING: Objects in walking corridor (1-2.2m), or near vehicles
            (inCorridor && isNear) -> ObstaclePriority.WARNING
            (isVehicle && isNear) -> ObstaclePriority.WARNING
            (obstacle.type == ObstacleType.STAIRS && isNear) -> ObstaclePriority.WARNING
            (obstacle.type == ObstacleType.DOOR && isNear) -> ObstaclePriority.WARNING

            // INFO: Side obstacles or medium distance
            (!inCorridor && (isVeryNear || isNear)) -> ObstaclePriority.INFO
            (inCorridor && obstacle.distance == DistanceBucket.MEDIUM) -> ObstaclePriority.INFO

            else -> ObstaclePriority.IGNORE
        }
    }

    fun analyzeWalkablePath(obstacles: List<TrackedObstacle>): WalkablePathAnalysis {
        var centerBlocked = false
        var leftBlocked = false
        var rightBlocked = false
        var highestHazard: TrackedObstacle? = null
        var maxHazardScore = -1

        for (obs in obstacles) {
            val isBlocking = obs.distance == DistanceBucket.VERY_NEAR || obs.distance == DistanceBucket.NEAR
            if (!isBlocking) continue

            when (obs.zone) {
                SpatialZone.FAR_LEFT, SpatialZone.LEFT -> leftBlocked = true
                SpatialZone.CENTER_LEFT, SpatialZone.CENTER, SpatialZone.CENTER_RIGHT -> centerBlocked = true
                SpatialZone.RIGHT, SpatialZone.FAR_RIGHT -> rightBlocked = true
                SpatialZone.UPPER_HAZARD, SpatialZone.LOW_GROUND_HAZARD -> centerBlocked = true
            }

            val score = calculateHazardScore(obs)
            if (score > maxHazardScore) {
                maxHazardScore = score
                highestHazard = obs
            }
        }

        val statusLevel = highestHazard?.priority ?: ObstaclePriority.INFO

        val (actionEn, actionHi, actionMr) = when {
            centerBlocked && !leftBlocked && !rightBlocked -> Triple(
                "Obstacle ahead. Safe to move 2 steps left or right.",
                "आगे रुकावट है। 2 कदम बाएँ या दाएँ जा सकते हैं।",
                "पुढे अडथळा आहे. २ पावले डावीकडे किंवा उजवीकडे वळू शकता."
            )
            centerBlocked && !leftBlocked && rightBlocked -> Triple(
                "Obstacle ahead and right. Move 2 steps left.",
                "आगे और दाएँ रुकावट है। 2 कदम बाएँ मुड़ें।",
                "पुढे आणि उजवीकडे अडथळा आहे. २ पावले डावीकडे वळा."
            )
            centerBlocked && leftBlocked && !rightBlocked -> Triple(
                "Obstacle ahead and left. Move 2 steps right.",
                "आगे और बाएँ रुकावट है। 2 कदम दाएँ मुड़ें।",
                "पुढे आणि डावीकडे अडथळा आहे. २ पावले उजवीकडे वळा."
            )
            centerBlocked && leftBlocked && rightBlocked -> Triple(
                "Path fully blocked. Please stop.",
                "रास्ता पूरी तरह से बंद है। कृपया रुकें।",
                "रस्ता पूर्णपणे बंद आहे. कृपया थांबा."
            )
            !centerBlocked && (leftBlocked || rightBlocked) -> Triple(
                "Center path is clear. Continue straight.",
                "बीच का रास्ता साफ है। सीधे चलें।",
                "मधला रस्ता मोकळा आहे. सरळ पुढे चला."
            )
            else -> Triple(
                "Path is clear ahead.",
                "आगे रास्ता साफ है।",
                "पुढे रस्ता मोकळा आहे."
            )
        }

        return WalkablePathAnalysis(
            isCenterClear = !centerBlocked,
            isLeftClear = !leftBlocked,
            isRightClear = !rightBlocked,
            dominantHazard = highestHazard,
            suggestedActionEn = actionEn,
            suggestedActionHi = actionHi,
            suggestedActionMr = actionMr,
            statusLevel = statusLevel
        )
    }

    private fun calculateHazardScore(obs: TrackedObstacle): Int {
        var score = when (obs.priority) {
            ObstaclePriority.URGENT -> 1000
            ObstaclePriority.WARNING -> 500
            ObstaclePriority.INFO -> 100
            ObstaclePriority.IGNORE -> 0
        }
        if (obs.isInWalkingCorridor) score += 300
        if (obs.isApproaching) score += 200
        if (obs.distance == DistanceBucket.VERY_NEAR) score += 200
        return score
    }

    /**
     * Determines whether to generate a spoken voice alert based on priority,
     * deduplicating repeated alerts for stationary users and giving clear step counts & directional guidance.
     */
    @Synchronized
    fun shouldAlert(
        obstacle: TrackedObstacle,
        language: AppLanguage,
        allObstacles: List<TrackedObstacle> = emptyList(),
        isUserSpeaking: Boolean = false,
        isFacingDown: Boolean = false,
        currentTimeMs: Long = System.currentTimeMillis()
    ): String? {
        // Suppress all sounds and alerts if user is speaking or phone is facing down
        if (isUserSpeaking || isFacingDown) {
            return null
        }

        // Suppress IGNORE and INFO (side objects / non-blocking / distant) from spontaneous speech
        if (obstacle.priority == ObstaclePriority.IGNORE || obstacle.priority == ObstaclePriority.INFO) {
            return null
        }

        // Don't alert for obstacles farther than 2.4 meters to prevent nuisance warnings
        if (obstacle.approximateMeters > 2.4f && obstacle.priority != ObstaclePriority.URGENT) {
            return null
        }

        // Only auto-speak if obstacle is in the walking corridor or severe hazard
        if (!obstacle.isInWalkingCorridor && obstacle.priority != ObstaclePriority.URGENT) {
            return null
        }

        // Clean up stale memory (> 10 seconds old)
        announcedTracks.entries.removeIf { (currentTimeMs - it.value.lastAnnouncedTimeMs) > 10000L }
        spatialAnnouncedMap.entries.removeIf { (currentTimeMs - it.value.lastAnnouncedTimeMs) > 10000L }

        val spatialKey = "${obstacle.type.name}_${obstacle.zone.name}"
        val existingRecord = if (obstacle.id > 0) announcedTracks[obstacle.id] else spatialAnnouncedMap[spatialKey]

        if (existingRecord != null) {
            val deltaDistance = existingRecord.lastAnnouncedDistanceMeters - obstacle.approximateMeters
            val timeSinceLastAlert = currentTimeMs - existingRecord.lastAnnouncedTimeMs

            // If the user has STOPPED at one place (distance hasn't decreased by at least 0.6m),
            // DO NOT repeat the alert!
            val userMovedCloser = deltaDistance >= 0.6f || (obstacle.priority == ObstaclePriority.URGENT && !existingRecord.hasAnnouncedUrgent)

            if (!userMovedCloser) {
                // User is stationary or obstacle is at the same distance: suppress repeated alerts
                return null
            }

            // User is continuing to walk towards the obstacle: require at least 6.5s delay before next step update
            if (timeSinceLastAlert < 6500L) {
                return null
            }

            // Update record with closer distance
            existingRecord.lastAnnouncedDistanceMeters = obstacle.approximateMeters
            existingRecord.lastAnnouncedTimeMs = currentTimeMs
            if (obstacle.priority == ObstaclePriority.URGENT) {
                existingRecord.hasAnnouncedUrgent = true
            }
        } else {
            // First time detecting this obstacle
            val newRecord = AnnouncedObstacleRecord(
                trackId = obstacle.id,
                type = obstacle.type,
                zone = obstacle.zone,
                lastAnnouncedDistanceMeters = obstacle.approximateMeters,
                lastAnnouncedTimeMs = currentTimeMs,
                hasAnnouncedUrgent = obstacle.priority == ObstaclePriority.URGENT
            )
            if (obstacle.id > 0) {
                announcedTracks[obstacle.id] = newRecord
            }
            spatialAnnouncedMap[spatialKey] = newRecord
        }

        // Global cooldown check between any two voice alerts (at least 3.0s between speech items)
        if ((currentTimeMs - lastSpokenTimeMs) < 3000L && obstacle.priority != ObstaclePriority.URGENT) {
            return null
        }

        // Generate step-based phrase with clear left/right avoidance
        val phrase = generateSpokenAlert(obstacle, language, allObstacles)
        lastSpokenMessage = phrase
        lastSpokenTimeMs = currentTimeMs
        lastSpokenPriority = obstacle.priority

        return phrase
    }

    /**
     * Generates concise, step-based guidance with directional avoidance recommendations.
     * 1 step = ~0.65m.
     */
    fun generateSpokenAlert(
        obstacle: TrackedObstacle,
        language: AppLanguage,
        allObstacles: List<TrackedObstacle> = emptyList()
    ): String {
        // Calculate steps ahead (1 step ≈ 0.65m)
        val stepsAhead = max(1, Math.round(obstacle.approximateMeters / 0.65f))

        // Determine clear detour directions from surrounding objects
        val otherBlocking = allObstacles.filter { it.id != obstacle.id && (it.distance == DistanceBucket.VERY_NEAR || it.distance == DistanceBucket.NEAR) }
        val leftBlocked = otherBlocking.any { it.zone in listOf(SpatialZone.FAR_LEFT, SpatialZone.LEFT, SpatialZone.CENTER_LEFT) }
        val rightBlocked = otherBlocking.any { it.zone in listOf(SpatialZone.FAR_RIGHT, SpatialZone.RIGHT, SpatialZone.CENTER_RIGHT) }

        val isObsLeft = obstacle.zone in listOf(SpatialZone.FAR_LEFT, SpatialZone.LEFT, SpatialZone.CENTER_LEFT)
        val isObsRight = obstacle.zone in listOf(SpatialZone.FAR_RIGHT, SpatialZone.RIGHT, SpatialZone.CENTER_RIGHT)

        // Choose best avoidance recommendation
        val (detourEn, detourHi, detourMr) = when {
            leftBlocked && rightBlocked -> Triple("Path blocked. Please stop.", "रास्ता बंद है। कृपया रुकें।", "रस्ता बंद आहे. कृपया थांबा.")
            !leftBlocked && rightBlocked -> Triple("Clear space on left, step left.", "बाईं ओर जगह है, बाएँ मुड़ें।", "डावीकडे जागा आहे, डावीकडे वळा.")
            leftBlocked && !rightBlocked -> Triple("Clear space on right, step right.", "दाईं ओर जगह है, दाएँ मुड़ें।", "उजवीकडे जागा आहे, उजवीकडे वळा.")
            isObsRight -> Triple("Clear space on left, move left.", "बाईं ओर साफ है, बाएँ चलें।", "डावीकडे मोकळे आहे, डावीकडे चला.")
            isObsLeft -> Triple("Clear space on right, move right.", "दाईं ओर साफ है, दाएँ चलें।", "उजवीकडे मोकळे आहे, उजवीकडे चला.")
            else -> Triple("Clear space on both sides, step left or right.", "दोनों ओर जगह है, बाएँ या दाएँ चलें।", "दोन्ही बाजूला जागा आहे, डावीकडे किंवा उजवीकडे वळा.")
        }

        val isUrgent = obstacle.priority == ObstaclePriority.URGENT || stepsAhead <= 1

        // 1. PERSON SPECIFIC PHRASING WITH STEPS
        if (obstacle.type == ObstacleType.PERSON) {
            val personCount = allObstacles.count { it.type == ObstacleType.PERSON }
            return when (language) {
                AppLanguage.HINDI -> {
                    when {
                        isUrgent -> "सावधान! 1 कदम पर व्यक्ति हैं, रुकें।"
                        personCount > 1 -> "सामने $stepsAhead कदम पर लोग हैं। $detourHi"
                        else -> "सामने $stepsAhead कदम पर व्यक्ति हैं। $detourHi"
                    }
                }
                AppLanguage.MARATHI -> {
                    when {
                        isUrgent -> "सावध रहा! १ पावलावर व्यक्ती आहे, थांबा."
                        personCount > 1 -> "समोर $stepsAhead पावलांवर लोक आहेत. $detourMr"
                        else -> "समोर $stepsAhead पावलांवर व्यक्ती आहे. $detourMr"
                    }
                }
                AppLanguage.ENGLISH -> {
                    when {
                        isUrgent -> "Caution! Person 1 step ahead, stop."
                        personCount > 1 -> "People $stepsAhead steps ahead. $detourEn"
                        else -> "Person $stepsAhead steps ahead. $detourEn"
                    }
                }
            }
        }

        // 2. WALL / FLAT SURFACE / LARGE BARRIER
        if (obstacle.type == ObstacleType.WALL || (obstacle.type == ObstacleType.UNKNOWN_OBSTACLE && obstacle.width * obstacle.height > 0.35f)) {
            return when (language) {
                AppLanguage.HINDI -> {
                    if (isUrgent) "सावधान! आगे 1 कदम पर दीवार या रुकावट है, रुकें।" else "आगे $stepsAhead कदम पर दीवार है। $detourHi"
                }
                AppLanguage.MARATHI -> {
                    if (isUrgent) "सावध रहा! पुढे १ पावलावर भिंत किंवा अडथळा आहे, थांबा." else "पुढे $stepsAhead पावलांवर भिंत आहे. $detourMr"
                }
                AppLanguage.ENGLISH -> {
                    if (isUrgent) "Caution! Wall 1 step ahead, please stop." else "Wall $stepsAhead steps ahead. $detourEn"
                }
            }
        }

        // 3. STAIRS / DROP-OFF
        if (obstacle.type == ObstacleType.STAIRS || obstacle.type == ObstacleType.DROP_OFF) {
            return when (language) {
                AppLanguage.HINDI -> "सावधान! आगे $stepsAhead कदम पर सीढ़ियाँ हैं। संभलकर चलें।"
                AppLanguage.MARATHI -> "सावध रहा! पुढे $stepsAhead पावलांवर पायऱ्या आहेत. काळजीपूर्वक चला."
                AppLanguage.ENGLISH -> "Warning! Stairs $stepsAhead steps ahead. Step carefully."
            }
        }

        // 4. VEHICLES & CARS & BIKES
        val isVehicle = obstacle.type in listOf(ObstacleType.VEHICLE, ObstacleType.CAR, ObstacleType.MOTORCYCLE, ObstacleType.BUS, ObstacleType.TRUCK, ObstacleType.BICYCLE)
        if (isVehicle) {
            val vNameEn = when (obstacle.type) {
                ObstacleType.CAR -> "Car"
                ObstacleType.MOTORCYCLE -> "Motorcycle"
                ObstacleType.BICYCLE -> "Bicycle"
                ObstacleType.BUS -> "Bus"
                ObstacleType.TRUCK -> "Truck"
                else -> "Vehicle"
            }
            val vNameHi = when (obstacle.type) {
                ObstacleType.CAR -> "कार"
                ObstacleType.MOTORCYCLE -> "मोटरसाइकिल"
                ObstacleType.BICYCLE -> "साइकिल"
                ObstacleType.BUS -> "बस"
                ObstacleType.TRUCK -> "ट्रक"
                else -> "गाड़ी"
            }
            val vNameMr = when (obstacle.type) {
                ObstacleType.CAR -> "कार"
                ObstacleType.MOTORCYCLE -> "मोटारसायकल"
                ObstacleType.BICYCLE -> "सायकल"
                ObstacleType.BUS -> "बस"
                ObstacleType.TRUCK -> "ट्रक"
                else -> "गाडी"
            }

            return when (language) {
                AppLanguage.HINDI -> {
                    if (obstacle.isApproaching) "सावधान! $vNameHi पास आ रही है।" else "आगे $stepsAhead कदम पर $vNameHi है। $detourHi"
                }
                AppLanguage.MARATHI -> {
                    if (obstacle.isApproaching) "सावध रहा! $vNameMr जवळ येत आहे." else "पुढे $stepsAhead पावलांवर $vNameMr आहे. $detourMr"
                }
                AppLanguage.ENGLISH -> {
                    if (obstacle.isApproaching) "Warning! $vNameEn approaching." else "$vNameEn $stepsAhead steps ahead. $detourEn"
                }
            }
        }

        // 5. ANIMALS (DOG / CAT / BIRD)
        if (obstacle.type == ObstacleType.DOG || obstacle.type == ObstacleType.CAT || obstacle.type == ObstacleType.BIRD) {
            val animalEn = when (obstacle.type) {
                ObstacleType.DOG -> "Dog"
                ObstacleType.CAT -> "Cat"
                ObstacleType.BIRD -> "Bird"
                else -> "Animal"
            }
            val animalHi = when (obstacle.type) {
                ObstacleType.DOG -> "कुत्ता"
                ObstacleType.CAT -> "बिल्ली"
                ObstacleType.BIRD -> "पक्षी"
                else -> "जानवर"
            }
            val animalMr = when (obstacle.type) {
                ObstacleType.DOG -> "कुत्रा"
                ObstacleType.CAT -> "मांजर"
                ObstacleType.BIRD -> "पक्षी"
                else -> "प्राणी"
            }
            return when (language) {
                AppLanguage.HINDI -> "आगे $stepsAhead कदम पर $animalHi है। $detourHi"
                AppLanguage.MARATHI -> "पुढे $stepsAhead पावलांवर $animalMr आहे. $detourMr"
                AppLanguage.ENGLISH -> "$animalEn $stepsAhead steps ahead. $detourEn"
            }
        }

        // 6. SPECIFIC NAMED OBJECTS (Chair, Table, Desk, Sofa, Bed, Laptop, Phone, Bag, Backpack, Box, Bottle, Door, Window, Stairs, Pole, Sign, Fence, Curb, Crosswalk)
        val typeNameEn = obstacle.type.spokenNameEn
        val typeHi = obstacle.type.spokenNameHi
        val typeMr = obstacle.type.spokenNameMr

        return when (language) {
            AppLanguage.HINDI -> {
                if (isUrgent) {
                    "सावधान! 1 कदम पर $typeHi है, रुकें।"
                } else {
                    "आगे $stepsAhead कदम पर $typeHi है। $detourHi"
                }
            }
            AppLanguage.MARATHI -> {
                if (isUrgent) {
                    "सावध रहा! १ पावलावर $typeMr आहे, थांबा."
                } else {
                    "पुढे $stepsAhead पावलांवर $typeMr आहे. $detourMr"
                }
            }
            AppLanguage.ENGLISH -> {
                if (isUrgent) {
                    "Caution! $typeNameEn 1 step ahead, stop."
                } else {
                    "$typeNameEn $stepsAhead steps ahead. $detourEn"
                }
            }
        }
    }
}

