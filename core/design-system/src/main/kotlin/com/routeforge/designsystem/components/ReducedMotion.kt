package com.routeforge.designsystem.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Whether the user turned system animations off ("Remove animations" / animator duration scale
 *  0) — Compose has no stable cross-version reduce-motion API, and this scale is the documented
 *  Android control for it. Read once per composition, not observed: a tour step is short-lived. */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    return remember(contentResolver) {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
