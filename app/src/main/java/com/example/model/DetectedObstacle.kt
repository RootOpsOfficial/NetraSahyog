package com.example.model

import android.graphics.RectF

/**
 * 9-Zone Spatial Matrix for fine-grained walking corridor and peripheral spatial awareness.
 */
enum class SpatialZone(
    val label: String,
    val spokenDescriptionEn: String,
    val spokenDescriptionHi: String,
    val spokenDescriptionMr: String
) {
    FAR_LEFT("Far Left", "on your far left", "आपके बहुत बाईं ओर", "तुमच्या खूप डावीकडे"),
    LEFT("Left", "on your left", "आपके बाईं ओर", "तुमच्या डावीकडे"),
    CENTER_LEFT("Center Left", "ahead, slightly left", "आगे थोड़ा बाईं ओर", "पुढे थोडे डावीकडे"),
    CENTER("Center", "directly ahead", "बिल्कुल आगे", "थेट समोर"),
    CENTER_RIGHT("Center Right", "ahead, slightly right", "आगे थोड़ा दाईं ओर", "पुढे थोडे उजवीकडे"),
    RIGHT("Right", "on your right", "आपके दाईं ओर", "तुमच्या उजवीकडे"),
    FAR_RIGHT("Far Right", "on your far right", "आपके बहुत दाईं ओर", "तुमच्या खूप उजवीकडे"),
    UPPER_HAZARD("Overhead", "overhead hazard", "ऊपर सिर के पास", "डोक्याजवळ अडथळा"),
    LOW_GROUND_HAZARD("Ground", "on the ground", "जमीन पर", "जमिनीवर")
}

/**
 * Conservative Proximity Buckets - avoiding false-precision decimals without depth sensors.
 */
enum class DistanceBucket(
    val label: String,
    val approximateSteps: Int,
    val spokenEn: String,
    val spokenHi: String,
    val spokenMr: String
) {
    VERY_NEAR("Very Close (<1m)", 1, "very close", "बहुत पास", "खूप जवळ"),
    NEAR("Close (1-2m)", 2, "about 2 steps ahead", "लगभग 2 कदम आगे", "सुमारे २ पावले पुढे"),
    MEDIUM("Medium (2-3.5m)", 4, "a few steps ahead", "कुछ कदम आगे", "काही पावले पुढे"),
    FAR("Far (>3.5m)", 6, "farther ahead", "दूर आगे", "दूर पुढे"),
    UNKNOWN("Unknown", 0, "ahead", "आगे", "पुढे")
}

enum class ObstaclePriority(val level: Int) {
    IGNORE(0),
    INFO(1),
    WARNING(2),
    URGENT(3)
}

enum class ObstacleType(
    val displayName: String,
    val spokenNameEn: String,
    val spokenNameHi: String,
    val spokenNameMr: String,
    val isSevereHazard: Boolean
) {
    PERSON("Person", "Person", "व्यक्ति", "व्यक्ती", false),
    DOG("Dog", "Dog", "कुत्ता", "कुत्रा", false),
    CAT("Cat", "Cat", "बिल्ली", "मांजर", false),
    BIRD("Bird", "Bird", "पक्षी", "पक्षी", false),
    CAR("Car", "Car", "कार", "कार", true),
    BUS("Bus", "Bus", "बस", "बस", true),
    TRUCK("Truck", "Truck", "ट्रक", "ट्रक", true),
    MOTORCYCLE("Motorcycle", "Motorcycle", "मोटरसाइकिल", "मोटारसायकल", true),
    BICYCLE("Bicycle", "Bicycle", "साइकिल", "सायकल", true),
    VEHICLE("Vehicle", "Vehicle", "वाहन", "वाहन", true),
    CHAIR("Chair", "Chair", "कुर्सी", "खुर्ची", false),
    TABLE("Table", "Table", "मेज़", "टेबल", false),
    DESK("Desk", "Desk", "डेस्क", "डेस्क", false),
    SOFA("Sofa", "Sofa", "सोफा", "सोफा", false),
    BED("Bed", "Bed", "बिस्तर", "बेड", false),
    LAPTOP("Laptop", "Laptop", "लैपटॉप", "लॅपटॉप", false),
    PHONE("Phone", "Mobile phone", "फ़ोन", "फोन", false),
    BAG("Bag", "Bag", "बैग", "बॅग", false),
    BACKPACK("Backpack", "Backpack", "बैग", "बॅग", false),
    BOX("Box", "Box", "डिब्बा", "खोके", false),
    BOTTLE("Bottle", "Bottle", "बोतल", "बाटली", false),
    DOOR("Door", "Door", "दरवाजा", "दरवाजा", false),
    WINDOW("Window", "Window", "खिड़की", "खिडकी", false),
    STAIRS("Stairs", "Stairs", "सीढ़ियां", "पायऱ्या", true),
    STEPS("Steps", "Steps", "कदम / सीढ़ियां", "पायऱ्या", true),
    POLE("Pole", "Pole", "खंभा", "खांब", true),
    SIGN("Sign", "Signboard", "साइनबोर्ड", "पाटी", false),
    FENCE("Fence", "Fence / Railing", "बाड़ / रेलिंग", "कुंपण", true),
    WALL("Wall", "Wall", "दीवार", "भिंत", true),
    CURB("Curb", "Curb", "सड़क का किनारा", "फूटपाथची कड", true),
    CROSSWALK("Crosswalk", "Pedestrian crosswalk", "पैदल पार पथ", "झेब्रा क्रॉसिंग", false),
    TRAFFIC_LIGHT("Traffic Light", "Traffic light", "ट्रैफ़िक लाइट", "ट्रॅफिक सिग्नल", false),
    STOP_SIGN("Stop Sign", "Stop sign", "स्टॉप साइन", "स्टॉप पाटी", false),
    BENCH("Bench", "Bench", "बेंच", "बाकडा", false),
    DROP_OFF("Drop-off", "Drop-off", "ढलान / किनारा", "उतार", true),
    GROUND_HAZARD("Ground Obstacle", "Ground obstacle", "जमीन पर रुकावट", "जमिनीवरील अडथळा", true),
    LARGE_OBSTRUCTION("Large Obstruction", "Large obstruction", "बड़ा अवरोध", "मोठा अडथळा", true),
    UNKNOWN_OBSTACLE("Obstacle", "Obstacle", "रुकावट", "अडथळा", true)
}

data class TargetGuidanceState(
    val isActive: Boolean = false,
    val targetQuery: String = "",
    val targetType: ObstacleType? = null,
    val targetLabel: String = "",
    val confidence: Float = 0f,
    val zone: SpatialZone = SpatialZone.CENTER,
    val approximateSteps: Int = 0,
    val approximateMeters: Float = 0f,
    val isVisible: Boolean = false,
    val lastSeenTimeMs: Long = 0L,
    val startStepCount: Int = 0,
    val remainingSteps: Int = 0,
    val guidanceInstructionEn: String = "",
    val guidanceInstructionHi: String = "",
    val guidanceInstructionMr: String = "",
    val isReached: Boolean = false
)

enum class PathMovementCommand(
    val spokenEn: String,
    val spokenHi: String,
    val spokenMr: String
) {
    FORWARD("Continue straight", "सीधे चलें", "सरळ पुढे चला"),
    SLIGHT_LEFT("Move slightly left", "थोड़ा बाएँ मुड़ें", "थोडे डावीकडे वळा"),
    SLIGHT_RIGHT("Move slightly right", "थोड़ा दाएँ मुड़ें", "थोडे उजवीकडे वळा"),
    STOP("Stop immediately", "तुरंत रुकें", "लगेच थांबा"),
    SLOW("Walk slowly, hazard nearby", "धीरे चलें, आगे खतरा है", "हळू चाला, पुढे अडथळा आहे"),
    TURN_LEFT("Turn left", "बाएँ मुड़ें", "डावीकडे वळा"),
    TURN_RIGHT("Turn right", "दाएँ मुड़ें", "उजवीकडे वळा"),
    TARGET_ARRIVED("You have reached the target", "आप लक्ष्य तक पहुँच गए हैं", "तुम्ही लक्ष्याजवळ पोहोचला आहात"),
    TARGET_LOST("Target is not currently visible", "लक्ष्य अभी दिखाई नहीं दे रहा है", "लक्ष्य सध्या दिसत नाही आहे")
}

data class TrackedObstacle(
    val id: Int,
    val type: ObstacleType,
    val rawLabel: String,
    val confidence: Float,
    val boundingBox: RectF, // normalized coordinates 0f..1f
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val zone: SpatialZone,
    val distance: DistanceBucket,
    val estimatedMetersApprox: Float,
    val priority: ObstaclePriority,
    val isInWalkingCorridor: Boolean,
    val isApproaching: Boolean,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val firstSeenTimeMs: Long = System.currentTimeMillis(),
    val lastSeenTimeMs: Long = System.currentTimeMillis(),
    val frameCount: Int = 1
) {
    val approximateMeters: Float get() = estimatedMetersApprox
}

data class WalkablePathAnalysis(
    val isCenterClear: Boolean = true,
    val isLeftClear: Boolean = true,
    val isRightClear: Boolean = true,
    val dominantHazard: TrackedObstacle? = null,
    val suggestedActionEn: String = "Path clear.",
    val suggestedActionHi: String = "रास्ता साफ है।",
    val suggestedActionMr: String = "रस्ता मोकळा आहे.",
    val statusLevel: ObstaclePriority = ObstaclePriority.INFO,
    val isGroundTiltedMode: Boolean = false
)
