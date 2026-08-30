package com.example.ai

import android.graphics.Bitmap
import com.example.model.AIProviderType
import com.example.model.AppLanguage
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIProviderManager {

    private val geminiAssistant = GeminiVisionAssistant()
    private val ocrEngine = OcrReadingEngine()

    private val _currentProvider = MutableStateFlow(AIProviderType.GEMINI_LIVE_FLASH)
    val currentProvider: StateFlow<AIProviderType> = _currentProvider.asStateFlow()

    fun setProvider(provider: AIProviderType) {
        _currentProvider.value = provider
    }

    suspend fun queryVisionScene(
        bitmap: Bitmap?,
        prompt: String,
        language: AppLanguage,
        obstacles: List<TrackedObstacle>,
        pathAnalysis: WalkablePathAnalysis
    ): String {
        return geminiAssistant.queryMultimodalVision(bitmap, prompt, language, obstacles, pathAnalysis)
    }

    suspend fun readOcrText(bitmap: Bitmap?, language: AppLanguage): String {
        if (bitmap == null) {
            return when (language) {
                AppLanguage.HINDI -> "कैमरा इमेज उपलब्ध नहीं है"
                AppLanguage.MARATHI -> "कॅमेरा प्रतिमा उपलब्ध नाही"
                AppLanguage.ENGLISH -> "Camera image not ready for reading"
            }
        }
        return ocrEngine.recognizeTextFromBitmap(bitmap, language)
    }
}
