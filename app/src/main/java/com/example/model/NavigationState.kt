package com.example.model

enum class AppLanguage(val code: String, val displayName: String, val speechLocaleTag: String) {
    ENGLISH("en", "English", "en-IN"),
    HINDI("hi", "हिंदी (Hindi)", "hi-IN"),
    MARATHI("mr", "मराठी (Marathi)", "mr-IN")
}

data class RealLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val speedMps: Float = 0f,
    val bearingDegrees: Float = 0f,
    val timestampMs: Long = System.currentTimeMillis(),
    val isMock: Boolean = false
)

data class UserSensorsContext(
    val azimuthHeadingDegrees: Float = 0f,
    val cardinalDirection: String = "N",
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isFacingDown: Boolean = false,
    val isGroundViewMode: Boolean = false,
    val isWalking: Boolean = false,
    val stepCount: Int = 0,
    val movementSpeedMps: Float = 0f,
    val location: RealLocation? = null,
    val isGpsActive: Boolean = false,
    val gpsStatusMessage: String = "Acquiring GPS..."
)

enum class NavigationStatus {
    IDLE,
    SEARCHING_PLACES,
    CALCULATING_ROUTE,
    NAVIGATING,
    APPROACHING_TURN,
    TURN_NOW,
    OFF_ROUTE_RECALCULATING,
    HAZARD_DETECTED,
    ARRIVED,
    ERROR
}

enum class TurnDirection(val spokenEn: String, val spokenHi: String, val spokenMr: String) {
    STRAIGHT("Continue straight", "सीधे चलें", "सरळ पुढे चला"),
    SLIGHT_LEFT("Turn slightly left", "हल्का बाएँ मुड़ें", "किंचित डावीकडे वळा"),
    LEFT("Turn left", "बाएँ मुड़ें", "डावीकडे वळा"),
    SLIGHT_RIGHT("Turn slightly right", "हल्का दाएँ मुड़ें", "किंचित उजवीकडे वळा"),
    RIGHT("Turn right", "दाएँ मुड़ें", "उजवीकडे वळा"),
    U_TURN("Make a U-turn", "वापस मुड़ें", "मागे वळा"),
    CROSSING("Pedestrian crossing ahead. Pay attention to traffic", "आगे सड़क पार करें। ट्रैफ़िक पर ध्यान दें", "पुढे रस्ता ओलांडा. वाहतुकीकडे लक्ष द्या"),
    STAIRS("Stairs ahead. Step carefully", "आगे सीढ़ियाँ हैं। ध्यान से चलें", "पुढे जिना आहे. काळजीपूर्वक चाला"),
    FOOTPATH("Continue along the sidewalk", "फुटपाथ पर चलें", "फुटपाथवरून चाला"),
    ARRIVED("You have arrived at your destination", "आप अपने गंतव्य पर पहुँच गए हैं", "तुम्ही तुमच्या गंतव्यस्थानी पोहोचला आहात")
}

data class RouteSegment(
    val instruction: TurnDirection,
    val instructionText: String = "",
    val streetOrFootpathName: String,
    val distanceMeters: Int,
    val durationSeconds: Int = 0,
    val isFootpath: Boolean = true,
    val hasCrossing: Boolean = false,
    val hasStairs: Boolean = false,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double,
    val polylinePoints: List<Pair<Double, Double>> = emptyList()
)

data class PoiItem(
    val id: String,
    val name: String,
    val nameHi: String = "",
    val nameMr: String = "",
    val category: PoiCategory,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val distanceMeters: Int = 0
)

enum class PoiCategory(val iconName: String, val labelEn: String, val labelHi: String, val labelMr: String) {
    PHARMACY("medication", "Pharmacy", "दवाखाना", "औषधालय"),
    HOSPITAL("local_hospital", "Hospital", "अस्पताल", "रुग्णालय"),
    BUS_STOP("directions_bus", "Bus Stop", "बस स्टॉप", "बस थांबा"),
    RAILWAY_STATION("train", "Railway Station", "रेलवे स्टेशन", "रेल्वे स्थानक"),
    ATM("atm", "ATM / Bank", "एटीएम / बैंक", "एटीएम / बँक"),
    COLLEGE("school", "College", "कॉलेज", "महाविद्यालय"),
    RESTAURANT("restaurant", "Restaurant", "रेस्तरां", "उपहारगृह"),
    CROSSING("traffic", "Pedestrian Crossing", "पैदल पार पथ", "पादचारी क्रॉसिंग"),
    GENERAL("place", "Location", "स्थान", "ठिकाण")
}

enum class NavigationProviderType(val displayName: String) {
    GOOGLE_MAPS_LIVE("Live Google Maps Navigation"),
    OFFLINE_DEMO("Offline Demo Dataset (Pune OSM)")
}

data class NavigationState(
    val status: NavigationStatus = NavigationStatus.IDLE,
    val providerType: NavigationProviderType = NavigationProviderType.GOOGLE_MAPS_LIVE,
    val currentDestination: PoiItem? = null,
    val destinationSearchQuery: String = "",
    val searchResults: List<PoiItem> = emptyList(),
    val totalRouteDistanceMeters: Int = 0,
    val remainingDistanceMeters: Int = 0,
    val currentStepIndex: Int = 0,
    val currentStep: RouteSegment? = null,
    val nextStep: RouteSegment? = null,
    val segments: List<RouteSegment> = emptyList(),
    val routePolyline: List<Pair<Double, Double>> = emptyList(),
    val isOffRoute: Boolean = false,
    val distanceFromRouteMeters: Float = 0f,
    val isCameraHazardBlocking: Boolean = false,
    val currentHazardAlert: String? = null,
    val errorMessage: String? = null
)
