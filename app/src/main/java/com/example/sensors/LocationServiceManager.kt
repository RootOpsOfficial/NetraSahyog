package com.example.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.model.RealLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationServiceManager(
    private val context: Context,
    private val onLocationUpdated: (RealLocation) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<RealLocation?>(null)
    val currentLocation: StateFlow<RealLocation?> = _currentLocation.asStateFlow()

    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                val realLoc = mapAndroidLocation(loc)
                _currentLocation.value = realLoc
                onLocationUpdated(realLoc)
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            onStatusChanged("Location permission required")
            return
        }

        if (isTracking) return

        try {
            // Get last known location first for immediate coordinate resolution
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    val realLoc = mapAndroidLocation(it)
                    _currentLocation.value = realLoc
                    onLocationUpdated(realLoc)
                }
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(1.5f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            onStatusChanged("GPS Active (Acquiring Satellites)")
        } catch (e: SecurityException) {
            onStatusChanged("Location permission error: ${e.message}")
        } catch (e: Exception) {
            onStatusChanged("Location service error: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        if (!isTracking) return
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
            onStatusChanged("GPS Paused")
        } catch (_: Exception) {}
    }

    private fun mapAndroidLocation(loc: Location): RealLocation {
        return RealLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = loc.accuracy,
            altitudeMeters = loc.altitude,
            speedMps = loc.speed,
            bearingDegrees = loc.bearing,
            timestampMs = loc.time,
            isMock = loc.isFromMockProvider
        )
    }
}
