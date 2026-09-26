package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.FakeRegionUsageTracker
import com.routeforge.routing.domain.FakeStorageQuotaStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SetStorageQuotaUseCaseTest {
    private val store = FakeStorageQuotaStore()

    private fun createUseCase(): SetStorageQuotaUseCase =
        SetStorageQuotaUseCase(
            storageQuotaStore = store,
            enforceStorageQuota =
                EnforceStorageQuotaUseCase(
                    storageQuotaStore = store,
                    regionCatalog = FakeRegionCatalog(),
                    regionUsageTracker = FakeRegionUsageTracker(),
                    draftWaypointsHolder = DraftWaypointsHolder(),
                    lastComputedRouteHolder = LastComputedRouteHolder(),
                ),
        )

    @Test
    fun `persists a fixed budget so it can be observed afterward`() =
        runTest {
            createUseCase()(500_000_000L)

            assertEquals(500_000_000L, store.observeMaxBytes().first())
        }

    @Test
    fun `persists Unlimited as null`() =
        runTest {
            createUseCase()(null)

            assertNull(store.observeMaxBytes().first())
        }
}
