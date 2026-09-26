package com.routeforge.simulation.presentation

import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Drawer content for the Simulation screen's sidebar menu — a short list of [destinations],
 *  each closing the drawer (via [onDismiss]) right after invoking its own action. */
@Composable
internal fun Sidebar(
    destinations: List<SidebarDestination>,
    onDismiss: () -> Unit,
) {
    ModalDrawerSheet {
        destinations.forEach { destination ->
            NavigationDrawerItem(
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
                selected = false,
                onClick = {
                    destination.onClick()
                    onDismiss()
                },
            )
        }
    }
}
