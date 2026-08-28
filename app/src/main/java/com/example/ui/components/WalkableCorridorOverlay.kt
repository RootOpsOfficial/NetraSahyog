package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.model.ObstaclePriority
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis
import com.example.perception.SpatialAnalyzer
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.DarkCoralText
import com.example.ui.theme.DeepAlertRed
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.NaturalBorder
import com.example.ui.theme.NaturalMuted
import com.example.ui.theme.NaturalSurfaceHighlight

@Composable
fun WalkableCorridorOverlay(
    obstacles: List<TrackedObstacle>,
    pathAnalysis: WalkablePathAnalysis,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw Virtual Walking Corridor Ground Trapezoid
        drawWalkingCorridor(width, height, pathAnalysis)

        // 2. Draw Bounding Boxes for Tracked Obstacles
        for (obs in obstacles) {
            val box = obs.boundingBox
            val left = box.left * width
            val top = box.top * height
            val right = box.right * width
            val bottom = box.bottom * height
            val boxWidth = right - left
            val boxHeight = bottom - top

            val boxColor = when (obs.priority) {
                ObstaclePriority.URGENT -> CoralAlert
                ObstaclePriority.WARNING -> AmberWarm
                ObstaclePriority.INFO -> LavenderPrimary
                ObstaclePriority.IGNORE -> NaturalMuted.copy(alpha = 0.5f)
            }

            // Draw bounding rect with dashed/solid stroke
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                style = Stroke(
                    width = if (obs.priority == ObstaclePriority.URGENT) 6f else 4f,
                    pathEffect = if (obs.priority == ObstaclePriority.URGENT) null else PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
            )

            // Draw corner accents for high visibility
            drawCornerAccents(left, top, right, bottom, boxColor)

            // Draw Label text badge
            val labelText = "${obs.type.displayName.uppercase()} ${"%.1f".format(obs.approximateMeters)}M"
            drawContext.canvas.nativeCanvas.apply {
                val paintBg = android.graphics.Paint().apply {
                    color = when (obs.priority) {
                        ObstaclePriority.URGENT -> 0xFFF2B8B5.toInt()
                        ObstaclePriority.WARNING -> 0xFFFFD8E4.toInt()
                        else -> 0xFFD0BCFF.toInt()
                    }
                    isAntiAlias = true
                }
                val paintText = android.graphics.Paint().apply {
                    color = when (obs.priority) {
                        ObstaclePriority.URGENT -> 0xFF601410.toInt()
                        ObstaclePriority.WARNING -> 0xFF633B48.toInt()
                        else -> 0xFF381E72.toInt()
                    }
                    textSize = 30f
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val textBounds = android.graphics.Rect()
                paintText.getTextBounds(labelText, 0, labelText.length, textBounds)
                val badgePadding = 14f
                val badgeTop = (top - 44f).coerceAtLeast(12f)
                val badgeRect = android.graphics.RectF(
                    left,
                    badgeTop,
                    left + textBounds.width() + badgePadding * 2,
                    badgeTop + 40f
                )
                drawRoundRect(badgeRect, 12f, 12f, paintBg)
                drawText(labelText, left + badgePadding, badgeTop + 28f, paintText)
            }
        }
    }
}

private fun DrawScope.drawWalkingCorridor(
    width: Float,
    height: Float,
    pathAnalysis: WalkablePathAnalysis
) {
    val corridorColor = when {
        !pathAnalysis.isCenterClear -> Color(0x33F2B8B5) // Coral alert tint
        !pathAnalysis.isLeftClear || !pathAnalysis.isRightClear -> Color(0x33FFD8E4) // Warm amber tint
        else -> Color(0x22D0BCFF) // Soft Lavender clear tint
    }

    val corridorBorderColor = when {
        !pathAnalysis.isCenterClear -> CoralAlert
        !pathAnalysis.isLeftClear || !pathAnalysis.isRightClear -> AmberWarm
        else -> LavenderPrimary.copy(alpha = 0.8f)
    }

    // Walking path perspective trapezoid
    val path = Path().apply {
        moveTo(width * 0.38f, height * 0.45f)
        lineTo(width * 0.62f, height * 0.45f)
        lineTo(width * 0.85f, height * 0.95f)
        lineTo(width * 0.15f, height * 0.95f)
        close()
    }

    drawPath(path, color = corridorColor)
    drawPath(
        path,
        color = corridorBorderColor,
        style = Stroke(
            width = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
        )
    )

    // Center guidance dotted line
    drawLine(
        color = corridorBorderColor,
        start = Offset(width * 0.5f, height * 0.45f),
        end = Offset(width * 0.5f, height * 0.95f),
        strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
    )
}

private fun DrawScope.drawCornerAccents(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color
) {
    val len = 24f
    val stroke = 6f

    // Top-Left
    drawLine(color, Offset(left, top), Offset(left + len, top), stroke)
    drawLine(color, Offset(left, top), Offset(left, top + len), stroke)

    // Top-Right
    drawLine(color, Offset(right, top), Offset(right - len, top), stroke)
    drawLine(color, Offset(right, top), Offset(right, top + len), stroke)

    // Bottom-Left
    drawLine(color, Offset(left, bottom), Offset(left + len, bottom), stroke)
    drawLine(color, Offset(left, bottom), Offset(left, bottom - len), stroke)

    // Bottom-Right
    drawLine(color, Offset(right, bottom), Offset(right - len, bottom), stroke)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - len), stroke)
}
