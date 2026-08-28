package com.example.model

enum class VoiceIntentType {
    SCENE_QUERY_FORWARD,        // "What is in front of me?", "मेरे सामने क्या है?", "माझ्या समोर काय आहे?"
    SCENE_QUERY_LEFT,           // "What is on my left?"
    SCENE_QUERY_RIGHT,          // "What is on my right?"
    IS_PATH_CLEAR,              // "Is the path clear?", "क्या रास्ता साफ है?", "रस्ता मोकळा आहे का?"
    QUERY_STEPS_FORWARD,        // "How many steps can I walk?", "कितने कदम चलूं?", "किती पावले जाऊ?"
    WHAT_OBSTACLE_IS_THAT,      // "Which obstacle is that?", "सामने कौन सी रुकावट है?"
    DESCRIBE_SURROUNDINGS,      // "Describe surroundings", "आसपास क्या है?"
    READ_TEXT_OCR,              // "Read this", "यह पढ़ें", "हे वाचा"
    WHAT_IS_THIS_OBJECT,        // "What is this?", "यह क्या है?", "हे काय आहे?"
    WHAT_COLOUR_IS_THIS,        // "What colour is this?", "यह कौन सा रंग है?"
    WHERE_IS_THE_DOOR,          // "Where is the door?", "दरवाजा कहाँ है?"
    WHERE_AM_I,                 // "Where am I?", "मैं कहाँ हूँ?", "मी कुठे आहे?"
    FIND_NEAREST_POI,           // "Find nearest pharmacy", "नजदीकी अस्पताल खोजें"
    NAVIGATE_TO_POI,            // "Take me to Fergusson College", "फार्मेसी ले चलो"
    STOP_NAVIGATION,            // "Stop navigation", "नेविगेशन बंद करें"
    SWITCH_TO_GEMINI_LIVE,      // "Switch to Gemini", "जेमिनी चालू करें"
    SWITCH_TO_OFFLINE_VISION,   // "Switch to offline mode"
    REPEAT_LAST_ALERT,          // "Repeat", "दोहराएं"
    CANCEL_OR_STOP,             // "Stop", "Quiet", "रुकें"
    FREEFORM_QUESTION           // Any freeform visual question sent directly to Gemini Live
}

data class ParsedVoiceCommand(
    val intent: VoiceIntentType,
    val rawSpokenText: String,
    val detectedLanguage: AppLanguage,
    val targetPoiCategory: PoiCategory? = null,
    val searchQuery: String? = null
)

enum class AIProviderType(val displayName: String, val isOffline: Boolean) {
    OFFLINE_LOCAL("On-Device Vision & Safety Rules", true),
    GEMINI_LIVE_FLASH("Gemini 2.5 Flash Live", false),
    OPENROUTER_FALLBACK("OpenRouter Multimodal Fallback", false),
    TEAMO_FALLBACK("Teamo Router Multimodal Fallback", false)
}

enum class AudioPriority {
    CRITICAL_SAFETY,    // Collision risk, immediate drop-off, stairs (Interrupts everything)
    HIGH_SAFETY,        // Approaching hazard entering corridor
    NAVIGATION_TURN,    // Urgent maneuver required now
    USER_AI_RESPONSE,   // Gemini speaking answers to user query (Never interrupted by routine chatter)
    NAVIGATION_INFO,    // Normal upcoming turn in 100m
    ROUTINE_INFO        // Informational descriptions
}

data class TelemetryMetrics(
    val cameraFps: Int = 30,
    val inferenceLatencyMs: Long = 16L,
    val endToEndAlertLatencyMs: Long = 38L,
    val aiRequestLatencyMs: Long = 0L,
    val currentActiveProvider: AIProviderType = AIProviderType.OFFLINE_LOCAL,
    val totalObstaclesTracked: Int = 0,
    val urgentHazardsCount: Int = 0,
    val droppedFramesCount: Int = 0,
    val lastAlertMessage: String = "System Ready. Camera Active.",
    val gpsProvider: String = "Fused GPS",
    val gpsAccuracy: Float = 0f,
    val networkStatus: String = "Online"
)
