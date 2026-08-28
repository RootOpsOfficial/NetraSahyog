package com.example.perception

import android.annotation.SuppressLint
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.DistanceBucket
import com.example.model.ObstaclePriority
import com.example.model.ObstacleType
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PerceptionOutput(
    val trackedObstacles: List<TrackedObstacle> = emptyList(),
    val pathAnalysis: WalkablePathAnalysis = WalkablePathAnalysis(),
    val mostCriticalObstacle: TrackedObstacle? = null,
    val inferenceTimeMs: Long = 0L,
    val frameFps: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)

class RealtimePerceptionEngine(
    private val scope: CoroutineScope
) : ImageAnalysis.Analyzer {

    private val _perceptionFlow = MutableStateFlow(PerceptionOutput())
    val perceptionFlow: StateFlow<PerceptionOutput> = _perceptionFlow.asStateFlow()

    private val temporalTracker = TemporalTracker()
    val priorityEngine = ObstaclePriorityEngine()

    @Volatile
    var isGroundViewMode: Boolean = false

    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    private var frameCounter = 0
    private var lastFpsCalculationTimeMs = System.currentTimeMillis()
    private var currentCalculatedFps = 30
    private var isProcessing = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            try {
                imageProxy.close()
            } catch (_: Exception) {}
            return
        }

        if (isProcessing) {
            try {
                imageProxy.close()
            } catch (_: Exception) {}
            return
        }

        isProcessing = true
        val startTime = System.currentTimeMillis()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // Calculate FPS
        frameCounter++
        val now = System.currentTimeMillis()
        if (now - lastFpsCalculationTimeMs >= 1000L) {
            currentCalculatedFps = frameCounter
            frameCounter = 0
            lastFpsCalculationTimeMs = now
        }

        val width = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val height = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        val inputImage = try {
            InputImage.fromMediaImage(mediaImage, rotationDegrees)
        } catch (e: Exception) {
            isProcessing = false
            try {
                imageProxy.close()
            } catch (_: Exception) {}
            return
        }

        // ML Kit Object Detection
        try {
            objectDetector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    try {
                        val rawObstacles = mutableListOf<TrackedObstacle>()

                        for (obj in detectedObjects) {
                            val rawBox = obj.boundingBox
                            val normBox = RectF(
                                (rawBox.left.toFloat() / width).coerceIn(0f, 1f),
                                (rawBox.top.toFloat() / height).coerceIn(0f, 1f),
                                (rawBox.right.toFloat() / width).coerceIn(0f, 1f),
                                (rawBox.bottom.toFloat() / height).coerceIn(0f, 1f)
                            )

                            val centerX = normBox.centerX()
                            val centerY = normBox.centerY()
                            val zone = SpatialAnalyzer.calculateSpatialZone(centerX, centerY, isGroundViewMode)
                            val inCorridor = SpatialAnalyzer.isInsideWalkingCorridor(normBox)

                            val topLabel = obj.labels.maxByOrNull { it.confidence }
                            val labelText = topLabel?.text ?: "Obstacle"
                            val confidence = topLabel?.confidence ?: 0.75f
                            val obstacleType = SpatialAnalyzer.mapRawLabelToType(labelText, confidence)

                            val (distanceBucket, approximateMeters) = SpatialAnalyzer.estimateDistance(normBox, obstacleType)

                            val rawObstacle = TrackedObstacle(
                                id = 0,
                                type = obstacleType,
                                rawLabel = labelText,
                                confidence = confidence,
                                boundingBox = normBox,
                                centerX = centerX,
                                centerY = centerY,
                                width = normBox.width(),
                                height = normBox.height(),
                                zone = zone,
                                distance = distanceBucket,
                                estimatedMetersApprox = approximateMeters,
                                priority = ObstaclePriority.IGNORE,
                                isInWalkingCorridor = inCorridor,
                                isApproaching = false
                            )
                            rawObstacles.add(rawObstacle)
                        }

                        // Temporal tracking across frames for persistence smoothing
                        val tracked = temporalTracker.updateTracks(rawObstacles, now)

                        // Priority assignment
                        val prioritized = tracked.map { obs ->
                            obs.copy(priority = priorityEngine.evaluatePriority(obs))
                        }

                        val pathAnalysis = SpatialAnalyzer.analyzeWalkingCorridor(prioritized, isGroundViewMode)
                        val mostCritical = prioritized
                            .filter { it.priority != ObstaclePriority.IGNORE }
                            .maxByOrNull { it.priority.level * 100 + if (it.isInWalkingCorridor) 50 else 0 }

                        val inferenceDuration = System.currentTimeMillis() - startTime

                        scope.launch {
                            _perceptionFlow.value = PerceptionOutput(
                                trackedObstacles = prioritized,
                                pathAnalysis = pathAnalysis,
                                mostCriticalObstacle = mostCritical,
                                inferenceTimeMs = inferenceDuration,
                                frameFps = currentCalculatedFps,
                                timestampMs = now
                            )
                        }
                    } catch (_: Exception) {
                        // Safe fallback
                    }
                }
                .addOnCompleteListener {
                    isProcessing = false
                    try {
                        imageProxy.close()
                    } catch (_: Exception) {}
                }
        } catch (_: Exception) {
            isProcessing = false
            try {
                imageProxy.close()
            } catch (_: Exception) {}
        }
    }
}
