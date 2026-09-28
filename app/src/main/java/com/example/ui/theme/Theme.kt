package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonRose,
    onPrimary = Color.White,
    primaryContainer = RoseDark,
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = NeonAmber,
    onSecondary = Color.Black,
    secondaryContainer = AmberDark,
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = ElectricCyan,
    onTertiary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderGlowRose
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
