package com.routeforge.designsystem.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

private const val HALO_WIDTH_MULTIPLIER = 3f
private const val HALO_MIN_ALPHA = 40
private const val HALO_MAX_ALPHA = 130
private const val PULSE_PERIOD_MILLIS = 1600

/** The "path still ahead" route line on [RouteForgeMap]: a thin, fully-opaque [mainOverlay] drawn
 *  over a wider [haloOverlay] in the same color whose alpha [AnimateRoutePulse] breathes in and
 *  out, so the remaining route reads as highlighted rather than a plain line. */
internal class PulsingRouteOverlay(
    private val haloOverlay: Polyline,
    private val mainOverlay: Polyline,
) {
    fun setPoints(points: List<Pair<Double, Double>>) {
        val geoPoints = points.map { (latitude, longitude) -> GeoPoint(latitude, longitude) }
        haloOverlay.setPoints(geoPoints)
        mainOverlay.setPoints(geoPoints)
    }

    fun setColor(
        colorArgb: Int,
        lineWidthPx: Float,
    ) {
        mainOverlay.outlinePaint.color = colorArgb
        mainOverlay.outlinePaint.strokeWidth = lineWidthPx
        haloOverlay.outlinePaint.color = colorArgb
        haloOverlay.outlinePaint.strokeWidth = lineWidthPx * HALO_WIDTH_MULTIPLIER
    }

    fun setHaloAlpha(alpha: Int) {
        haloOverlay.outlinePaint.alpha = alpha
    }
}

/** Builds a [PulsingRouteOverlay]'s pair of [Polyline]s and adds them to [mapView], halo first so
 *  the thinner main line draws on top of it. */
internal fun buildPulsingRouteOverlay(mapView: MapView): PulsingRouteOverlay {
    val haloOverlay = Polyline()
    val mainOverlay = Polyline()
    mapView.overlays.add(haloOverlay)
    mapView.overlays.add(mainOverlay)
    return PulsingRouteOverlay(haloOverlay, mainOverlay)
}

/** Continuously animates [overlay]'s halo alpha between a dim and a bright value, calling
 *  [onPulse] on every frame so the caller can invalidate its (non-Compose) `MapView`. */
@Composable
internal fun AnimateRoutePulse(
    overlay: PulsingRouteOverlay,
    onPulse: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "route-pulse")
    val pulse by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = PULSE_PERIOD_MILLIS, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "route-pulse-alpha",
        )
    LaunchedEffect(pulse) {
        overlay.setHaloAlpha(HALO_MIN_ALPHA + ((HALO_MAX_ALPHA - HALO_MIN_ALPHA) * pulse).toInt())
        onPulse()
    }
}
