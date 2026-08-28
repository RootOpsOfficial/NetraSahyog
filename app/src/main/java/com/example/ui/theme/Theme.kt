package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = DeepVioletOnPrimary,
    primaryContainer = VioletContainer,
    onPrimaryContainer = LavenderPrimary,
    secondary = LavenderPrimary,
    onSecondary = DeepVioletOnPrimary,
    secondaryContainer = NaturalMuted,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarning,
    error = CoralAlert,
    onError = DarkCoralText,
    errorContainer = DeepAlertRed,
    onErrorContainer = CoralAlert,
    background = NaturalBackground,
    onBackground = TextPrimary,
    surface = NaturalSurface,
    onSurface = TextPrimary,
    surfaceVariant = NaturalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = NaturalBorder,
    outlineVariant = NaturalMuted
  )

private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color disabled to maintain the curated Natural Tones palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
