package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ParkSpaceLightColorScheme =
  lightColorScheme(
    primary = Emerald900,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald950,
    secondary = Jade500,
    onSecondary = Color.White,
    secondaryContainer = Emerald50,
    onSecondaryContainer = Emerald900,
    tertiary = Brass600,
    onTertiary = Color.White,
    tertiaryContainer = Brass100,
    onTertiaryContainer = Brass900,
    background = Limestone50,
    onBackground = SlateInk900,
    surface = Color.White,
    onSurface = SlateInk900,
    surfaceVariant = Limestone100,
    onSurfaceVariant = SlateInk700,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Limestone50,
    surfaceContainer = Limestone100,
    surfaceContainerHigh = Limestone200,
    outline = Limestone300,
    outlineVariant = Limestone200,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF5D1111),
  )

private val ParkSpaceDarkColorScheme =
  darkColorScheme(
    primary = Jade400,
    onPrimary = Emerald950,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald50,
    secondary = Jade200,
    onSecondary = Emerald950,
    secondaryContainer = Jade900,
    onSecondaryContainer = Emerald100,
    tertiary = Brass400,
    onTertiary = Brass900,
    tertiaryContainer = Color(0xFF4F3407),
    onTertiaryContainer = Brass100,
    background = Asphalt950,
    onBackground = Mist100,
    surface = Asphalt900,
    onSurface = Mist100,
    surfaceVariant = Asphalt850,
    onSurfaceVariant = Mist300,
    surfaceContainerLowest = Asphalt950,
    surfaceContainerLow = Asphalt900,
    surfaceContainer = Asphalt850,
    surfaceContainerHigh = Asphalt800,
    outline = Asphalt700,
    outlineVariant = Asphalt800,
    error = ErrorCrimsonDark,
    onError = Asphalt950,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFCDADA),
  )

val LocalParkSpaceSemanticColors = staticCompositionLocalOf { LightSemanticColors }

@Composable
fun ParkSpaceTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> ParkSpaceDarkColorScheme
      else -> ParkSpaceLightColorScheme
    }

  val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

  CompositionLocalProvider(LocalParkSpaceSemanticColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = ParkSpaceTypography,
      shapes = ParkSpaceShapes,
      content = content,
    )
  }
}

object ParkSpaceThemeTokens {
  val semantic: ParkSpaceSemanticColors
    @Composable get() = LocalParkSpaceSemanticColors.current
}
