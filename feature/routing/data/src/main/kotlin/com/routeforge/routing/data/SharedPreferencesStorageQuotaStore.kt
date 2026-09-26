package com.routeforge.routing.data

import android.content.Context
import com.routeforge.routing.domain.DEFAULT_STORAGE_QUOTA_BYTES
import com.routeforge.routing.domain.StorageQuotaStore
import kotlinx.coroutines.flow.MutableStateFlow

private const val PREFS_NAME = "routeforge_storage_quota"
private const val KEY_MAX_BYTES = "max_bytes"
private const val UNLIMITED_SENTINEL = -1L

/** `null` (Unlimited) is stored as [UNLIMITED_SENTINEL] since [android.content.SharedPreferences]
 *  has no native nullable-Long API. */
class SharedPreferencesStorageQuotaStore(
    context: Context,
) : StorageQuotaStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val maxBytes = MutableStateFlow(readMaxBytes())

    override fun observeMaxBytes() = maxBytes

    override fun setMaxBytes(maxBytes: Long?) {
        prefs.edit().putLong(KEY_MAX_BYTES, maxBytes ?: UNLIMITED_SENTINEL).apply()
        this.maxBytes.value = maxBytes
    }

    private fun readMaxBytes(): Long? {
        val stored = prefs.getLong(KEY_MAX_BYTES, DEFAULT_STORAGE_QUOTA_BYTES)
        return if (stored == UNLIMITED_SENTINEL) null else stored
    }
}
