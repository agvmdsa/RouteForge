package com.routeforge.designsystem.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private const val ARROW_BORDER_WIDTH_FRACTION = 0.06f
private val MarkerDotSize = 20.dp
private val MarkerBadgeSize = 28.dp
private val MarkerArrowSize = 32.dp

private fun buildDotDrawable(
    context: Context,
    colorArgb: Int,
    sizePx: Int,
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = Paint.Style.FILL
        }
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
    val backgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColorArgb
            style = Paint.Style.FILL
        }
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

private fun buildArrowDrawable(
    context: Context,
    colorArgb: Int,
    sizePx: Int,
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // A north-facing chevron (Waze/Google-Maps-style nav puck); rotated per-marker via
    // Marker.rotation to reflect the mocked position's current bearing.
    val path =
        Path().apply {
            moveTo(sizePx * 0.5f, 0f)
            lineTo(sizePx * 0.9f, sizePx * 0.9f)
            lineTo(sizePx * 0.5f, sizePx * 0.65f)
            lineTo(sizePx * 0.1f, sizePx * 0.9f)
            close()
        }
    val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = Paint.Style.FILL
        }
    canvas.drawPath(path, fillPaint)
    val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * ARROW_BORDER_WIDTH_FRACTION
        }
    canvas.drawPath(path, borderPaint)
    return BitmapDrawable(context.resources, bitmap)
}

internal fun RouteForgeMapMarkerIcon.toDrawable(
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
        is RouteForgeMapMarkerIcon.Arrow -> {
            val sizePx = with(density) { MarkerArrowSize.roundToPx() }
            buildArrowDrawable(context, colorArgb, sizePx)
        }
    }
