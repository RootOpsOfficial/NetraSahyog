package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.ObstaclePriority
import java.util.Locale
import java.util.UUID

class VoiceAlertManager(
    context: Context,
    private val onTtsReady: () -> Unit = {},
    var onSpeakingStateChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private var currentLanguage = AppLanguage.ENGLISH
    var isMuted = false
    var isSpeaking = false
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            setLanguage(currentLanguage)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingStateChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingStateChanged(false)
                }
            })
            onTtsReady()
        } else {
            Log.e("VoiceAlertManager", "TTS initialization failed: $status")
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        if (!isReady || tts == null) return

        val locale = when (language) {
            AppLanguage.ENGLISH -> Locale("en", "IN")
            AppLanguage.HINDI -> Locale("hi", "IN")
            AppLanguage.MARATHI -> Locale("mr", "IN")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to Hindi or English if Marathi data isn't installed
            if (language == AppLanguage.MARATHI) {
                tts?.setLanguage(Locale("hi", "IN"))
            } else {
                tts?.setLanguage(Locale.US)
            }
        }
    }

    fun speak(
        text: String,
        priority: ObstaclePriority = ObstaclePriority.INFO,
        forceInterrupt: Boolean = false
    ) {
        if (isMuted || !isReady || tts == null || text.isBlank()) return

        val queueMode = if (priority == ObstaclePriority.URGENT || forceInterrupt) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        // Slightly faster speech for urgency
        if (priority == ObstaclePriority.URGENT) {
            tts?.setSpeechRate(1.15f)
            tts?.setPitch(1.05f)
        } else {
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
        }

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, queueMode, null, utteranceId)
    }

    fun stop() {
        if (isReady && tts != null) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
