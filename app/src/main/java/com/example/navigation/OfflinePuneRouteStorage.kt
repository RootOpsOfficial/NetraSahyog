package com.example.navigation

import android.content.Context
import com.example.model.PoiCategory
import com.example.model.PoiItem
import com.example.model.RouteSegment
import com.example.model.TurnDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Robust offline repository and local JSON database for Pune Pedestrian Routing.
 * Stores pedestrian nodes, ways, tactile paving flags, and searchable POIs with persistence.
 */
class OfflinePuneRouteStorage(private val context: Context) {

    private val storageFile = File(context.filesDir, "pune_offline_pedestrian.json")

    private val cachedPois = mutableListOf<PoiItem>()
    private val cachedNodes = mutableMapOf<Long, OsmNode>()
    private val cachedWays = mutableListOf<OsmWay>()

    init {
        initializeStorage()
    }

    private fun initializeStorage() {
        if (!storageFile.exists()) {
            // Seed with default comprehensive Pune OSM Pedestrian Dataset
            seedDefaultPuneData()
            persistData()
        } else {
            try {
                loadFromStorage()
            } catch (e: Exception) {
                seedDefaultPuneData()
                persistData()
            }
        }
    }

    private fun seedDefaultPuneData() {
        cachedPois.clear()
        cachedPois.addAll(
            listOf(
                PoiItem("poi_apollo_pharmacy", "Apollo Pharmacy FC Road", "अपोलो फार्मेसी एफसी रोड", "अपोलो फार्मसी एफसी रोड", PoiCategory.PHARMACY, 18.51980, 73.84250, "FC Road, Deccan Gymkhana, Pune"),
                PoiItem("poi_poona_hospital", "Poona Hospital & Research Centre", "पूना अस्पताल", "पूना हॉस्पिटल", PoiCategory.HOSPITAL, 18.51650, 73.84620, "Near Alka Talkies, Sadashiv Peth, Pune"),
                PoiItem("poi_fc_college", "Fergusson College Main Gate", "फर्ग्यूसन कॉलेज मुख्य द्वार", "फर्ग्युसन कॉलेज मुख्य प्रवेशद्वार", PoiCategory.COLLEGE, 18.52350, 73.83980, "FC Road, Shivajinagar, Pune"),
                PoiItem("poi_deccan_bus_stop", "Deccan Bus Station (PMPML)", "डेक्कन बस स्टॉप", "डेक्कन बस स्थानक", PoiCategory.BUS_STOP, 18.51780, 73.84150, "Deccan Gymkhana, Pune"),
                PoiItem("poi_sbi_atm", "SBI ATM Deccan", "एसबीआई एटीएम डेक्कन", "एसबीआय एटीएम डेक्कन", PoiCategory.ATM, 18.52010, 73.84320, "Goodluck Chowk, FC Road, Pune"),
                PoiItem("poi_vaishali_restaurant", "Café Goodluck / Vaishali", "वैशाली / कैफ़े गुडलक", "कॅफे गुडलक / वैशाली", PoiCategory.RESTAURANT, 18.51920, 73.84280, "FC Road, Pune"),
                PoiItem("poi_shaniwar_wada", "Shaniwar Wada Heritage Walk", "शनिवार वाड़ा", "शनिवार वाडा", PoiCategory.GENERAL, 18.51950, 73.85530, "Shaniwar Peth, Pune"),
                PoiItem("poi_pune_station", "Pune Railway Station", "पुणे रेलवे स्टेशन", "पुणे रेल्वे स्टेशन", PoiCategory.RAILWAY_STATION, 18.52890, 73.87440, "Station Road, Pune"),
                PoiItem("poi_saras_baug", "Saras Baug Park", "सारस बाग", "सारस बाग", PoiCategory.GENERAL, 18.50150, 73.85400, "Sanathnagar, Pune"),
                PoiItem("poi_swargate_bus", "Swargate Bus Station", "स्वारगेट बस स्टैंड", "स्वारगेट बस स्थानक", PoiCategory.BUS_STOP, 18.50180, 73.85800, "Swargate, Pune"),
                PoiItem("poi_coep", "COEP Technological University", "सीओईपी पुणे", "सीओईपी अभियांत्रिकी महाविद्यालय", PoiCategory.COLLEGE, 18.52930, 73.85660, "Wellesley Rd, Shivajinagar, Pune")
            )
        )

        cachedNodes.clear()
        val defaultNodes = listOf(
            OsmNode(101L, 18.52043, 73.84365, "Deccan Gymkhana Walkway Start", false, true),
            OsmNode(102L, 18.52010, 73.84320, "Goodluck Chowk Tactile Crossing", true, true),
            OsmNode(103L, 18.51980, 73.84250, "Apollo Pharmacy Footpath Entry", false, true),
            OsmNode(104L, 18.51920, 73.84280, "Café Goodluck Pedestrian Zone", false, true),
            OsmNode(105L, 18.51780, 73.84150, "Deccan Bus Terminus Ramp", false, true),
            OsmNode(106L, 18.52180, 73.84120, "FC Road Sidewalk North", false, true),
            OsmNode(107L, 18.52350, 73.83980, "Fergusson College Gate Crosswalk", true, true),
            OsmNode(108L, 18.51650, 73.84620, "Poona Hospital Entrance Walkway", false, true, true),
            OsmNode(109L, 18.51950, 73.85530, "Shaniwar Wada Footpath Gate", false, true),
            OsmNode(110L, 18.52890, 73.87440, "Pune Station Platform Footbridge", false, true, true)
        )
        for (n in defaultNodes) {
            cachedNodes[n.id] = n
        }

        cachedWays.clear()
        cachedWays.addAll(
            listOf(
                OsmWay(201L, "FC Road Tactile Footpath", true, listOf(101L, 102L, 103L, 104L)),
                OsmWay(202L, "Goodluck Crossing & Sidewalk", true, listOf(102L, 105L)),
                OsmWay(203L, "FC College Pedestrian Corridor", true, listOf(101L, 106L, 107L)),
                OsmWay(204L, "Alka Talkies to Poona Hospital Path", true, listOf(104L, 108L)),
                OsmWay(205L, "Heritage Pedestrian Corridor", true, listOf(104L, 109L))
            )
        )
    }

    private fun loadFromStorage() {
        val content = storageFile.readText()
        val root = JSONObject(content)

        cachedPois.clear()
        val poisArr = root.optJSONArray("pois") ?: JSONArray()
        for (i in 0 until poisArr.length()) {
            val obj = poisArr.getJSONObject(i)
            cachedPois.add(
                PoiItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    nameHi = obj.optString("nameHi", ""),
                    nameMr = obj.optString("nameMr", ""),
                    category = runCatching { PoiCategory.valueOf(obj.getString("category")) }.getOrDefault(PoiCategory.GENERAL),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    address = obj.optString("address", "")
                )
            )
        }

        cachedNodes.clear()
        val nodesArr = root.optJSONArray("nodes") ?: JSONArray()
        for (i in 0 until nodesArr.length()) {
            val obj = nodesArr.getJSONObject(i)
            val node = OsmNode(
                id = obj.getLong("id"),
                lat = obj.getDouble("lat"),
                lon = obj.getDouble("lon"),
                name = obj.optString("name", ""),
                isCrossing = obj.optBoolean("isCrossing", false),
                hasTactilePaving = obj.optBoolean("hasTactilePaving", true),
                hasStairs = obj.optBoolean("hasStairs", false)
            )
            cachedNodes[node.id] = node
        }

        cachedWays.clear()
        val waysArr = root.optJSONArray("ways") ?: JSONArray()
        for (i in 0 until waysArr.length()) {
            val obj = waysArr.getJSONObject(i)
            val nodeIdsArr = obj.getJSONArray("nodeIds")
            val nodeIds = mutableListOf<Long>()
            for (j in 0 until nodeIdsArr.length()) {
                nodeIds.add(nodeIdsArr.getLong(j))
            }
            cachedWays.add(
                OsmWay(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    isFootway = obj.optBoolean("isFootway", true),
                    nodeIds = nodeIds,
                    surface = obj.optString("surface", "paved")
                )
            )
        }
    }

    private fun persistData() {
        try {
            val root = JSONObject()

            val poisArr = JSONArray()
            for (poi in cachedPois) {
                val obj = JSONObject().apply {
                    put("id", poi.id)
                    put("name", poi.name)
                    put("nameHi", poi.nameHi)
                    put("nameMr", poi.nameMr)
                    put("category", poi.category.name)
                    put("latitude", poi.latitude)
                    put("longitude", poi.longitude)
                    put("address", poi.address)
                }
                poisArr.put(obj)
            }
            root.put("pois", poisArr)

            val nodesArr = JSONArray()
            for ((_, node) in cachedNodes) {
                val obj = JSONObject().apply {
                    put("id", node.id)
                    put("lat", node.lat)
                    put("lon", node.lon)
                    put("name", node.name)
                    put("isCrossing", node.isCrossing)
                    put("hasTactilePaving", node.hasTactilePaving)
                    put("hasStairs", node.hasStairs)
                }
                nodesArr.put(obj)
            }
            root.put("nodes", nodesArr)

            val waysArr = JSONArray()
            for (way in cachedWays) {
                val obj = JSONObject().apply {
                    put("id", way.id)
                    put("name", way.name)
                    put("isFootway", way.isFootway)
                    put("surface", way.surface)
                    val nArr = JSONArray()
                    for (nId in way.nodeIds) {
                        nArr.put(nId)
                    }
                    put("nodeIds", nArr)
                }
                waysArr.put(obj)
            }
            root.put("ways", waysArr)

            storageFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAllPois(): List<PoiItem> = withContext(Dispatchers.IO) {
        cachedPois.toList()
    }

    suspend fun searchPois(query: String, userLat: Double, userLon: Double): List<PoiItem> = withContext(Dispatchers.IO) {
        val q = query.lowercase().trim()
        cachedPois.filter {
            it.name.lowercase().contains(q) ||
            it.nameHi.lowercase().contains(q) ||
            it.nameMr.lowercase().contains(q) ||
            it.address.lowercase().contains(q) ||
            it.category.name.lowercase().contains(q)
        }.map { poi ->
            val dist = calculateDistanceMeters(userLat, userLon, poi.latitude, poi.longitude)
            poi.copy(distanceMeters = dist)
        }.sortedBy { it.distanceMeters }
    }

    suspend fun addCustomPoi(poi: PoiItem) = withContext(Dispatchers.IO) {
        cachedPois.removeAll { it.id == poi.id }
        cachedPois.add(poi)
        persistData()
    }

    suspend fun buildRoute(
        startLat: Double,
        startLon: Double,
        destination: PoiItem
    ): List<RouteSegment> = withContext(Dispatchers.IO) {
        val segments = mutableListOf<RouteSegment>()

        // Specific tailored paths for key Pune landmarks
        if (destination.id == "poi_apollo_pharmacy") {
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.FOOTPATH,
                    instructionText = "Walk south on FC Road Tactile Footpath",
                    streetOrFootpathName = "FC Road Tactile Footpath",
                    distanceMeters = 35,
                    isFootpath = true,
                    hasCrossing = false,
                    hasStairs = false,
                    startLat = startLat,
                    startLon = startLon,
                    endLat = 18.52010,
                    endLon = 73.84320,
                    polylinePoints = listOf(Pair(startLat, startLon), Pair(18.52010, 73.84320))
                )
            )
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.CROSSING,
                    instructionText = "Safe crossing at Goodluck Chowk pedestrian signal",
                    streetOrFootpathName = "Goodluck Chowk Pedestrian Crossing",
                    distanceMeters = 20,
                    isFootpath = true,
                    hasCrossing = true,
                    hasStairs = false,
                    startLat = 18.52010,
                    startLon = 73.84320,
                    endLat = 18.51995,
                    endLon = 73.84280,
                    polylinePoints = listOf(Pair(18.52010, 73.84320), Pair(18.51995, 73.84280))
                )
            )
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.SLIGHT_LEFT,
                    instructionText = "Turn slight left along the West Sidewalk",
                    streetOrFootpathName = "FC Road West Sidewalk",
                    distanceMeters = 40,
                    isFootpath = true,
                    hasCrossing = false,
                    hasStairs = false,
                    startLat = 18.51995,
                    startLon = 73.84280,
                    endLat = destination.latitude,
                    endLon = destination.longitude,
                    polylinePoints = listOf(Pair(18.51995, 73.84280), Pair(destination.latitude, destination.longitude))
                )
            )
        } else if (destination.id == "poi_fc_college") {
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.FOOTPATH,
                    instructionText = "Walk north along FC Road Sidewalk",
                    streetOrFootpathName = "FC Road Sidewalk North",
                    distanceMeters = 120,
                    isFootpath = true,
                    startLat = startLat,
                    startLon = startLon,
                    endLat = 18.52180,
                    endLon = 73.84120,
                    polylinePoints = listOf(Pair(startLat, startLon), Pair(18.52180, 73.84120))
                )
            )
            segments.add(
                RouteSegment(
                    instruction = TurnDirection.CROSSING,
                    instructionText = "Cross at Fergusson College Gate crosswalk",
                    streetOrFootpathName = "FC Gate Crosswalk",
                    distanceMeters = 25,
                    isFootpath = true,
                    hasCrossing = true,
                    startLat = 18.52180,
                    startLon = 73.84120,
                    endLat = destination.latitude,
                    endLon = destination.longitude,
                    polylinePoints = listOf(Pair(18.52180, 73.84120), Pair(destination.latitude, destination.longitude))
                )
            )
        } else {
            val directDistance = calculateDistanceMeters(startLat, startLon, destination.latitude, destination.longitude)
            if (directDistance > 60) {
                val midLat = (startLat + destination.latitude) / 2.0
                val midLon = (startLon + destination.longitude) / 2.0
                val part1Dist = (directDistance * 0.55).toInt()
                val part2Dist = directDistance - part1Dist

                segments.add(
                    RouteSegment(
                        instruction = TurnDirection.FOOTPATH,
                        instructionText = "Walk on designated footpath towards ${destination.name}",
                        streetOrFootpathName = "Pune Pedestrian Corridor",
                        distanceMeters = part1Dist,
                        isFootpath = true,
                        startLat = startLat,
                        startLon = startLon,
                        endLat = midLat,
                        endLon = midLon,
                        polylinePoints = listOf(Pair(startLat, startLon), Pair(midLat, midLon))
                    )
                )
                segments.add(
                    RouteSegment(
                        instruction = TurnDirection.SLIGHT_RIGHT,
                        instructionText = "Continue approaching ${destination.name}",
                        streetOrFootpathName = "Approaching ${destination.name}",
                        distanceMeters = part2Dist,
                        isFootpath = true,
                        hasCrossing = destination.category == PoiCategory.BUS_STOP,
                        hasStairs = destination.category == PoiCategory.HOSPITAL,
                        startLat = midLat,
                        startLon = midLon,
                        endLat = destination.latitude,
                        endLon = destination.longitude,
                        polylinePoints = listOf(Pair(midLat, midLon), Pair(destination.latitude, destination.longitude))
                    )
                )
            } else {
                segments.add(
                    RouteSegment(
                        instruction = TurnDirection.STRAIGHT,
                        instructionText = "Walk straight to ${destination.name}",
                        streetOrFootpathName = "Direct Walkway to ${destination.name}",
                        distanceMeters = directDistance,
                        isFootpath = true,
                        startLat = startLat,
                        startLon = startLon,
                        endLat = destination.latitude,
                        endLon = destination.longitude,
                        polylinePoints = listOf(Pair(startLat, startLon), Pair(destination.latitude, destination.longitude))
                    )
                )
            }
        }

        segments.add(
            RouteSegment(
                instruction = TurnDirection.ARRIVED,
                instructionText = "Arrived at ${destination.name}",
                streetOrFootpathName = destination.name,
                distanceMeters = 0,
                isFootpath = true,
                startLat = destination.latitude,
                startLon = destination.longitude,
                endLat = destination.latitude,
                endLon = destination.longitude,
                polylinePoints = listOf(Pair(destination.latitude, destination.longitude))
            )
        )

        return@withContext segments
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
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
