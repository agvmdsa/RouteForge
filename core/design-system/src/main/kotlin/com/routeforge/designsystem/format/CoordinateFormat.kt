package com.routeforge.designsystem.format

import java.util.Locale

private const val COORDINATE_DECIMAL_PLACES = 5

/** Formats a latitude/longitude pair for display, always with a dot decimal separator (not the
 *  device locale's), since this text is routinely copied to the clipboard and pasted into GPS
 *  tools that expect a dot — a comma-locale device would otherwise silently produce "40,71234". */
fun formatCoordinates(
    latitude: Double,
    longitude: Double,
): String = "%.${COORDINATE_DECIMAL_PLACES}f, %.${COORDINATE_DECIMAL_PLACES}f".format(Locale.US, latitude, longitude)
