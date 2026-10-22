package com.routeforge.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Map-overlay colors with no matching [androidx.compose.material3.ColorScheme] role — there's no
 *  "route line" slot in Material3's palette, and it needs to stay visually distinct from both the
 *  warm cream map/UI background and the waypoint markers' own color (the scheme's `primary`, a
 *  dark espresso-brown), so it isn't reused from the scheme either. */
internal object MapColors {
    /** The route polyline's color — a warm terracotta accent chosen from the same warm-earthy
     *  family as [RouteForgeTheme]'s palette (see that file), replacing a leftover cool-purple
     *  literal (`0xFF7C4DFF`) that predated this app's palette and never matched it. */
    val RouteLine = Color(0xFFC1521C)
}
