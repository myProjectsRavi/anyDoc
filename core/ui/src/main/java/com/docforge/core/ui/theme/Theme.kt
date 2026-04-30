package com.docforge.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Neutral50,
    secondary = AccentBlue,
    onSecondary = Neutral50,
    tertiary = SecondaryTeal,
    background = Neutral50,
    onBackground = DarkCharcoal,
    surface = Neutral50,
    onSurface = DarkCharcoal,
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = DarkCharcoal,
    secondary = SecondaryTeal,
    onSecondary = Neutral50,
    tertiary = PrimaryBlue,
    background = DarkCharcoal,
    onBackground = Neutral50,
    surface = DarkCharcoal,
    onSurface = Neutral50,
    error = ErrorRed
)

@Composable
fun DocForgeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
