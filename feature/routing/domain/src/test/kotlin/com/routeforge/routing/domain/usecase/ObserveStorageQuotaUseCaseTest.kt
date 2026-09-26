package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.DEFAULT_STORAGE_QUOTA_BYTES
import com.routeforge.routing.domain.FakeStorageQuotaStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ObserveStorageQuotaUseCaseTest {
    @Test
    fun `emits the default budget when nothing was ever set`() =
        runTest {
            val store = FakeStorageQuotaStore()
            val useCase = ObserveStorageQuotaUseCase(store)

            assertEquals(DEFAULT_STORAGE_QUOTA_BYTES, useCase().first())
        }

    @Test
    fun `emits the updated value after it changes`() =
        runTest {
            val store = FakeStorageQuotaStore()
            val useCase = ObserveStorageQuotaUseCase(store)

            store.setMaxBytes(2_000_000_000L)

            assertEquals(2_000_000_000L, useCase().first())
        }

    @Test
    fun `emits null for an unlimited budget`() =
        runTest {
            val store = FakeStorageQuotaStore()
            val useCase = ObserveStorageQuotaUseCase(store)

            store.setMaxBytes(null)

            assertNull(useCase().first())
        }
}
