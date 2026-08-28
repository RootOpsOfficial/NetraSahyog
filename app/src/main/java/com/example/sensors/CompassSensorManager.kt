package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.UserSensorsContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _sensorState = MutableStateFlow(UserSensorsContext())
    val sensorState: StateFlow<UserSensorsContext> = _sensorState.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var smoothedAzimuth = 0f
    private var stepCount = 0

    private var lastAccelMagnitude = 9.8f
    private var stepThresholdMs = 0L

    fun start() {
        rotationVectorSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometerSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepDetectorSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val azimuthRad = orientationAngles[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f

                // Smooth compass jitter using exponential moving average
                smoothedAzimuth = smoothedAzimuth * 0.8f + azimuthDeg * 0.2f
                val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                // Extreme tilt / in pocket / flat face down
                val isFacingDown = abs(pitchDeg) > 75f || abs(rollDeg) > 145f

                // Ground View Mode: User intentionally tilting phone downward to inspect stairs, curbs, drop-offs
                val isGroundViewMode = !isFacingDown && (pitchDeg in -70f..-30f || pitchDeg in 30f..70f)

                val cardinal = getCardinalDirection(smoothedAzimuth)

                _sensorState.value = _sensorState.value.copy(
                    azimuthHeadingDegrees = smoothedAzimuth,
                    cardinalDirection = cardinal,
                    pitchDegrees = pitchDeg,
                    rollDegrees = rollDeg,
                    isFacingDown = isFacingDown,
                    isGroundViewMode = isGroundViewMode
                )
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                stepCount++
                _sensorState.value = _sensorState.value.copy(
                    stepCount = stepCount,
                    isWalking = true
                )
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = abs(magnitude - lastAccelMagnitude)
                lastAccelMagnitude = magnitude

                val now = System.currentTimeMillis()
                if (delta > 2.8f && (now - stepThresholdMs) > 350L) {
                    stepThresholdMs = now
                    stepCount++
                    _sensorState.value = _sensorState.value.copy(
                        stepCount = stepCount,
                        isWalking = true,
                        movementSpeedMps = 1.1f
                    )
                } else if (now - stepThresholdMs > 2500L) {
                    _sensorState.value = _sensorState.value.copy(
                        isWalking = false,
                        movementSpeedMps = 0f
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun setLocationContext(location: com.example.model.RealLocation) {
        _sensorState.value = _sensorState.value.copy(
            location = location,
            isGpsActive = true,
            gpsStatusMessage = "GPS Active (±${location.accuracyMeters.toInt()}m)"
        )
    }

    fun setGpsUnavailable(reason: String) {
        _sensorState.value = _sensorState.value.copy(
            isGpsActive = false,
            gpsStatusMessage = reason
        )
    }

    private fun getCardinalDirection(deg: Float): String {
        return when {
            deg >= 337.5 || deg < 22.5 -> "North"
            deg >= 22.5 && deg < 67.5 -> "North-East"
            deg >= 67.5 && deg < 112.5 -> "East"
            deg >= 112.5 && deg < 157.5 -> "South-East"
            deg >= 157.5 && deg < 202.5 -> "South"
            deg >= 202.5 && deg < 247.5 -> "South-West"
            deg >= 247.5 && deg < 292.5 -> "West"
            else -> "North-West"
        }
    }
}
