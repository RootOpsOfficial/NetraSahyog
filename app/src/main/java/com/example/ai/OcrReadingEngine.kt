package com.example.ai

import android.graphics.Bitmap
import com.example.model.AppLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OcrReadingEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap, language: AppLanguage): String {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    if (text.isBlank()) {
                        val emptyMsg = when (language) {
                            AppLanguage.HINDI -> "कोई टेक्स्ट नहीं मिला"
                            AppLanguage.MARATHI -> "कोणताही मजकूर आढळला नाही"
                            AppLanguage.ENGLISH -> "No readable text detected in front of camera"
                        }
                        continuation.resume(emptyMsg)
                    } else {
                        val header = when (language) {
                            AppLanguage.HINDI -> "पढ़ा गया टेक्स्ट: "
                            AppLanguage.MARATHI -> "वाचलेला मजकूर: "
                            AppLanguage.ENGLISH -> "Detected text: "
                        }
                        // Clean up text lines for speech
                        val cleaned = text.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                        continuation.resume("$header $cleaned")
                    }
                }
                .addOnFailureListener { e ->
                    val errMsg = when (language) {
                        AppLanguage.HINDI -> "टेक्स्ट पढ़ने में त्रुटि"
                        AppLanguage.MARATHI -> "मजकूर वाचण्यात त्रुटी"
                        AppLanguage.ENGLISH -> "Text recognition failed: ${e.localizedMessage}"
                    }
                    continuation.resume(errMsg)
                }
        }
    }
}
