package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.StorageQuotaStore
import kotlinx.coroutines.flow.Flow

class ObserveStorageQuotaUseCase(
    private val storageQuotaStore: StorageQuotaStore,
) {
    operator fun invoke(): Flow<Long?> = storageQuotaStore.observeMaxBytes()
}
