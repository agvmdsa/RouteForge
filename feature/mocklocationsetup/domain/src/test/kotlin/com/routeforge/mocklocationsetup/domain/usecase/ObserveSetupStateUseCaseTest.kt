package com.routeforge.mocklocationsetup.domain.usecase

import com.routeforge.mocklocationsetup.domain.FakeDeveloperSettingsDataSource
import com.routeforge.mocklocationsetup.domain.model.SetupStepId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObserveSetupStateUseCaseTest {
    @Test
    fun `both steps pending when neither check passes`() {
        val dataSource =
            FakeDeveloperSettingsDataSource(
                developerOptionsEnabled = false,
                appSelectedAsMockLocationProvider = false,
            )
        val useCase = ObserveSetupStateUseCase(dataSource)

        val state = useCase()

        assertEquals(
            listOf(SetupStepId.ENABLE_DEVELOPER_OPTIONS, SetupStepId.SELECT_MOCK_LOCATION_APP),
            state.pendingSteps.map { it.id },
        )
        assertTrue(!state.isReady)
    }

    @Test
    fun `pending steps empty and ready when both checks pass`() {
        val dataSource =
            FakeDeveloperSettingsDataSource(
                developerOptionsEnabled = true,
                appSelectedAsMockLocationProvider = true,
            )
        val useCase = ObserveSetupStateUseCase(dataSource)

        val state = useCase()

        assertTrue(state.pendingSteps.isEmpty())
        assertTrue(state.isReady)
    }

    @Test
    fun `only select mock location app step when developer options already enabled`() {
        val dataSource =
            FakeDeveloperSettingsDataSource(
                developerOptionsEnabled = true,
                appSelectedAsMockLocationProvider = false,
            )
        val useCase = ObserveSetupStateUseCase(dataSource)

        val state = useCase()

        assertEquals(
            listOf(SetupStepId.SELECT_MOCK_LOCATION_APP),
            state.pendingSteps.map { it.id },
        )
    }

    @Test
    fun `only select mock location app step reappears when a different app is later selected`() {
        val dataSource =
            FakeDeveloperSettingsDataSource(
                developerOptionsEnabled = true,
                appSelectedAsMockLocationProvider = false,
            )
        val useCase = ObserveSetupStateUseCase(dataSource)

        val state = useCase()

        assertEquals(
            listOf(SetupStepId.SELECT_MOCK_LOCATION_APP),
            state.pendingSteps.map { it.id },
        )
    }

    @Test
    fun `both steps reappear starting with enable developer options when developer options is later disabled`() {
        val dataSource =
            FakeDeveloperSettingsDataSource(
                developerOptionsEnabled = false,
                appSelectedAsMockLocationProvider = false,
            )
        val useCase = ObserveSetupStateUseCase(dataSource)

        val state = useCase()

        assertEquals(
            listOf(SetupStepId.ENABLE_DEVELOPER_OPTIONS, SetupStepId.SELECT_MOCK_LOCATION_APP),
            state.pendingSteps.map { it.id },
        )
    }

    @Test
    fun `blocked by policy short-circuits to the policy-restricted state`() {
        val dataSource =
            FakeDeveloperSettingsDataSource(
                developerOptionsEnabled = false,
                appSelectedAsMockLocationProvider = false,
                restrictedByPolicy = true,
            )
        val useCase = ObserveSetupStateUseCase(dataSource)

        val state = useCase()

        assertTrue(state.isBlockedByPolicy)
        assertTrue(state.pendingSteps.isEmpty())
        assertTrue(!state.isReady)
    }
}
