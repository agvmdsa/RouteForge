package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.StorageQuotaStore

class SetStorageQuotaUseCase(
    private val storageQuotaStore: StorageQuotaStore,
    private val enforceStorageQuota: EnforceStorageQuotaUseCase,
) {
    operator fun invoke(maxBytes: Long?) {
        storageQuotaStore.setMaxBytes(maxBytes)
        enforceStorageQuota()
    }
}
