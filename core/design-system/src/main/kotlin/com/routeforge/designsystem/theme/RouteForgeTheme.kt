package com.routeforge.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm, earthy palette inspired by the SoundLevels app logo (cream/beige with coffee-brown
// accents) — RouteForge's own identity, distinct from any reference app's cool navy chrome.
// A manual light/dark switch is planned for a future settings screen; for now it follows the
// system setting, same as before.
private val CreamBackground = Color(0xFFF7EEE1)
private val CreamSurface = Color(0xFFFBF5EC)
private val CoffeeBrown = Color(0xFF6B4226)
private val TanContainer = Color(0xFFE3D2B0)
private val OnCream = Color(0xFF2B1D12)

private val EspressoBackground = Color(0xFF241811)
private val EspressoSurface = Color(0xFF2F2117)
private val WarmTan = Color(0xFFD9B98C)
private val BrownContainer = Color(0xFF4A3826)
private val OnEspresso = Color(0xFFF3E9DA)

private val LightColors =
    lightColorScheme(
        primary = CoffeeBrown,
        onPrimary = CreamSurface,
        secondaryContainer = TanContainer,
        onSecondaryContainer = OnCream,
        background = CreamBackground,
        onBackground = OnCream,
        surface = CreamSurface,
        onSurface = OnCream,
    )

private val DarkColors =
    darkColorScheme(
        primary = WarmTan,
        onPrimary = EspressoBackground,
        secondaryContainer = BrownContainer,
        onSecondaryContainer = OnEspresso,
        background = EspressoBackground,
        onBackground = OnEspresso,
        surface = EspressoSurface,
        onSurface = OnEspresso,
    )

@Composable
fun RouteForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
