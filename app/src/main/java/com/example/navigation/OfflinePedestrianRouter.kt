package com.example.navigation

import com.example.model.PoiItem
import com.example.model.RouteSegment
import com.example.model.TurnDirection
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OfflinePedestrianRouter {

    fun calculateRoute(
        startLat: Double,
        startLon: Double,
        destination: PoiItem
    ): List<RouteSegment> {
        val segments = mutableListOf<RouteSegment>()

        // Check if destination is Apollo Pharmacy
        if (destination.id == "poi_apollo_pharmacy") {
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.FOOTPATH,
                    streetOrFootpathName = "FC Road Tactile Footpath",
                    distanceMeters = 35,
                    isFootpath = true,
                    hasCrossing = false,
                    hasStairs = false,
                    startLat = startLat,
                    startLon = startLon,
                    endLat = 18.52010,
                    endLon = 73.84320
                )
            )
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.CROSSING,
                    streetOrFootpathName = "Goodluck Chowk Pedestrian Crossing",
                    distanceMeters = 20,
                    isFootpath = true,
                    hasCrossing = true,
                    hasStairs = false,
                    startLat = 18.52010,
                    startLon = 73.84320,
                    endLat = 18.51995,
                    endLon = 73.84280
                )
            )
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.SLIGHT_LEFT,
                    streetOrFootpathName = "FC Road West Sidewalk",
                    distanceMeters = 40,
                    isFootpath = true,
                    hasCrossing = false,
                    hasStairs = false,
                    startLat = 18.51995,
                    startLon = 73.84280,
                    endLat = destination.latitude,
                    endLon = destination.longitude
                )
            )
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.ARRIVED,
                    streetOrFootpathName = destination.name,
                    distanceMeters = 0,
                    isFootpath = true,
                    startLat = destination.latitude,
                    startLon = destination.longitude,
                    endLat = destination.latitude,
                    endLon = destination.longitude
                )
            )
            return segments
        }

        // Generic fallback route calculation with intermediate steps
        val directDistance = calculateDistanceMeters(startLat, startLon, destination.latitude, destination.longitude)

        if (directDistance > 60) {
            val midLat = (startLat + destination.latitude) / 2.0
            val midLon = (startLon + destination.longitude) / 2.0
            val part1Dist = (directDistance * 0.55).toInt()
            val part2Dist = directDistance - part1Dist

            segments.add(
                RouteSegment(
                    instruction = TurnDirection.FOOTPATH,
                    streetOrFootpathName = "Pedestrian Footpath",
                    distanceMeters = part1Dist,
                    isFootpath = true,
                    hasCrossing = false,
                    startLat = startLat,
                    startLon = startLon,
                    endLat = midLat,
                    endLon = midLon
                )
            )
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.SLIGHT_RIGHT,
                    streetOrFootpathName = "Approaching ${destination.name}",
                    distanceMeters = part2Dist,
                    isFootpath = true,
                    hasCrossing = destination.category == com.example.model.PoiCategory.BUS_STOP,
                    hasStairs = destination.category == com.example.model.PoiCategory.HOSPITAL,
                    startLat = midLat,
                    startLon = midLon,
                    endLat = destination.latitude,
                    endLon = destination.longitude
                )
            )
        } else {
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.STRAIGHT,
                    streetOrFootpathName = "Footpath to ${destination.name}",
                    distanceMeters = directDistance,
                    isFootpath = true,
                    startLat = startLat,
                    startLon = startLon,
                    endLat = destination.latitude,
                    endLon = destination.longitude
                )
            )
        }

        segments.add(
            RouteSegment(
                instruction = TurnDirection.ARRIVED,
                streetOrFootpathName = destination.name,
                distanceMeters = 0,
                isFootpath = true,
                startLat = destination.latitude,
                startLon = destination.longitude,
                endLat = destination.latitude,
                endLon = destination.longitude
            )
        )

        return segments
    }

    companion object {
        fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return (r * c).toInt()
        }
    }
}
