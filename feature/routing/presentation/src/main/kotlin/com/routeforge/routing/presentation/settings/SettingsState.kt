package com.routeforge.routing.presentation.settings

import com.routeforge.coredomain.model.MapStyle
import com.routeforge.routing.domain.DEFAULT_STORAGE_QUOTA_BYTES

data class SettingsState(
    val quotaBytes: Long? = DEFAULT_STORAGE_QUOTA_BYTES,
    val usedBytes: Long = 0L,
    val mapStyle: MapStyle = MapStyle.DEFAULT,
)
