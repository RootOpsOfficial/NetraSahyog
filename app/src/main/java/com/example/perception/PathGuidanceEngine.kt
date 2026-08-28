package com.example.perception

import com.example.model.AppLanguage
import com.example.model.DistanceBucket
import com.example.model.ObstaclePriority
import com.example.model.PathMovementCommand
import com.example.model.SpatialZone
import com.example.model.TargetGuidanceState
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis

data class MovementGuidance(
    val command: PathMovementCommand,
    val spokenInstructionEn: String,
    val spokenInstructionHi: String,
    val spokenInstructionMr: String,
    val isBlocked: Boolean,
    val blockingObstacle: TrackedObstacle?
)

object PathGuidanceEngine {

    fun computeGuidance(
        pathAnalysis: WalkablePathAnalysis,
        obstacles: List<TrackedObstacle>,
        targetState: TargetGuidanceState?
    ): MovementGuidance {
        val criticalHazard = obstacles.filter { it.isInWalkingCorridor && it.distance == DistanceBucket.VERY_NEAR }
            .maxByOrNull { it.priority.level }

        val dominantHazard = pathAnalysis.dominantHazard

        // 1. Immediate Safety Override
        if (criticalHazard != null && criticalHazard.priority == ObstaclePriority.URGENT) {
            val obsNameEn = criticalHazard.type.spokenNameEn
            val obsNameHi = criticalHazard.type.spokenNameHi
            val obsNameMr = criticalHazard.type.spokenNameMr
            return MovementGuidance(
                command = PathMovementCommand.STOP,
                spokenInstructionEn = "Stop. $obsNameEn directly ahead.",
                spokenInstructionHi = "रुकें। सामने $obsNameHi है।",
                spokenInstructionMr = "थांबा. समोर $obsNameMr आहे.",
                isBlocked = true,
                blockingObstacle = criticalHazard
            )
        }

        // 2. Target Guidance Integration if Active
        if (targetState != null && targetState.isActive && targetState.isVisible) {
            val targetZone = targetState.zone
            val targetNameEn = targetState.targetType?.spokenNameEn ?: targetState.targetLabel
            val targetNameHi = targetState.targetType?.spokenNameHi ?: targetState.targetLabel
            val targetNameMr = targetState.targetType?.spokenNameMr ?: targetState.targetLabel

            if (targetState.isReached) {
                return MovementGuidance(
                    command = PathMovementCommand.TARGET_ARRIVED,
                    spokenInstructionEn = "You have reached the $targetNameEn.",
                    spokenInstructionHi = "आप $targetNameHi तक पहुँच गए हैं।",
                    spokenInstructionMr = "तुम्ही $targetNameMr जवळ पोहोचला आहात.",
                    isBlocked = false,
                    blockingObstacle = null
                )
            }

            // If path to target is blocked by another obstacle:
            if (!pathAnalysis.isCenterClear && dominantHazard != null && dominantHazard.type != targetState.targetType) {
                val hazardNameEn = dominantHazard.type.spokenNameEn
                val hazardNameHi = dominantHazard.type.spokenNameHi
                val hazardNameMr = dominantHazard.type.spokenNameMr

                if (pathAnalysis.isLeftClear) {
                    return MovementGuidance(
                        command = PathMovementCommand.SLIGHT_LEFT,
                        spokenInstructionEn = "$targetNameEn is ahead. $hazardNameEn is blocking the path. Move slightly left.",
                        spokenInstructionHi = "$targetNameHi आगे है। रास्ता $hazardNameHi से बंद है, थोड़ा बाएँ चलें।",
                        spokenInstructionMr = "$targetNameMr पुढे आहे. $hazardNameMr ने रस्ता अडवला आहे, थोडे डावीकडे चला.",
                        isBlocked = true,
                        blockingObstacle = dominantHazard
                    )
                } else if (pathAnalysis.isRightClear) {
                    return MovementGuidance(
                        command = PathMovementCommand.SLIGHT_RIGHT,
                        spokenInstructionEn = "$targetNameEn is ahead. $hazardNameEn is blocking the path. Move slightly right.",
                        spokenInstructionHi = "$targetNameHi आगे है। रास्ता $hazardNameHi से बंद है, थोड़ा दाएँ चलें।",
                        spokenInstructionMr = "$targetNameMr पुढे आहे. $hazardNameMr ने रस्ता अडवला आहे, थोडे उजवीकडे चला.",
                        isBlocked = true,
                        blockingObstacle = dominantHazard
                    )
                }
            }

            // Path to target is clear: guide along the target angle
            return when (targetZone) {
                SpatialZone.FAR_LEFT, SpatialZone.LEFT, SpatialZone.CENTER_LEFT -> MovementGuidance(
                    command = PathMovementCommand.SLIGHT_LEFT,
                    spokenInstructionEn = "$targetNameEn is slightly to your left. Move left.",
                    spokenInstructionHi = "$targetNameHi बाएँ है। बाएँ मुड़ें।",
                    spokenInstructionMr = "$targetNameMr डावीकडे आहे. डावीकडे वळा.",
                    isBlocked = false,
                    blockingObstacle = null
                )
                SpatialZone.FAR_RIGHT, SpatialZone.RIGHT, SpatialZone.CENTER_RIGHT -> MovementGuidance(
                    command = PathMovementCommand.SLIGHT_RIGHT,
                    spokenInstructionEn = "$targetNameEn is slightly to your right. Move right.",
                    spokenInstructionHi = "$targetNameHi दाएँ है। दाएँ मुड़ें।",
                    spokenInstructionMr = "$targetNameMr उजवीकडे आहे. उजवीकडे वळा.",
                    isBlocked = false,
                    blockingObstacle = null
                )
                else -> MovementGuidance(
                    command = PathMovementCommand.FORWARD,
                    spokenInstructionEn = "Continue straight towards $targetNameEn.",
                    spokenInstructionHi = "$targetNameHi की ओर सीधे चलें।",
                    spokenInstructionMr = "$targetNameMr च्या दिशेने सरळ पुढे चला.",
                    isBlocked = false,
                    blockingObstacle = null
                )
            }
        }

        // 3. Normal Walkable Corridor Path Guidance
        return when {
            !pathAnalysis.isCenterClear && pathAnalysis.isLeftClear && !pathAnalysis.isRightClear -> {
                val obsName = dominantHazard?.type?.spokenNameEn ?: "Obstacle"
                MovementGuidance(
                    command = PathMovementCommand.SLIGHT_LEFT,
                    spokenInstructionEn = "$obsName ahead. Move slightly left.",
                    spokenInstructionHi = "आगे रुकावट है। थोड़ा बाएँ चलें।",
                    spokenInstructionMr = "पुढे अडथळा आहे. थोडे डावीकडे चला.",
                    isBlocked = true,
                    blockingObstacle = dominantHazard
                )
            }
            !pathAnalysis.isCenterClear && !pathAnalysis.isLeftClear && pathAnalysis.isRightClear -> {
                val obsName = dominantHazard?.type?.spokenNameEn ?: "Obstacle"
                MovementGuidance(
                    command = PathMovementCommand.SLIGHT_RIGHT,
                    spokenInstructionEn = "$obsName ahead. Move slightly right.",
                    spokenInstructionHi = "आगे रुकावट है। थोड़ा दाएँ चलें।",
                    spokenInstructionMr = "पुढे अडथळा आहे. थोडे उजवीकडे चला.",
                    isBlocked = true,
                    blockingObstacle = dominantHazard
                )
            }
            !pathAnalysis.isCenterClear && !pathAnalysis.isLeftClear && !pathAnalysis.isRightClear -> {
                MovementGuidance(
                    command = PathMovementCommand.STOP,
                    spokenInstructionEn = "Path is blocked ahead. Please stop.",
                    spokenInstructionHi = "आगे रास्ता बंद है। कृपया रुकें।",
                    spokenInstructionMr = "पुढे रस्ता बंद आहे. कृपया थांबा.",
                    isBlocked = true,
                    blockingObstacle = dominantHazard
                )
            }
            else -> {
                MovementGuidance(
                    command = PathMovementCommand.FORWARD,
                    spokenInstructionEn = "Path is clear ahead. Continue straight.",
                    spokenInstructionHi = "आगे रास्ता साफ है। सीधे चलें।",
                    spokenInstructionMr = "पुढे रस्ता मोकळा आहे. सरळ पुढे चला.",
                    isBlocked = false,
                    blockingObstacle = null
                )
            }
        }
    }
}
