package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.model.AppLanguage
import com.example.model.ParsedVoiceCommand
import com.example.model.PoiCategory
import com.example.model.VoiceIntentType
import java.util.Locale

class VoiceRecognitionEngine(
    private val context: Context,
    private val onCommandParsed: (ParsedVoiceCommand) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onPartialTranscript: (String) -> Unit = {},
    private val onRmsUpdated: (Float) -> Unit = {},
    private val onSpeechError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isContinuousMode = false
    private var currentLanguage = AppLanguage.ENGLISH
    private var isTemporarilyPaused = false

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun setContinuousMode(enabled: Boolean, language: AppLanguage = currentLanguage) {
        isContinuousMode = enabled
        currentLanguage = language
        if (enabled) {
            isTemporarilyPaused = false
            if (!isListening) {
                startListening(language)
            }
        } else {
            stopListening()
        }
    }

    fun pauseForSpeaking() {
        isTemporarilyPaused = true
        stopListening()
    }

    fun resumeAfterSpeaking() {
        isTemporarilyPaused = false
        if (isContinuousMode && !isListening) {
            mainHandler.postDelayed({
                if (isContinuousMode && !isTemporarilyPaused && !isListening) {
                    startListening(currentLanguage)
                }
            }, 350)
        }
    }

    fun startListening(language: AppLanguage = AppLanguage.ENGLISH) {
        currentLanguage = language
        if (isListening || isTemporarilyPaused) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError("Speech recognition not available on device")
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    onRmsUpdated(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                    isListening = false
                    onListeningStateChanged(false)
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
                        else -> "Speech error $error"
                    }
                    if (!isContinuousMode) {
                        onSpeechError(errorMsg)
                    } else {
                        scheduleContinuousRestart()
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    onListeningStateChanged(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull() ?: ""
                    if (spokenText.isNotBlank()) {
                        onPartialTranscript(spokenText)
                        val parsed = parseVoiceCommand(spokenText, language)
                        onCommandParsed(parsed)
                    }
                    if (isContinuousMode) {
                        scheduleContinuousRestart()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val currentText = partials?.firstOrNull() ?: ""
                    if (currentText.isNotBlank()) {
                        onPartialTranscript(currentText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechLocaleTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onListeningStateChanged(false)
            if (isContinuousMode) {
                scheduleContinuousRestart()
            } else {
                onSpeechError(e.message ?: "Could not start speech listening")
            }
        }
    }

    private fun scheduleContinuousRestart() {
        if (!isContinuousMode || isTemporarilyPaused) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isContinuousMode && !isTemporarilyPaused && !isListening) {
                startListening(currentLanguage)
            }
        }, 400)
    }

    fun stopListening() {
        mainHandler.removeCallbacksAndMessages(null)
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            isListening = false
            onListeningStateChanged(false)
        }
    }

    fun destroy() {
        isContinuousMode = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }

    fun parseVoiceCommand(text: String, currentLanguage: AppLanguage): ParsedVoiceCommand {
        val lower = text.lowercase(Locale.ROOT).trim()

        // 1. Step clearance query ("How many steps should I go ahead?", "can I walk", "कितने कदम आगे जाऊँ", "पावले")
        if (lower.contains("how many step") || lower.contains("how many steps") || lower.contains("कितने कदम") || lower.contains("किती पावले") ||
            lower.contains("can i walk") || lower.contains("how far can i go")) {
            return ParsedVoiceCommand(VoiceIntentType.QUERY_STEPS_FORWARD, text, currentLanguage)
        }

        // 2. Obstacle identity query ("What obstacle is that?", "which obstacle", "रुकावट क्या है", "अडथळा कोणता")
        if (lower.contains("which obstacle") || lower.contains("what obstacle") || lower.contains("what is blocking") ||
            lower.contains("रुकावट क्या") || lower.contains("बाधा क्या") || lower.contains("अडथळा कोणता")) {
            return ParsedVoiceCommand(VoiceIntentType.WHAT_OBSTACLE_IS_THAT, text, currentLanguage)
        }

        // 3. Color detection ("What colour is this?", "रंग क्या है", "रंग कोणता")
        if (lower.contains("colour") || lower.contains("color") || lower.contains("रंग") || lower.contains("colour of")) {
            return ParsedVoiceCommand(VoiceIntentType.WHAT_COLOUR_IS_THIS, text, currentLanguage)
        }

        // 4. Door detection ("Where is the door?", "दरवाजा कहाँ है")
        if (lower.contains("door") || lower.contains("entrance") || lower.contains("gate") || lower.contains("दरवाजा") || lower.contains("दार")) {
            return ParsedVoiceCommand(VoiceIntentType.WHERE_IS_THE_DOOR, text, currentLanguage)
        }

        // 5. Read OCR ("Read this", "यह पढ़ें", "बोर्ड वाचा")
        if (lower.contains("read") || lower.contains("text") || lower.contains("sign") ||
            lower.contains("पढ़ो") || lower.contains("पढ़ें") || lower.contains("वाचा") ||
            lower.contains("बोर्ड") || lower.contains("पाटी")) {
            return ParsedVoiceCommand(VoiceIntentType.READ_TEXT_OCR, text, currentLanguage)
        }

        // 6. Scene Query Forward ("What is in front of me?", "what do you see")
        if (lower.contains("front") || lower.contains("ahead") || lower.contains("सामने") ||
            lower.contains("पुढे") || lower.contains("समोर") || lower.contains("what do you see") ||
            lower.contains("क्या दिख रहा") || lower.contains("काय दिसत आहे") || lower.contains("what am i looking at")) {
            return ParsedVoiceCommand(VoiceIntentType.SCENE_QUERY_FORWARD, text, currentLanguage)
        }

        // 7. Scene Query Left
        if (lower.contains("on my left") || lower.contains("to my left") || lower.contains("बाईं ओर क्या") || lower.contains("डावीकडे काय")) {
            return ParsedVoiceCommand(VoiceIntentType.SCENE_QUERY_LEFT, text, currentLanguage)
        }

        // 8. Scene Query Right
        if (lower.contains("on my right") || lower.contains("to my right") || lower.contains("दाईं ओर क्या") || lower.contains("उजवीकडे काय")) {
            return ParsedVoiceCommand(VoiceIntentType.SCENE_QUERY_RIGHT, text, currentLanguage)
        }

        // 9. Path Clear check
        if (lower.contains("is path clear") || lower.contains("is it clear") || lower.contains("क्या रास्ता साफ है") || lower.contains("रस्ता मोकळा आहे का")) {
            return ParsedVoiceCommand(VoiceIntentType.IS_PATH_CLEAR, text, currentLanguage)
        }

        // 10. Describe Surroundings
        if (lower.contains("describe") || lower.contains("surrounding") || lower.contains("describe room") ||
            lower.contains("आसपास") || lower.contains("वर्णन") || lower.contains("परिसर")) {
            return ParsedVoiceCommand(VoiceIntentType.DESCRIBE_SURROUNDINGS, text, currentLanguage)
        }

        // 11. Where am I
        if (lower.contains("where am i") || lower.contains("current location") || lower.contains("कहाँ हूँ") || lower.contains("कुठे आहे")) {
            return ParsedVoiceCommand(VoiceIntentType.WHERE_AM_I, text, currentLanguage)
        }

        // 12. Navigation to POI / Location
        if (lower.contains("navigate to") || lower.contains("take me to") || lower.contains("directions to") ||
            lower.contains("ले चलो") || lower.contains("रास्ता दिखाओ") || lower.contains("मार्ग दाखवा") ||
            lower.contains("जाना है") || lower.contains("जायचे आहे")) {

            val targetCategory = when {
                lower.contains("pharmacy") || lower.contains("medical") || lower.contains("medicine") || lower.contains("दवा") || lower.contains("औषध") -> PoiCategory.PHARMACY
                lower.contains("hospital") || lower.contains("clinic") || lower.contains("doctor") || lower.contains("अस्पताल") || lower.contains("रुग्णालय") -> PoiCategory.HOSPITAL
                lower.contains("bus") || lower.contains("stop") || lower.contains("बस") || lower.contains("थांबा") -> PoiCategory.BUS_STOP
                lower.contains("station") || lower.contains("train") || lower.contains("रेलवे") || lower.contains("स्थानक") -> PoiCategory.RAILWAY_STATION
                lower.contains("atm") || lower.contains("bank") || lower.contains("बैंक") || lower.contains("पैसे") -> PoiCategory.ATM
                lower.contains("college") || lower.contains("school") || lower.contains("university") || lower.contains("कॉलेज") || lower.contains("शाळा") -> PoiCategory.COLLEGE
                lower.contains("food") || lower.contains("hotel") || lower.contains("restaurant") || lower.contains("खाना") || lower.contains("हॉटेल") -> PoiCategory.RESTAURANT
                else -> PoiCategory.GENERAL
            }

            // Extract target name (e.g., "take me to Fergusson College" -> "Fergusson College")
            var destinationQuery = text
            val triggerWords = listOf("navigate to", "take me to", "directions to", "route to", "ले चलो", "रास्ता दिखाओ", "मार्ग दाखवा")
            for (word in triggerWords) {
                if (lower.contains(word)) {
                    val index = lower.indexOf(word) + word.length
                    destinationQuery = text.substring(index).trim()
                    break
                }
            }

            return ParsedVoiceCommand(
                intent = VoiceIntentType.NAVIGATE_TO_POI,
                rawSpokenText = text,
                detectedLanguage = currentLanguage,
                targetPoiCategory = targetCategory,
                searchQuery = if (destinationQuery.isNotBlank()) destinationQuery else text
            )
        }

        // 13. Stop / Cancel
        if (lower.contains("stop") || lower.contains("quiet") || lower.contains("cancel") || lower.contains("रुकें") ||
            lower.contains("चुप") || lower.contains("थांबवा") || lower.contains("शांत")) {
            return ParsedVoiceCommand(VoiceIntentType.CANCEL_OR_STOP, text, currentLanguage)
        }

        // 14. Switch mode
        if (lower.contains("switch to gemini") || lower.contains("open gemini") || lower.contains("जेमिनी चालू")) {
            return ParsedVoiceCommand(VoiceIntentType.SWITCH_TO_GEMINI_LIVE, text, currentLanguage)
        }
        if (lower.contains("switch to offline") || lower.contains("offline vision")) {
            return ParsedVoiceCommand(VoiceIntentType.SWITCH_TO_OFFLINE_VISION, text, currentLanguage)
        }

        // Default to conversational freeform question handled by Gemini AI
        return ParsedVoiceCommand(VoiceIntentType.FREEFORM_QUESTION, text, currentLanguage)
    }
}
