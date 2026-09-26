package com.routeforge.routing.presentation

import com.routeforge.routing.domain.DEFAULT_STORAGE_QUOTA_BYTES
import com.routeforge.routing.domain.StorageQuotaStore
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
