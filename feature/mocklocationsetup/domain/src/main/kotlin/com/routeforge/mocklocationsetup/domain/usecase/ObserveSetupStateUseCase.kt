package com.routeforge.mocklocationsetup.domain.usecase

import com.routeforge.mocklocationsetup.domain.MockLocationSetupStateSource
import com.routeforge.mocklocationsetup.domain.model.SetupState
import com.routeforge.mocklocationsetup.domain.model.SetupStep
import com.routeforge.mocklocationsetup.domain.model.SetupStepId

/** One row of the setup checklist: [isPending] decides whether the step still needs to run,
 *  [hasDeepLink] whether a direct-to-settings shortcut exists. Collapsing both of today's steps
 *  into this shape (rather than one hand-copied if-block per step) means a third step is one more
 *  list entry, not a second copy-pasted block. */
private data class SetupStepDefinition(
    val id: SetupStepId,
    val title: String,
    val explanation: String,
    val instructions: String,
    val isPending: () -> Boolean,
    val hasDeepLink: () -> Boolean,
)

class ObserveSetupStateUseCase(
    private val dataSource: MockLocationSetupStateSource,
) {
    operator fun invoke(): SetupState {
        if (dataSource.isDeveloperOptionsRestrictedByPolicy()) {
            return SetupState(pendingSteps = emptyList(), isBlockedByPolicy = true)
        }

        val stepDefinitions =
            listOf(
                SetupStepDefinition(
                    id = SetupStepId.ENABLE_DEVELOPER_OPTIONS,
                    title = "Enable Developer Options",
                    explanation =
                        "RouteForge needs Developer Options turned on before it can " +
                            "inject a mock location, since Android hides mock-location controls " +
                            "until Developer Options is enabled.",
                    instructions =
                        "Open About Phone and tap the Build Number entry 7 times " +
                            "until you see \"You are now a developer\".",
                    isPending = { !dataSource.isDeveloperOptionsEnabled() },
                    hasDeepLink = { dataSource.developerOptionsDeepLinkIntent() != null },
                ),
                SetupStepDefinition(
                    id = SetupStepId.SELECT_MOCK_LOCATION_APP,
                    title = "Select mock location app",
                    explanation =
                        "Android only allows one app at a time to simulate GPS " +
                            "locations. RouteForge must be chosen as that app before it can " +
                            "inject a route.",
                    instructions =
                        "Open Developer Options, find \"Select mock location app\", " +
                            "and choose RouteForge.",
                    isPending = { !dataSource.isAppSelectedAsMockLocationProvider() },
                    hasDeepLink = { dataSource.mockLocationAppDeepLinkIntent() != null },
                ),
            )

        val pendingSteps =
            stepDefinitions.filter { it.isPending() }.map { definition ->
                SetupStep(
                    id = definition.id,
                    title = definition.title,
                    explanation = definition.explanation,
                    instructions = definition.instructions,
                    canDeepLink = definition.hasDeepLink(),
                )
            }

        return SetupState(pendingSteps = pendingSteps, isBlockedByPolicy = false)
    }
}
