package com.routeforge.mocklocationsetup.presentation

import android.content.Intent
import com.routeforge.mocklocationsetup.domain.DeveloperSettingsDataSource
import com.routeforge.mocklocationsetup.domain.usecase.ObserveSetupStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeDataSource(
    var developerOptionsEnabled: Boolean = false,
    var appSelectedAsMockLocationProvider: Boolean = false,
    var restrictedByPolicy: Boolean = false,
) : DeveloperSettingsDataSource {
    override fun isDeveloperOptionsEnabled(): Boolean = developerOptionsEnabled

    override fun isAppSelectedAsMockLocationProvider(): Boolean = appSelectedAsMockLocationProvider

    override fun isDeveloperOptionsRestrictedByPolicy(): Boolean = restrictedByPolicy

    override fun developerOptionsDeepLinkIntent(): Intent? = null

    override fun mockLocationAppDeepLinkIntent(): Intent? = null
}

class MockLocationSetupViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load reflects the current pending steps`() {
        val dataSource = FakeDataSource(developerOptionsEnabled = false)
        val viewModel = MockLocationSetupViewModel(ObserveSetupStateUseCase(dataSource), dataSource)

        assertTrue(!viewModel.state.value.isReady)
        assertEquals(2, viewModel.state.value.pendingSteps.size)
    }

    @Test
    fun `refresh on resume advances state from pending to ready`() {
        val dataSource = FakeDataSource(developerOptionsEnabled = false)
        val viewModel = MockLocationSetupViewModel(ObserveSetupStateUseCase(dataSource), dataSource)

        assertTrue(!viewModel.state.value.isReady)

        dataSource.developerOptionsEnabled = true
        dataSource.appSelectedAsMockLocationProvider = true
        viewModel.refresh()

        assertTrue(viewModel.state.value.isReady)
    }

    @Test
    fun `refresh regresses state from ready back to pending when setup drifts`() {
        val dataSource =
            FakeDataSource(
                developerOptionsEnabled = true,
                appSelectedAsMockLocationProvider = true,
            )
        val viewModel = MockLocationSetupViewModel(ObserveSetupStateUseCase(dataSource), dataSource)

        assertTrue(viewModel.state.value.isReady)

        dataSource.appSelectedAsMockLocationProvider = false
        viewModel.refresh()

        assertTrue(!viewModel.state.value.isReady)
        assertEquals(1, viewModel.state.value.pendingSteps.size)
    }
}
