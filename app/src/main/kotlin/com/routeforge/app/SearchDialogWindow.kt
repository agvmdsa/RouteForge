package com.routeforge.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Hosts the search inside the navigation dialog without the dialog's own dimming. The search sheet draws the only scrim, so the
 * screen behind it is lightly dimmed once, not twice.
 */
@Composable
internal fun SearchDialogWindow(content: @Composable () -> Unit) {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { dialogWindow?.setDimAmount(0f) }
    content()
}
