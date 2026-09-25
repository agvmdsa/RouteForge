package com.routeforge.mocklocationsetup.presentation

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.routeforge.mocklocationsetup.domain.model.SetupStepId
import org.junit.Rule
import org.junit.Test

class MockLocationSetupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bothStepsRendered_whenBothPending() {
        composeTestRule.setContent {
            MockLocationSetupScreen(
                state =
                    MockLocationSetupState(
                        pendingSteps =
                            listOf(
                                SetupStepUi(
                                    id = SetupStepId.ENABLE_DEVELOPER_OPTIONS,
                                    title = "Enable Developer Options",
                                    explanation = "e",
                                    instructions = "i",
                                    canDeepLink = true,
                                ),
                                SetupStepUi(
                                    id = SetupStepId.SELECT_MOCK_LOCATION_APP,
                                    title = "Select mock location app",
                                    explanation = "e",
                                    instructions = "i",
                                    canDeepLink = true,
                                ),
                            ),
                    ),
                onAction = {},
            )
        }

        composeTestRule.onNodeWithText("Enable Developer Options").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select mock location app").assertIsDisplayed()
    }

    @Test
    fun onlyMockLocationStepRendered_whenDeveloperOptionsAlreadySatisfied() {
        composeTestRule.setContent {
            MockLocationSetupScreen(
                state =
                    MockLocationSetupState(
                        pendingSteps =
                            listOf(
                                SetupStepUi(
                                    id = SetupStepId.SELECT_MOCK_LOCATION_APP,
                                    title = "Select mock location app",
                                    explanation = "e",
                                    instructions = "i",
                                    canDeepLink = true,
                                ),
                            ),
                    ),
                onAction = {},
            )
        }

        composeTestRule.onNodeWithText("Select mock location app").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Developer Options").assertDoesNotExist()
    }

    @Test
    fun readyStateRendered_whenNoPendingSteps() {
        composeTestRule.setContent {
            MockLocationSetupScreen(state = MockLocationSetupState(isReady = true), onAction = {})
        }

        composeTestRule.onNodeWithText("You're all set!").assertIsDisplayed()
    }

    @Test
    fun policyBlockedMessageRendered_whenBlockedByPolicy() {
        composeTestRule.setContent {
            MockLocationSetupScreen(state = MockLocationSetupState(isBlockedByPolicy = true), onAction = {})
        }

        composeTestRule
            .onNodeWithText("Developer Options is disabled")
            .assertIsDisplayed()
    }
}
