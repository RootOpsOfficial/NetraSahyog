package com.example.perception

import android.graphics.RectF
import com.example.model.DistanceBucket
import com.example.model.ObstaclePriority
import com.example.model.ObstacleType
import com.example.model.SpatialZone
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis

object SpatialAnalyzer {

    // Virtual walking corridor horizontal boundaries (normalized 0f..1f)
    const val CORRIDOR_LEFT = 0.28f
    const val CORRIDOR_RIGHT = 0.72f
    const val CORRIDOR_TOP = 0.35f

    fun calculateSpatialZone(centerX: Float, centerY: Float, isGroundViewMode: Boolean): SpatialZone {
        if (isGroundViewMode && centerY > 0.65f) {
            return SpatialZone.LOW_GROUND_HAZARD
        }
        if (centerY < 0.18f) {
            return SpatialZone.UPPER_HAZARD
        }
        return when {
            centerX < 0.15f -> SpatialZone.FAR_LEFT
            centerX < 0.30f -> SpatialZone.LEFT
            centerX < 0.43f -> SpatialZone.CENTER_LEFT
            centerX < 0.57f -> SpatialZone.CENTER
            centerX < 0.70f -> SpatialZone.CENTER_RIGHT
            centerX < 0.85f -> SpatialZone.RIGHT
            else -> SpatialZone.FAR_RIGHT
        }
    }

    fun isInsideWalkingCorridor(box: RectF): Boolean {
        // Overlap test with virtual corridor [CORRIDOR_LEFT..CORRIDOR_RIGHT] & [CORRIDOR_TOP..1.0]
        val horizontalOverlap = box.right > CORRIDOR_LEFT && box.left < CORRIDOR_RIGHT
        val verticalOverlap = box.bottom > CORRIDOR_TOP
        return horizontalOverlap && verticalOverlap
    }

    fun estimateDistance(box: RectF, type: ObstacleType): Pair<DistanceBucket, Float> {
        val boxHeight = box.height()
        val boxWidth = box.width()
        val boxArea = boxHeight * boxWidth
        val bottomY = box.bottom

        // Conservative Proximity Estimation without fabricated exact depth decimals
        val estimatedMeters = when (type) {
            ObstacleType.WALL, ObstacleType.LARGE_OBSTRUCTION, ObstacleType.FENCE -> {
                when {
                    boxArea > 0.45f || bottomY > 0.88f -> 0.8f
                    boxArea > 0.25f -> 1.5f
                    boxArea > 0.12f -> 2.6f
                    else -> 4.5f
                }
            }
            ObstacleType.VEHICLE, ObstacleType.CAR, ObstacleType.BUS, ObstacleType.TRUCK -> {
                when {
                    boxHeight > 0.55f || boxArea > 0.30f -> 1.4f
                    boxHeight > 0.35f -> 2.8f
                    boxHeight > 0.20f -> 5.0f
                    else -> 8.0f
                }
            }
            ObstacleType.PERSON -> {
                when {
                    boxHeight > 0.65f || bottomY > 0.90f -> 0.9f
                    boxHeight > 0.45f -> 1.8f
                    boxHeight > 0.28f -> 3.0f
                    else -> 5.0f
                }
            }
            ObstacleType.MOTORCYCLE, ObstacleType.BICYCLE -> {
                when {
                    boxHeight > 0.50f || boxArea > 0.25f -> 1.2f
                    boxHeight > 0.30f -> 2.4f
                    else -> 4.5f
                }
            }
            ObstacleType.CHAIR, ObstacleType.BENCH, ObstacleType.TABLE, ObstacleType.DESK, ObstacleType.SOFA, ObstacleType.BED -> {
                when {
                    boxHeight > 0.50f || bottomY > 0.85f -> 0.8f
                    boxHeight > 0.30f -> 1.6f
                    boxHeight > 0.18f -> 2.8f
                    else -> 4.5f
                }
            }
            ObstacleType.DOOR, ObstacleType.WINDOW -> {
                when {
                    boxHeight > 0.70f -> 1.5f
                    boxHeight > 0.45f -> 3.0f
                    else -> 5.5f
                }
            }
            ObstacleType.POLE, ObstacleType.SIGN -> {
                when {
                    boxHeight > 0.60f || bottomY > 0.85f -> 0.9f
                    boxHeight > 0.35f -> 2.0f
                    else -> 4.0f
                }
            }
            ObstacleType.DOG, ObstacleType.CAT, ObstacleType.BIRD -> {
                when {
                    boxHeight > 0.40f || bottomY > 0.82f -> 0.8f
                    boxHeight > 0.22f -> 1.8f
                    else -> 3.5f
                }
            }
            ObstacleType.STAIRS, ObstacleType.STEPS, ObstacleType.DROP_OFF, ObstacleType.GROUND_HAZARD, ObstacleType.CURB -> {
                when {
                    bottomY > 0.80f -> 0.9f
                    bottomY > 0.55f -> 1.9f
                    else -> 3.5f
                }
            }
            else -> {
                when {
                    boxArea > 0.35f || (boxHeight > 0.45f && bottomY > 0.80f) -> 1.0f
                    boxArea > 0.18f -> 1.9f
                    boxArea > 0.08f -> 3.2f
                    else -> 5.0f
                }
            }
        }

        val bucket = when {
            estimatedMeters <= 1.0f -> DistanceBucket.VERY_NEAR
            estimatedMeters <= 2.2f -> DistanceBucket.NEAR
            estimatedMeters <= 3.8f -> DistanceBucket.MEDIUM
            else -> DistanceBucket.FAR
        }

        return Pair(bucket, estimatedMeters)
    }

    fun mapRawLabelToType(label: String, confidence: Float): ObstacleType {
        val lower = label.lowercase().trim()
        return when {
            lower.contains("person") || lower.contains("human") || lower.contains("pedestrian") || lower.contains("man") || lower.contains("woman") || lower.contains("child") -> ObstacleType.PERSON
            lower.contains("dog") || lower.contains("puppy") || lower.contains("canine") -> ObstacleType.DOG
            lower.contains("cat") || lower.contains("kitten") || lower.contains("feline") -> ObstacleType.CAT
            lower.contains("bird") -> ObstacleType.BIRD
            lower.contains("motorcycle") || lower.contains("motorbike") || lower.contains("scooter") || lower.contains("moped") -> ObstacleType.MOTORCYCLE
            lower.contains("bicycle") || lower.contains("bike") || lower.contains("cycle") -> ObstacleType.BICYCLE
            lower.contains("car") || lower.contains("automobile") || lower.contains("sedan") || lower.contains("suv") || lower.contains("taxi") -> ObstacleType.CAR
            lower.contains("bus") -> ObstacleType.BUS
            lower.contains("truck") || lower.contains("lorry") || lower.contains("van") -> ObstacleType.TRUCK
            lower.contains("vehicle") -> ObstacleType.VEHICLE
            lower.contains("chair") || lower.contains("seat") || lower.contains("armchair") -> ObstacleType.CHAIR
            lower.contains("couch") || lower.contains("sofa") -> ObstacleType.SOFA
            lower.contains("bench") -> ObstacleType.BENCH
            lower.contains("table") || lower.contains("dining") -> ObstacleType.TABLE
            lower.contains("desk") -> ObstacleType.DESK
            lower.contains("bed") -> ObstacleType.BED
            lower.contains("laptop") || lower.contains("computer") -> ObstacleType.LAPTOP
            lower.contains("phone") || lower.contains("mobile") || lower.contains("cell") -> ObstacleType.PHONE
            lower.contains("backpack") || lower.contains("rucksack") -> ObstacleType.BACKPACK
            lower.contains("bag") || lower.contains("handbag") || lower.contains("suitcase") || lower.contains("luggage") || lower.contains("purse") -> ObstacleType.BAG
            lower.contains("box") || lower.contains("carton") || lower.contains("package") -> ObstacleType.BOX
            lower.contains("bottle") || lower.contains("cup") || lower.contains("mug") || lower.contains("glass") -> ObstacleType.BOTTLE
            lower.contains("door") || lower.contains("doorway") || lower.contains("entry") || lower.contains("gate") || lower.contains("entrance") -> ObstacleType.DOOR
            lower.contains("window") -> ObstacleType.WINDOW
            lower.contains("stair") || lower.contains("staircase") || lower.contains("stairway") -> ObstacleType.STAIRS
            lower.contains("step") -> ObstacleType.STEPS
            lower.contains("crosswalk") || lower.contains("zebra") -> ObstacleType.CROSSWALK
            lower.contains("traffic light") || lower.contains("traffic signal") || lower.contains("signal") -> ObstacleType.TRAFFIC_LIGHT
            lower.contains("stop sign") -> ObstacleType.STOP_SIGN
            lower.contains("sign") || lower.contains("signboard") || lower.contains("billboard") || lower.contains("notice") -> ObstacleType.SIGN
            lower.contains("pole") || lower.contains("pillar") || lower.contains("post") || lower.contains("column") || lower.contains("lamp") -> ObstacleType.POLE
            lower.contains("fence") || lower.contains("railing") || lower.contains("guardrail") -> ObstacleType.FENCE
            lower.contains("wall") -> ObstacleType.WALL
            lower.contains("curb") || lower.contains("kerb") -> ObstacleType.CURB
            lower.contains("drop") || lower.contains("ledge") || lower.contains("hole") || lower.contains("pit") -> ObstacleType.DROP_OFF
            lower.contains("barrier") || lower.contains("block") -> ObstacleType.LARGE_OBSTRUCTION
            else -> ObstacleType.UNKNOWN_OBSTACLE
        }
    }

    fun analyzeWalkingCorridor(
        trackedObstacles: List<TrackedObstacle>,
        isGroundViewMode: Boolean
    ): WalkablePathAnalysis {
        val centerObstacles = trackedObstacles.filter { it.isInWalkingCorridor && it.distance != DistanceBucket.FAR }
        val leftObstacles = trackedObstacles.filter { it.zone in listOf(SpatialZone.FAR_LEFT, SpatialZone.LEFT, SpatialZone.CENTER_LEFT) && it.distance == DistanceBucket.VERY_NEAR }
        val rightObstacles = trackedObstacles.filter { it.zone in listOf(SpatialZone.CENTER_RIGHT, SpatialZone.RIGHT, SpatialZone.FAR_RIGHT) && it.distance == DistanceBucket.VERY_NEAR }

        val isCenterClear = centerObstacles.isEmpty()
        val isLeftClear = leftObstacles.isEmpty()
        val isRightClear = rightObstacles.isEmpty()

        val dominantHazard = centerObstacles.maxByOrNull { it.priority.level }

        val statusLevel = when {
            dominantHazard?.priority == ObstaclePriority.URGENT -> ObstaclePriority.URGENT
            dominantHazard?.priority == ObstaclePriority.WARNING -> ObstaclePriority.WARNING
            !isCenterClear -> ObstaclePriority.WARNING
            else -> ObstaclePriority.INFO
        }

        val (actionEn, actionHi, actionMr) = when {
            !isCenterClear && isLeftClear && !isRightClear -> Triple("Obstacle ahead. Clear space on left, move left.", "आगे रुकावट है। बाईं ओर जगह है, बाएँ मुड़ें।", "पुढे अडथळा आहे. डावीकडे मोकळी जागा आहे, डावीकडे वळा.")
            !isCenterClear && !isLeftClear && isRightClear -> Triple("Obstacle ahead. Clear space on right, move right.", "आगे रुकावट है। दाईं ओर जगह है, दाएँ मुड़ें।", "पुढे अडथळा आहे. उजवीकडे मोकळी जागा आहे, उजवीकडे वळा.")
            !isCenterClear && isLeftClear && isRightClear -> Triple("Obstacle ahead. Space available on both sides.", "आगे रुकावट है। दोनों तरफ जगह है।", "पुढे अडथळा आहे. दोन्ही बाजूंना जागा उपलब्ध आहे.")
            !isCenterClear && !isLeftClear && !isRightClear -> Triple("Path blocked ahead. Please stop.", "आगे रास्ता बंद है। कृपया रुकें।", "पुढे रस्ता बंद आहे. कृपया थांबा.")
            isGroundViewMode -> Triple("Inspecting ground steps.", "जमीन और सीढ़ियों की जांच की जा रही है।", "जमीन आणि पायऱ्यांची तपासणी चालू आहे.")
            else -> Triple("Path is clear ahead.", "रास्ता साफ है।", "रस्ता मोकळा आहे.")
        }

        return WalkablePathAnalysis(
            isCenterClear = isCenterClear,
            isLeftClear = isLeftClear,
            isRightClear = isRightClear,
            dominantHazard = dominantHazard,
            suggestedActionEn = actionEn,
            suggestedActionHi = actionHi,
            suggestedActionMr = actionMr,
            statusLevel = statusLevel,
            isGroundTiltedMode = isGroundViewMode
        )
    }
}
