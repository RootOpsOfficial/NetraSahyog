package com.example.perception

import android.graphics.RectF
import com.example.model.TrackedObstacle
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class TemporalTracker {

    private var nextTrackId = 1
    private val activeTracks = mutableListOf<TrackedObstacle>()

    @Synchronized
    fun updateTracks(
        rawDetections: List<TrackedObstacle>,
        currentTimeMs: Long = System.currentTimeMillis()
    ): List<TrackedObstacle> {
        val updatedList = mutableListOf<TrackedObstacle>()
        val matchedCurrentIndices = mutableSetOf<Int>()

        for (existing in activeTracks) {
            var bestMatchIndex = -1
            var bestDistance = Float.MAX_VALUE

            for (i in rawDetections.indices) {
                if (i in matchedCurrentIndices) continue
                val candidate = rawDetections[i]

                val centerDist = hypot(
                    candidate.centerX - existing.centerX,
                    candidate.centerY - existing.centerY
                )
                val iou = calculateIoU(candidate.boundingBox, existing.boundingBox)

                if (centerDist < 0.25f || iou > 0.20f) {
                    if (centerDist < bestDistance) {
                        bestDistance = centerDist
                        bestMatchIndex = i
                    }
                }
            }

            if (bestMatchIndex != -1) {
                matchedCurrentIndices.add(bestMatchIndex)
                val matched = rawDetections[bestMatchIndex]

                val dt = max(1L, currentTimeMs - existing.lastSeenTimeMs) / 1000f
                val vx = (matched.centerX - existing.centerX) / dt
                val vy = (matched.centerY - existing.centerY) / dt

                // Approaching if bounding box area is growing or moving down in frame
                val isApproaching = (matched.height > existing.height * 1.05f) || (vy > 0.15f)

                val updatedTrack = matched.copy(
                    id = existing.id,
                    velocityX = vx,
                    velocityY = vy,
                    isApproaching = isApproaching,
                    firstSeenTimeMs = existing.firstSeenTimeMs,
                    lastSeenTimeMs = currentTimeMs,
                    frameCount = existing.frameCount + 1
                )
                updatedList.add(updatedTrack)
            }
        }

        // Add unmatched new detections as new tracks
        for (i in rawDetections.indices) {
            if (i !in matchedCurrentIndices) {
                val newDet = rawDetections[i]
                val newTrack = newDet.copy(
                    id = nextTrackId++,
                    firstSeenTimeMs = currentTimeMs,
                    lastSeenTimeMs = currentTimeMs,
                    frameCount = 1
                )
                updatedList.add(newTrack)
            }
        }

        // Clean up tracks older than 800ms
        activeTracks.clear()
        val recentTracks = updatedList.filter { (currentTimeMs - it.lastSeenTimeMs) < 800L }
        activeTracks.addAll(recentTracks)

        return recentTracks
    }

    private fun calculateIoU(a: RectF, b: RectF): Float {
        val xA = max(a.left, b.left)
        val yA = max(a.top, b.top)
        val xB = min(a.right, b.right)
        val yB = min(a.bottom, b.bottom)

        val interArea = max(0f, xB - xA) * max(0f, yB - yA)
        val boxAArea = a.width() * a.height()
        val boxBArea = b.width() * b.height()

        val unionArea = boxAArea + boxBArea - interArea
        return if (unionArea > 0f) interArea / unionArea else 0f
    }
}
