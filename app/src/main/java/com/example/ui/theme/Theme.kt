package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GothicIndustrialColorScheme = darkColorScheme(
  primary = AntiqueBrass,
  onPrimary = Color.White,
  primaryContainer = FoundryIron,
  onPrimaryContainer = BrassGlow,
  secondary = AlchemicalEmerald,
  onSecondary = Color.Black,
  tertiary = SteamCyan,
  onTertiary = Color.Black,
  background = IndustrialDark,
  onBackground = Color(0xFFF1F5F9),
  surface = SlateSurface,
  onSurface = Color(0xFFE2E8F0),
  error = SlagCrimson,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = GothicIndustrialColorScheme,
    typography = Typography,
    content = content
  )
}
