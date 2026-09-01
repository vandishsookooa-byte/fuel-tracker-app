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
    primary = NaturalClayDark,
    onPrimary = Color(0xFF532211),
    primaryContainer = Color(0xFF703925),
    onPrimaryContainer = Color(0xFFFFDCD1),
    secondary = NaturalTaupeLight,
    onSecondary = Color(0xFF3B2E2A),
    secondaryContainer = NaturalRoseBorderDark,
    onSecondaryContainer = NaturalPeachSoft,
    tertiary = NaturalSageLight,
    onTertiary = Color(0xFF1E3520),
    tertiaryContainer = NaturalSageContainerDark,
    onTertiaryContainer = NaturalSageLight,
    background = NaturalCoffeeDark,
    onBackground = Color(0xFFF0E0DC),
    surface = NaturalSurfaceDark,
    onSurface = Color(0xFFF0E0DC),
    surfaceVariant = Color(0xFF3B2E2A),
    onSurfaceVariant = NaturalOnSurfaceVariantDark,
    outline = NaturalRoseBorderDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalClayLight,
    onPrimary = Color.White,
    primaryContainer = NaturalPeachSoft,
    onPrimaryContainer = NaturalWalnutLight,
    secondary = NaturalTaupeGray,
    onSecondary = Color.White,
    secondaryContainer = NaturalRoseBorder,
    onSecondaryContainer = NaturalWalnutLight,
    tertiary = NaturalSageGreen,
    onTertiary = Color.White,
    tertiaryContainer = NaturalSageContainer,
    onTertiaryContainer = Color(0xFF0C1F0E),
    background = NaturalCreamVariant,
    onBackground = NaturalWalnutLight,
    surface = Color.White,
    onSurface = NaturalWalnutLight,
    surfaceVariant = Color(0xFFF5E5E0),
    onSurfaceVariant = Color(0xFF524440),
    outline = NaturalRoseBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the gorgeous Natural Tones brand essence
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
