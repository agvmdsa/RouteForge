package com.routeforge.mocklocationsetup.domain.usecase

import com.routeforge.mocklocationsetup.domain.DeveloperSettingsDataSource
import com.routeforge.mocklocationsetup.domain.model.SetupState
import com.routeforge.mocklocationsetup.domain.model.SetupStep
import com.routeforge.mocklocationsetup.domain.model.SetupStepId

class ObserveSetupStateUseCase(
    private val dataSource: DeveloperSettingsDataSource,
) {
    operator fun invoke(): SetupState {
        if (dataSource.isDeveloperOptionsRestrictedByPolicy()) {
            return SetupState(pendingSteps = emptyList(), isBlockedByPolicy = true)
        }

        val pendingSteps =
            buildList {
                if (!dataSource.isDeveloperOptionsEnabled()) {
                    add(
                        SetupStep(
                            id = SetupStepId.ENABLE_DEVELOPER_OPTIONS,
                            title = "Enable Developer Options",
                            explanation =
                                "RouteForge needs Developer Options turned on before it can " +
                                    "inject a mock location, since Android hides mock-location controls " +
                                    "until Developer Options is enabled.",
                            instructions =
                                "Open About Phone and tap the Build Number entry 7 times " +
                                    "until you see \"You are now a developer\".",
                            canDeepLink = dataSource.developerOptionsDeepLinkIntent() != null,
                        ),
                    )
                }

                if (!dataSource.isAppSelectedAsMockLocationProvider()) {
                    add(
                        SetupStep(
                            id = SetupStepId.SELECT_MOCK_LOCATION_APP,
                            title = "Select mock location app",
                            explanation =
                                "Android only allows one app at a time to simulate GPS " +
                                    "locations. RouteForge must be chosen as that app before it can " +
                                    "inject a route.",
                            instructions =
                                "Open Developer Options, find \"Select mock location app\", " +
                                    "and choose RouteForge.",
                            canDeepLink = dataSource.mockLocationAppDeepLinkIntent() != null,
                        ),
                    )
                }
            }

        return SetupState(pendingSteps = pendingSteps, isBlockedByPolicy = false)
    }
}
