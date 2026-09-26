package com.routeforge.routing.presentation

import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.routing.domain.DEFAULT_STORAGE_QUOTA_BYTES
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.usecase.EnforceStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.GetStorageUsageSummaryUseCase
import com.routeforge.routing.domain.usecase.ObserveStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.SetStorageQuotaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val quotaStore = FakeStorageQuotaStore()
    private val regionCatalog = FakeRegionCatalog()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            observeStorageQuota = ObserveStorageQuotaUseCase(quotaStore),
            setStorageQuota =
                SetStorageQuotaUseCase(
                    storageQuotaStore = quotaStore,
                    enforceStorageQuota =
                        EnforceStorageQuotaUseCase(
                            storageQuotaStore = quotaStore,
                            regionCatalog = regionCatalog,
                            regionUsageTracker = FakeRegionUsageTracker(),
                            draftWaypointsHolder = DraftWaypointsHolder(),
                            lastComputedRouteHolder = LastComputedRouteHolder(),
                        ),
                ),
            getStorageUsageSummary = GetStorageUsageSummaryUseCase(regionCatalog),
        )

    @Test
    fun `initial state reflects the default 1 GB budget`() {
        val viewModel = createViewModel()

        assertEquals(DEFAULT_STORAGE_QUOTA_BYTES, viewModel.state.value.quotaBytes)
    }

    @Test
    fun `selecting a quota updates state and persists it`() {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnQuotaSelected(2_000_000_000L))

        assertEquals(2_000_000_000L, viewModel.state.value.quotaBytes)
        assertEquals(2_000_000_000L, quotaStore.observeMaxBytes().value)
    }

    @Test
    fun `selecting Unlimited updates state to null`() {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnQuotaSelected(null))

        assertEquals(null, viewModel.state.value.quotaBytes)
    }

    @Test
    fun `initial state reflects the current storage usage summary`() {
        regionCatalog.regions =
            listOf(
                Region(
                    id = "a",
                    displayName = "a",
                    minLatitude = 0.0,
                    minLongitude = 0.0,
                    maxLatitude = 1.0,
                    maxLongitude = 1.0,
                    tileIds = listOf("a.rd5"),
                    approximateSizeBytes = 100L,
                    status = RegionStatus.DOWNLOADED,
                ),
            )
        regionCatalog.sizesOnDiskBytes = mapOf("a" to 250L)

        val viewModel = createViewModel()

        assertEquals(250L, viewModel.state.value.usedBytes)
    }

    @Test
    fun `usage summary reflects eviction after lowering the quota`() {
        regionCatalog.regions =
            listOf(
                Region(
                    id = "a",
                    displayName = "a",
                    minLatitude = 0.0,
                    minLongitude = 0.0,
                    maxLatitude = 1.0,
                    maxLongitude = 1.0,
                    tileIds = listOf("a.rd5"),
                    approximateSizeBytes = 2_000_000_000L,
                    status = RegionStatus.DOWNLOADED,
                ),
            )
        regionCatalog.sizesOnDiskBytes = mapOf("a" to 2_000_000_000L)
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnQuotaSelected(500_000_000L))

        assertEquals(0L, viewModel.state.value.usedBytes)
    }
}
