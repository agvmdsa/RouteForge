package com.routeforge.routing.domain

import kotlinx.coroutines.flow.MutableStateFlow

class FakeStorageQuotaStore(
    initialMaxBytes: Long? = DEFAULT_STORAGE_QUOTA_BYTES,
) : StorageQuotaStore {
    private val maxBytes = MutableStateFlow(initialMaxBytes)

    override fun observeMaxBytes() = maxBytes

    override fun setMaxBytes(maxBytes: Long?) {
        this.maxBytes.value = maxBytes
    }
}
