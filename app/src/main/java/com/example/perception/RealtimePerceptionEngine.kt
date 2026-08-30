package com.example.perception

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.DistanceBucket
import com.example.model.ObstaclePriority
import com.example.model.ObstacleType
import com.example.model.SpatialZone
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PerceptionOutput(
    val trackedObstacles: List<TrackedObstacle> = emptyList(),
    val pathAnalysis: WalkablePathAnalysis = WalkablePathAnalysis(),
    val mostCriticalObstacle: TrackedObstacle? = null,
    val detectedSceneLabels: List<String> = emptyList(),
    val inferenceTimeMs: Long = 0L,
    val frameFps: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)

class RealtimePerceptionEngine(
    private val scope: CoroutineScope,
    var onFrameBitmapAvailable: ((Bitmap) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val _perceptionFlow = MutableStateFlow(PerceptionOutput())
    val perceptionFlow: StateFlow<PerceptionOutput> = _perceptionFlow.asStateFlow()

    private val temporalTracker = TemporalTracker()
    val priorityEngine = ObstaclePriorityEngine()

    @Volatile
    var isGroundViewMode: Boolean = false

    @Volatile
    var latestBitmap: Bitmap? = null
        private set

    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    private val imageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.50f)
            .build()
        ImageLabeling.getClient(options)
    }

    private var frameCounter = 0
    private var lastFpsCalculationTimeMs = System.currentTimeMillis()
    private var currentCalculatedFps = 30
    private var lastBitmapCaptureTimeMs = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val startTime = System.currentTimeMillis()
        try {
            val mediaImage = imageProxy.image ?: return
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            val width = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
            val height = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

            // Periodically extract true camera bitmap (every 300ms) for real-time Gemini vision & OCR
            val now = System.currentTimeMillis()
            if (now - lastBitmapCaptureTimeMs >= 300L) {
                try {
                    val bmp = imageProxy.toBitmap()
                    latestBitmap = bmp
                    onFrameBitmapAvailable?.invoke(bmp)
                    lastBitmapCaptureTimeMs = now
                } catch (_: Exception) {}
            }

            // Calculate FPS
            frameCounter++
            if (now - lastFpsCalculationTimeMs >= 1000L) {
                currentCalculatedFps = frameCounter
                frameCounter = 0
                lastFpsCalculationTimeMs = now
            }

            // Run ML Kit Object Detection & Image Labeling in parallel
            val objectTask = objectDetector.process(inputImage)
            val labelTask = imageLabeler.process(inputImage)

            val detectedObjects = Tasks.await(objectTask)
            val detectedLabels = try {
                Tasks.await(labelTask)
            } catch (_: Exception) {
                emptyList()
            }

            val sceneLabelStrings = detectedLabels.map { it.text }

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

                val topObjLabel = obj.labels.maxByOrNull { it.confidence }
                var labelText = topObjLabel?.text ?: ""
                var confidence = topObjLabel?.confidence ?: 0.65f

                // If object label is generic or empty, check detected scene labels
                if (labelText.isBlank() || labelText.equals("Home good", ignoreCase = true) || labelText.equals("Fashion good", ignoreCase = true) || labelText.equals("Place", ignoreCase = true)) {
                    val bestSceneLabel = detectedLabels.firstOrNull { it.confidence > 0.60f }
                    if (bestSceneLabel != null) {
                        labelText = bestSceneLabel.text
                        confidence = bestSceneLabel.confidence
                    }
                }

                val obstacleType = SpatialAnalyzer.mapRawLabelToType(labelText, confidence, normBox)

                // Only create obstacle if it's not completely negligible
                if (normBox.width() > 0.05f && normBox.height() > 0.05f) {
                    val (distanceBucket, approximateMeters) = SpatialAnalyzer.estimateDistance(normBox, obstacleType)

                    val rawObstacle = TrackedObstacle(
                        id = 0,
                        type = obstacleType,
                        rawLabel = if (labelText.isNotBlank()) labelText else obstacleType.displayName,
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

            _perceptionFlow.value = PerceptionOutput(
                trackedObstacles = prioritized,
                pathAnalysis = pathAnalysis,
                mostCriticalObstacle = mostCritical,
                detectedSceneLabels = sceneLabelStrings,
                inferenceTimeMs = inferenceDuration,
                frameFps = currentCalculatedFps,
                timestampMs = now
            )
        } catch (_: Exception) {
            // Silently handle frame skip/cancel
        } finally {
            try {
                imageProxy.close()
            } catch (_: Exception) {}
        }
    }

    fun close() {
        try {
            objectDetector.close()
            imageLabeler.close()
        } catch (_: Exception) {}
    }
}
