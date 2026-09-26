package com.routeforge.routing.domain

import kotlinx.coroutines.flow.StateFlow

/** The budget applied until the user has ever chosen one of their own (FR-004). */
const val DEFAULT_STORAGE_QUOTA_BYTES = 1_000_000_000L

/** Persists the user's chosen maximum storage budget for downloaded offline map region data.
 *  A `null` budget means Unlimited — no automatic cleanup ever runs. */
interface StorageQuotaStore {
    fun observeMaxBytes(): StateFlow<Long?>

    fun setMaxBytes(maxBytes: Long?)
}
