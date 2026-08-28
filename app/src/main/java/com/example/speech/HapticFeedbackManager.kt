package com.example.speech

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.ObstaclePriority
import com.example.model.SpatialZone

class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    } catch (_: Exception) {
        null
    }

    private var lastVibrationTimeMs = 0L

    /**
     * Distinct single audio chime / ring confirming voice listening has started
     */
    fun playListeningStartChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 130)
        } catch (_: Exception) {}
    }

    /**
     * Soft chime confirming speech received
     */
    fun playListeningStopChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 90)
        } catch (_: Exception) {}
    }

    /**
     * Controlled intelligent vibration for hazards:
     * - URGENT (<1m in corridor / collision risk): Discrete double-tap pulse.
     * - WARNING (1-2m in corridor): Short single gentle tap.
     * - INFO / SAFE: 0 vibration.
     * - Enforces cooldown to prevent sensor noise buzzing.
     */
    fun vibrateForHazard(
        priority: ObstaclePriority,
        zone: SpatialZone,
        isFacingDown: Boolean = false,
        currentTimeMs: Long = System.currentTimeMillis()
    ) {
        if (isFacingDown) return
        if (vibrator == null || !vibrator.hasVibrator()) return

        val minInterval = if (priority == ObstaclePriority.URGENT) 800L else 1800L
        if ((currentTimeMs - lastVibrationTimeMs) < minInterval) return

        when (priority) {
            ObstaclePriority.URGENT -> {
                lastVibrationTimeMs = currentTimeMs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 130, 70, 140)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 130, 70, 140), -1)
                }
            }
            ObstaclePriority.WARNING -> {
                lastVibrationTimeMs = currentTimeMs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(100L, 220)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100L)
                }
            }
            else -> {
                // No vibration for INFO or IGNORE
            }
        }
    }

    fun vibrateForNavigationTurn(isLeft: Boolean, isFacingDown: Boolean = false) {
        if (isFacingDown) return
        if (vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = if (isLeft) longArrayOf(0, 100, 60, 100) else longArrayOf(0, 140, 70, 140)
            val amplitudes = if (isLeft) intArrayOf(0, 220, 0, 220) else intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(140L)
        }
    }

    fun vibrateSuccess() {
        if (vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 70, 40, 70, 40, 100)
            val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120L)
        }
    }
}
