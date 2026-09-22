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

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldPrimaryDark,
  onPrimary = EmeraldOnPrimaryDark,
  primaryContainer = EmeraldPrimaryContainerDark,
  onPrimaryContainer = EmeraldOnPrimaryContainerDark,
  secondary = GoldSecondaryDark,
  onSecondary = GoldOnSecondaryDark,
  secondaryContainer = GoldSecondaryContainerDark,
  onSecondaryContainer = GoldOnSecondaryContainerDark,
  surface = SurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurface = OnSurfaceDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  background = SurfaceDark,
  onBackground = OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimaryLight,
  onPrimary = EmeraldOnPrimaryLight,
  primaryContainer = EmeraldPrimaryContainerLight,
  onPrimaryContainer = EmeraldOnPrimaryContainerLight,
  secondary = GoldSecondaryLight,
  onSecondary = GoldOnSecondaryLight,
  secondaryContainer = GoldSecondaryContainerLight,
  onSecondaryContainer = GoldOnSecondaryContainerLight,
  surface = SurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurface = OnSurfaceLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  background = Color(0xFFF1F5F9),
  onBackground = OnSurfaceLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use intentional emerald & gold palette
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
