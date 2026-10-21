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
  primary = ForestGreenDarkPrimary,
  onPrimary = ForestGreenDark,
  primaryContainer = ForestGreenLight,
  onPrimaryContainer = Color.White,
  secondary = SageSecondaryDark,
  onSecondary = ForestGreenDark,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = Color.White,
  tertiary = TerracottaAccentDark,
  onTertiary = ForestGreenDark,
  tertiaryContainer = Color(0xFF5A2212),
  onTertiaryContainer = Color.White,
  background = DarkBackground,
  onBackground = Color(0xFFEDEFEA),
  surface = DarkSurface,
  onSurface = Color(0xFFEDEFEA),
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = Color(0xFFC7CEC8),
  outline = OutlineWarm
)

private val LightColorScheme = lightColorScheme(
  primary = ForestGreenPrimary,
  onPrimary = Color.White,
  primaryContainer = SageContainer,
  onPrimaryContainer = OnSageContainer,
  secondary = SageSecondary,
  onSecondary = Color.White,
  secondaryContainer = SurfaceVariantWarm,
  onSecondaryContainer = ForestGreenDark,
  tertiary = TerracottaAccent,
  onTertiary = Color.White,
  tertiaryContainer = TerracottaContainer,
  onTertiaryContainer = OnTerracottaContainer,
  background = WarmCreamBackground,
  onBackground = Color(0xFF191D1A),
  surface = PureWhiteSurface,
  onSurface = Color(0xFF191D1A),
  surfaceVariant = SurfaceVariantWarm,
  onSurfaceVariant = Color(0xFF434944),
  outline = OutlineWarm,
  outlineVariant = OutlineVariantWarm
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent custom branding
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
