package com.example.perception

import android.content.Context
import java.io.File

object MlKitStorageInitializer {

    fun ensureStorageReady(context: Context) {
        try {
            val accelDir = File(context.filesDir, "com.google.mlkit.acceleration")
            if (!accelDir.exists()) {
                accelDir.mkdirs()
            }

            // Create target files that ML Kit proto_data_store looks for to avoid native file-not-found errors
            val filesToTouch = listOf(
                "com.google.perception.acceleration_analytics_storage_v2.",
                "com.google.perception.acceleration_analytics_storage_v2.pb",
                "com.google.perception.acceleration_analytics_storage_v2.tmp",
                "com.google.perception.acceleration_analytics_storage_v2"
            )

            for (fileName in filesToTouch) {
                val targetFile = File(accelDir, fileName)
                if (!targetFile.exists()) {
                    targetFile.createNewFile()
                }
                targetFile.setReadable(true, false)
                targetFile.setWritable(true, false)
            }
        } catch (_: Exception) {}
    }
}
