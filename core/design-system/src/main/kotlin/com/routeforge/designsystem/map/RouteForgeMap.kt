package com.routeforge.designsystem.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val OSMDROID_PREFS_NAME = "osmdroid_config_routeforge"
private const val DEFAULT_MAP_ZOOM = 15.0
private const val FALLBACK_WORLD_MAP_ZOOM = 3.0
private const val ROUTE_LINE_WIDTH_PX = 6f
private val MarkerDotSize = 20.dp
private val MarkerBadgeSize = 28.dp

private fun buildDotDrawable(
    context: Context,
    colorArgb: Int,
    sizePx: Int,
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb; style = Paint.Style.FILL }
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius, paint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun buildNumberedDrawable(
    context: Context,
    number: Int,
    backgroundColorArgb: Int,
    textColorArgb: Int,
    sizePx: Int,
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColorArgb; style = Paint.Style.FILL }
    canvas.drawCircle(radius, radius, radius, backgroundPaint)
    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorArgb
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.55f
        }
    val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(number.toString(), radius, textY, textPaint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun RouteForgeMapMarkerIcon.toDrawable(
    context: Context,
    density: Density,
): BitmapDrawable =
    when (this) {
        is RouteForgeMapMarkerIcon.Dot -> {
            val sizePx = with(density) { MarkerDotSize.roundToPx() }
            buildDotDrawable(context, colorArgb, sizePx)
        }
        is RouteForgeMapMarkerIcon.Numbered -> {
            val sizePx = with(density) { MarkerBadgeSize.roundToPx() }
            buildNumberedDrawable(context, number, backgroundColorArgb, textColorArgb, sizePx)
        }
    }

private data class RouteForgeMapComponents(
    val mapView: MapView,
    val routeOverlay: Polyline,
)

private fun buildMapComponents(
    context: Context,
    onTap: (Double, Double) -> Unit,
): RouteForgeMapComponents {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences(OSMDROID_PREFS_NAME, Context.MODE_PRIVATE))
        userAgentValue = context.packageName
    }
    val mapView =
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(FALLBACK_WORLD_MAP_ZOOM)
        }
    val routeOverlay = Polyline().apply { outlinePaint.strokeWidth = ROUTE_LINE_WIDTH_PX }
    val tapOverlay =
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    onTap(point.latitude, point.longitude)
                    return true
                }

                override fun longPressHelper(point: GeoPoint): Boolean = false
            },
        )
    mapView.overlays.add(tapOverlay)
    mapView.overlays.add(routeOverlay)
    return RouteForgeMapComponents(mapView, routeOverlay)
}

/**
 * Shared OSM map surface used by every RouteForge screen that renders a map (Simulation, Route
 * creation). Callers describe *what* to show (markers, an optional polyline, a tap callback);
 * this component owns the OSMDroid `MapView` lifecycle and overlay bookkeeping.
 *
 * Camera movement is caller-driven: pass a new [cameraTarget] value whenever the screen decides
 * the map should recenter (e.g. "a mocked session just started" or "the first waypoint was just
 * placed") — the map does not decide this on its own, since different screens want different
 * auto-centering policies (Simulation recenters per new session; route-building recenters once).
 */
@Composable
fun RouteForgeMap(
    markers: List<RouteForgeMapMarker>,
    modifier: Modifier = Modifier,
    polylinePoints: List<Pair<Double, Double>> = emptyList(),
    cameraTarget: Pair<Double, Double>? = null,
    onMapTap: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnTap by rememberUpdatedState(onMapTap)

    val components =
        remember {
            buildMapComponents(
                context = context,
                onTap = { latitude, longitude -> currentOnTap?.invoke(latitude, longitude) },
            )
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> components.mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> components.mapView.onPause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            components.mapView.onDetach()
        }
    }

    LaunchedEffect(markers) {
        val mapView = components.mapView
        mapView.overlays.removeAll { it is Marker }
        for (marker in markers) {
            val osmMarker =
                Marker(mapView).apply {
                    position = GeoPoint(marker.latitude, marker.longitude)
                    icon = marker.icon.toDrawable(context, density)
                    isDraggable = marker.draggable
                    marker.onClick?.let { onClick ->
                        setOnMarkerClickListener { _, _ ->
                            onClick()
                            true
                        }
                    }
                    marker.onDragEnd?.let { onDragEnd ->
                        setOnMarkerDragListener(
                            object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker) = Unit

                                override fun onMarkerDragEnd(marker: Marker) {
                                    onDragEnd(marker.position.latitude, marker.position.longitude)
                                }

                                override fun onMarkerDragStart(marker: Marker) = Unit
                            },
                        )
                    }
                }
            mapView.overlays.add(osmMarker)
        }
        mapView.invalidate()
    }

    LaunchedEffect(polylinePoints) {
        components.routeOverlay.setPoints(polylinePoints.map { (latitude, longitude) -> GeoPoint(latitude, longitude) })
        components.mapView.invalidate()
    }

    val hasSetInitialZoom = remember { mutableStateOf(false) }
    LaunchedEffect(cameraTarget) {
        val target = cameraTarget ?: return@LaunchedEffect
        if (!hasSetInitialZoom.value) {
            components.mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
            hasSetInitialZoom.value = true
        }
        components.mapView.controller.setCenter(GeoPoint(target.first, target.second))
    }

    AndroidView(factory = { components.mapView }, modifier = modifier.fillMaxSize())
}
