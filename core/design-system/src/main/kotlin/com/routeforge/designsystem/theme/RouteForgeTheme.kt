package com.routeforge.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm, earthy palette matching RouteForge's own logo exactly (cream/beige background, tan
// accents, dark espresso-brown pin glyph) — see docs/logo.svg and the launcher icon drawables for
// the source-of-truth hex values these are pulled from.
private val LogoCream = Color(0xFFF6ECDD)
private val LogoCreamDeep = Color(0xFFEEDFC8)
private val CreamSurface = Color(0xFFFBF5EC)
private val LogoTan = Color(0xFFDCC7A9)
private val LogoTanDeep = Color(0xFFC7AE8A)
private val LogoEspresso = Color(0xFF4E2F1B)
private val CoffeeBrown = Color(0xFF6B4226)
private val OnCream = Color(0xFF2B1D12)

private val EspressoBackground = Color(0xFF241811)
private val EspressoSurface = Color(0xFF2F2117)
private val WarmTan = Color(0xFFD9B98C)
private val BrownContainer = Color(0xFF4A3826)
private val OnEspresso = Color(0xFFF3E9DA)

// Every ColorScheme slot the app actually reads (via MaterialTheme.colorScheme.* or a Material3
// component's own defaults, e.g. NavigationBar's container) is set explicitly here — leaving any
// of them unset falls back to Material3's own baseline (cool purple/navy) palette instead of this
// one, which is exactly what previously made the bottom tab bar look mismatched.
private val LightColors =
    lightColorScheme(
        primary = LogoEspresso,
        onPrimary = LogoCream,
        primaryContainer = LogoTan,
        onPrimaryContainer = LogoEspresso,
        secondaryContainer = LogoTan,
        onSecondaryContainer = LogoEspresso,
        tertiary = LogoTanDeep,
        background = LogoCream,
        onBackground = OnCream,
        surface = CreamSurface,
        onSurface = OnCream,
        surfaceVariant = LogoCreamDeep,
        onSurfaceVariant = CoffeeBrown,
        surfaceContainer = LogoCreamDeep,
        outlineVariant = LogoTan,
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

/** [darkTheme] defaults to `false` — the app is light-mode-only for now (the logo palette above),
 *  rather than following the system setting, since [DarkColors] hasn't been updated to match it
 *  yet. Still overridable (e.g. for a future settings toggle) once that catches up. */
@Composable
fun RouteForgeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
