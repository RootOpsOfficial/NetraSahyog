package com.example.navigation

import com.example.model.PoiCategory
import com.example.model.PoiItem

data class OsmNode(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String = "",
    val isCrossing: Boolean = false,
    val hasTactilePaving: Boolean = true,
    val hasStairs: Boolean = false
)

data class OsmWay(
    val id: Long,
    val name: String,
    val isFootway: Boolean = true,
    val nodeIds: List<Long>,
    val surface: String = "paved"
)

object PuneOsmDataset {

    // Center anchor: FC Road / Deccan Gymkhana, Pune (18.52043, 73.84365)
    val PUNE_POIS = listOf(
        PoiItem(
            id = "poi_apollo_pharmacy",
            name = "Apollo Pharmacy FC Road",
            nameHi = "अपोलो फार्मेसी एफसी रोड",
            nameMr = "अपोलो फार्मसी एफसी रोड",
            category = PoiCategory.PHARMACY,
            latitude = 18.51980,
            longitude = 73.84250,
            address = "FC Road, Deccan Gymkhana, Pune"
        ),
        PoiItem(
            id = "poi_poona_hospital",
            name = "Poona Hospital & Research Centre",
            nameHi = "पूना अस्पताल",
            nameMr = "पूना हॉस्पिटल",
            category = PoiCategory.HOSPITAL,
            latitude = 18.51650,
            longitude = 73.84620,
            address = "Near Alka Talkies, Sadashiv Peth, Pune"
        ),
        PoiItem(
            id = "poi_fc_college",
            name = "Fergusson College Main Gate",
            nameHi = "फर्ग्यूसन कॉलेज मुख्य द्वार",
            nameMr = "फर्ग्युसन कॉलेज मुख्य प्रवेशद्वार",
            category = PoiCategory.COLLEGE,
            latitude = 18.52350,
            longitude = 73.83980,
            address = "FC Road, Shivajinagar, Pune"
        ),
        PoiItem(
            id = "poi_deccan_bus_stop",
            name = "Deccan Bus Station (PMPML)",
            nameHi = "डेक्कन बस स्टॉप",
            nameMr = "डेक्कन बस स्थानक",
            category = PoiCategory.BUS_STOP,
            latitude = 18.51780,
            longitude = 73.84150,
            address = "Deccan Gymkhana, Pune"
        ),
        PoiItem(
            id = "poi_sbi_atm",
            name = "SBI ATM Deccan",
            nameHi = "एसबीआई एटीएम डेक्कन",
            nameMr = "एसबीआय एटीएम डेक्कन",
            category = PoiCategory.ATM,
            latitude = 18.52010,
            longitude = 73.84320,
            address = "Goodluck Chowk, FC Road, Pune"
        ),
        PoiItem(
            id = "poi_vaishali_restaurant",
            name = "Café Goodluck / Vaishali",
            nameHi = "वैशाली / कैफ़े गुडलक",
            nameMr = "कॅफे गुडलक / वैशाली",
            category = PoiCategory.RESTAURANT,
            latitude = 18.51920,
            longitude = 73.84280,
            address = "FC Road, Pune"
        ),
        PoiItem(
            id = "poi_pune_station",
            name = "Pune Railway Station",
            nameHi = "पुणे रेलवे स्टेशन",
            nameMr = "पुणे रेल्वे स्टेशन",
            category = PoiCategory.RAILWAY_STATION,
            latitude = 18.52890,
            longitude = 73.87440,
            address = "Station Road, Pune"
        )
    )

    // Key Pedestrian Nodes in FC Road / Deccan Walkway Network
    val NODES = listOf(
        OsmNode(101L, 18.52043, 73.84365, "Deccan Gymkhana Walkway Start", false, true),
        OsmNode(102L, 18.52010, 73.84320, "Goodluck Chowk Tactile Crossing", true, true),
        OsmNode(103L, 18.51980, 73.84250, "Apollo Pharmacy Footpath Entry", false, true),
        OsmNode(104L, 18.51920, 73.84280, "Café Goodluck Pedestrian Zone", false, true),
        OsmNode(105L, 18.51780, 73.84150, "Deccan Bus Terminus Ramp", false, true),
        OsmNode(106L, 18.52180, 73.84120, "FC Road Sidewalk North", false, true),
        OsmNode(107L, 18.52350, 73.83980, "Fergusson College Gate Crosswalk", true, true),
        OsmNode(108L, 18.51650, 73.84620, "Poona Hospital Entrance Walkway", false, true, true)
    )

    val WAYS = listOf(
        OsmWay(201L, "FC Road Tactile Footpath", true, listOf(101L, 102L, 103L, 104L)),
        OsmWay(202L, "Goodluck Crossing & Sidewalk", true, listOf(102L, 105L)),
        OsmWay(203L, "FC College Pedestrian Corridor", true, listOf(101L, 106L, 107L)),
        OsmWay(204L, "Alka Talkies to Poona Hospital Path", true, listOf(104L, 108L))
    )
}
