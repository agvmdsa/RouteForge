package com.routeforge.routing.presentation

import com.routeforge.routing.domain.DEFAULT_STORAGE_QUOTA_BYTES

data class SettingsState(
    val quotaBytes: Long? = DEFAULT_STORAGE_QUOTA_BYTES,
    val usedBytes: Long = 0L,
)
