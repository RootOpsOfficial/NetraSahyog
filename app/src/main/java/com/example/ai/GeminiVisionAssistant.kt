package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.TrackedObstacle
import com.example.model.WalkablePathAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiVisionAssistant {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Primary & Fallback API keys provided for multimodal intelligence
    private val geminiKeys = listOf(
        BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" },
        "AQ.Ab8RN6JvCHwALBZAG_R0VIkGD4X-i2hv0ttu9zeYcoX2nrrxbg",
        "AQ.Ab8RN6KziBrVR9NWuoa-0fb_ZS5OArBT3kvWt9FPHre5jBLE6g"
    ).filterNotNull().filter { it.isNotBlank() }

    private val openRouterKey = "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"
    private val teamoKeys = listOf(
        "sk-teamo-98abeda42277dfc872fdef4c3eb03e3d5d848e399dc8f64d",
        "sk-teamo-f9edf12d938f88acebfacaa7e0dced2bfb9876dc3d1bdc71",
        "sk-teamo-30c31a35902943a39deb3398ce3eb89d23ddb9f7a9119254"
    )

    suspend fun queryMultimodalVision(
        bitmap: Bitmap?,
        userPrompt: String,
        language: AppLanguage,
        localObstacles: List<TrackedObstacle>,
        pathAnalysis: WalkablePathAnalysis
    ): String = withContext(Dispatchers.IO) {
        val antiHallucinationSystemPrompt = when (language) {
            AppLanguage.HINDI -> """
                आप NETRA AI के वास्तविक समय के दृश्य सहायक हैं।
                दी गई लाइव कैमरा छवि को ध्यान से देखें और उपयोगकर्ता के प्रश्न का सीधा, सटीक उत्तर 1-2 वाक्यों में दें।
                दरवाजे, मेज, कुर्सी, दीवार, व्यक्ति, वस्तु या रंग को सही पहचानें।
                यदि कोई रुकावट नहीं है तो रुकावट का झूठा दावा न करें।
                केवल वही बताएं जो कैमरे में वास्तव में दिख रहा है।
            """.trimIndent()
            AppLanguage.MARATHI -> """
                तुम्ही NETRA AI चे रिअल-टाइम व्हिजन असिस्टंट आहात.
                दिलेल्या थेट कॅमेरा चित्राकडे काळजीपूर्वक पहा आणि वापरकर्त्याच्या प्रश्नाचे थेट, अचूक उत्तर १-२ वाक्यात द्या.
                दार, टेबल, खुर्ची, भिंत, व्यक्ती, वस्तू किंवा रंग अचूक ओळखा.
                काल्पनिक किंवा चुकीचा अडथळा सांगू नका.
                केवळ कॅमेरा चित्रात जे प्रत्यक्ष दिसत आहे तेच सांगा.
            """.trimIndent()
            AppLanguage.ENGLISH -> """
                You are NETRA AI's real-time camera assistant.
                Carefully look at the provided live camera image.
                Answer the user's question directly and accurately in 1-2 conversational sentences.
                Correctly identify what is actually in the image: doors as doors, tables as tables, chairs as chairs, walls as walls, people as people, objects and colors as they appear.
                Do NOT say "obstacle ahead" or inject hazard warnings unless there is a genuine physical blockage.
                Never make up objects or guess. Speak naturally about what is actually in view.
            """.trimIndent()
        }

        val promptText = """
            $antiHallucinationSystemPrompt
            User Question: "$userPrompt"
        """.trimIndent()

        // Prepare Base64 JPEG if bitmap is provided
        val base64Image = bitmap?.let { bmp ->
            try {
                val outputStream = ByteArrayOutputStream()
                val targetW = 480
                val targetH = (480f * bmp.height / bmp.width).toInt().coerceAtLeast(100)
                val scaled = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            } catch (_: Exception) {
                null
            }
        }

        // TIER 1: Try Gemini endpoints with provided Gemini API keys
        for (gKey in geminiKeys) {
            val res = callGeminiNative(gKey, promptText, base64Image)
            if (res != null && res.isNotBlank()) {
                return@withContext res.trim()
            }
        }

        // TIER 2: Try OpenRouter with Multimodal
        val orResult = callOpenRouterVision(promptText, base64Image)
        if (orResult != null && orResult.isNotBlank()) {
            return@withContext orResult.trim()
        }

        // TIER 3: Try Teamo Router
        for (tKey in teamoKeys) {
            val teamoResult = callOpenAiCompatibleVision("https://api.teamorouter.com/v1/chat/completions", tKey, "google/gemini-2.5-flash", promptText, base64Image)
            if (teamoResult != null && teamoResult.isNotBlank()) {
                return@withContext teamoResult.trim()
            }
        }

        // TIER 4: Local High-Precision Fallback Reasoning (Offline)
        return@withContext generateLocalSceneDescription(userPrompt, language, localObstacles, pathAnalysis)
    }

    private fun callGeminiNative(apiKey: String, prompt: String, base64Image: String?): String? {
        val models = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        for (model in models) {
            try {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply { put("text", prompt) })

                if (base64Image != null) {
                    val inlineData = JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Image)
                    }
                    partsArray.put(JSONObject().apply { put("inline_data", inlineData) })
                }

                val requestBodyJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply { put("parts", partsArray) })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("maxOutputTokens", 120)
                        put("temperature", 0.2)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: return@use
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                if (text.isNotBlank()) return text
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GeminiVisionAssistant", "Gemini call error on $model: ${e.message}")
            }
        }
        return null
    }

    private fun callOpenRouterVision(prompt: String, base64Image: String?): String? {
        return callOpenAiCompatibleVision("https://openrouter.ai/api/v1/chat/completions", openRouterKey, "google/gemini-2.5-flash", prompt, base64Image)
    }

    private fun callOpenAiCompatibleVision(url: String, apiKey: String, model: String, prompt: String, base64Image: String?): String? {
        try {
            val contentArray = JSONArray()
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })

            if (base64Image != null) {
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    })
                })
                put("max_tokens", 120)
                put("temperature", 0.2)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use
                    val json = JSONObject(responseBody)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val message = choices.getJSONObject(0).optJSONObject("message")
                        val content = message?.optString("content")
                        if (!content.isNullOrBlank()) return content
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Local Semantic Fallback when offline or no API response available.
     */
    private fun generateLocalSceneDescription(
        prompt: String,
        language: AppLanguage,
        obstacles: List<TrackedObstacle>,
        pathAnalysis: WalkablePathAnalysis
    ): String {
        val lower = prompt.lowercase()
        val visibleObstacles = obstacles.filter { it.confidence > 0.5f }

        // Specific question handling
        if (lower.contains("how many people") || lower.contains("how many person") || lower.contains("कितने लोग") || lower.contains("किती व्यक्ती")) {
            val count = visibleObstacles.count { it.type == com.example.model.ObstacleType.PERSON }
            return when (language) {
                AppLanguage.HINDI -> if (count == 0) "सामने कोई व्यक्ति नहीं दिख रहा है।" else "सामने $count व्यक्ति दिख रहे हैं।"
                AppLanguage.MARATHI -> if (count == 0) "समोर कोणीही व्यक्ती दिसत नाही." else "समोर $count व्यक्ती दिसत आहेत."
                AppLanguage.ENGLISH -> if (count == 0) "No people detected in front of you." else "I can see $count person(s) in the scene."
            }
        }

        if (lower.contains("door") || lower.contains("दरवाजा") || lower.contains("दार")) {
            val door = visibleObstacles.firstOrNull { it.type == com.example.model.ObstacleType.DOOR }
            return if (door != null) {
                when (language) {
                    AppLanguage.HINDI -> "दरवाजा ${door.zone.spokenDescriptionHi} है।"
                    AppLanguage.MARATHI -> "दार ${door.zone.spokenDescriptionMr} आहे."
                    AppLanguage.ENGLISH -> "I detect a doorway ${door.zone.spokenDescriptionEn}."
                }
            } else {
                when (language) {
                    AppLanguage.HINDI -> "वर्तमान कैमरे के दृश्य में दरवाजा स्पष्ट रूप से नहीं दिख रहा है।"
                    AppLanguage.MARATHI -> "कॅमेरा दृश्यात दार स्पष्टपणे दिसत नाही."
                    AppLanguage.ENGLISH -> "No door is clearly visible in the current camera view."
                }
            }
        }

        if (lower.contains("chair") || lower.contains("कुर्सी") || lower.contains("खुर्ची")) {
            val chair = visibleObstacles.firstOrNull { it.type == com.example.model.ObstacleType.CHAIR || it.type == com.example.model.ObstacleType.BENCH }
            return if (chair != null) {
                when (language) {
                    AppLanguage.HINDI -> "कुर्सी ${chair.zone.spokenDescriptionHi} है।"
                    AppLanguage.MARATHI -> "खुर्ची ${chair.zone.spokenDescriptionMr} आहे."
                    AppLanguage.ENGLISH -> "There is a chair ${chair.zone.spokenDescriptionEn}."
                }
            } else {
                when (language) {
                    AppLanguage.HINDI -> "सामने कोई कुर्सी नहीं दिख रही है।"
                    AppLanguage.MARATHI -> "समोर कोणतीही खुर्ची दिसत नाही."
                    AppLanguage.ENGLISH -> "No chair is visible directly in front of you."
                }
            }
        }

        if (lower.contains("step") || lower.contains("कदम") || lower.contains("पावले")) {
            return when (language) {
                AppLanguage.HINDI -> if (pathAnalysis.isCenterClear) "आगे का रास्ता 6 से 8 कदम साफ है।" else pathAnalysis.suggestedActionHi
                AppLanguage.MARATHI -> if (pathAnalysis.isCenterClear) "पुढील रस्ता ६ ते ८ पावले मोकळा आहे." else pathAnalysis.suggestedActionMr
                AppLanguage.ENGLISH -> if (pathAnalysis.isCenterClear) "The forward path is clear for at least 6 to 8 steps." else pathAnalysis.suggestedActionEn
            }
        }

        if (visibleObstacles.isEmpty()) {
            return when (language) {
                AppLanguage.HINDI -> "सामने का क्षेत्र खुला दिख रहा है। कोई बड़ी रुकावट नहीं है।"
                AppLanguage.MARATHI -> "समोरील परिसर मोकळा दिसत आहे. कोणताही मोठा अडथळा नाही."
                AppLanguage.ENGLISH -> "The area ahead appears open with no immediate obstructions detected."
            }
        }

        val primary = visibleObstacles.first()
        val typeNameEn = primary.type.displayName
        return when (language) {
            AppLanguage.HINDI -> "सामने ${primary.zone.spokenDescriptionHi} $typeNameEn दिख रहा है।"
            AppLanguage.MARATHI -> "समोर ${primary.zone.spokenDescriptionMr} $typeNameEn दिसत आहे."
            AppLanguage.ENGLISH -> "I see a $typeNameEn ${primary.zone.spokenDescriptionEn}."
        }
    }
}
