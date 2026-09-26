package com.routeforge.simulation.presentation

import androidx.compose.ui.graphics.vector.ImageVector

/** One item in the Simulation screen's sidebar menu — an icon, a label, and what tapping it does.
 *  Adding a future destination (e.g. Favorites) is just one more entry in the list passed to
 *  [Sidebar]; nothing else about the menu's own open/close/dismiss mechanics changes. */
data class SidebarDestination(
    val icon: ImageVector,
    val labelRes: Int,
    val onClick: () -> Unit,
)
