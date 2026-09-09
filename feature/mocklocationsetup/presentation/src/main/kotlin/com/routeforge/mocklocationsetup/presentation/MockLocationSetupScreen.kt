package com.routeforge.mocklocationsetup.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.mocklocationsetup.domain.model.SetupStepId
import org.koin.androidx.compose.koinViewModel

@Composable
fun MockLocationSetupRoot(
    onSetupReady: () -> Unit,
    viewModel: MockLocationSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MockLocationSetupEvent.LaunchDeepLink -> context.startActivity(event.intent)
            }
        }
    }

    LaunchedEffect(state.isReady) {
        if (state.isReady) onSetupReady()
    }

    MockLocationSetupScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun MockLocationSetupScreen(
    state: MockLocationSetupState,
    onAction: (MockLocationSetupAction) -> Unit,
) {
    Scaffold { paddingValues ->
        when {
            state.isBlockedByPolicy -> PolicyBlockedContent(Modifier.padding(paddingValues))
            state.isReady -> ReadyContent(Modifier.padding(paddingValues))
            else ->
                PendingStepsContent(
                    steps = state.pendingSteps,
                    onAction = onAction,
                    modifier = Modifier.padding(paddingValues),
                )
        }
    }
}

@Composable
private fun PendingStepsContent(
    steps: List<SetupStepUi>,
    onAction: (MockLocationSetupAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(steps, key = { it.id }) { step ->
            SetupStepCard(step = step, onAction = onAction)
        }
    }
}

@Composable
private fun SetupStepCard(
    step: SetupStepUi,
    onAction: (MockLocationSetupAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = step.title, style = MaterialTheme.typography.titleMedium)
        Text(text = step.explanation, style = MaterialTheme.typography.bodyMedium)
        Text(text = step.instructions, style = MaterialTheme.typography.bodySmall)
        if (step.canDeepLink) {
            Button(onClick = { onAction(MockLocationSetupAction.OnDeepLinkClick(step.id)) }) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun ReadyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "RouteForge is ready to simulate locations",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun PolicyBlockedContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Your device administrator has disabled Developer Options",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text =
                "RouteForge cannot enable mock locations on a managed device where this " +
                    "capability is restricted.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun MockLocationSetupScreenPendingPreview() {
    RouteForgeTheme {
        MockLocationSetupScreen(
            state =
                MockLocationSetupState(
                    pendingSteps =
                        listOf(
                            SetupStepUi(
                                id = SetupStepId.ENABLE_DEVELOPER_OPTIONS,
                                title = "Enable Developer Options",
                                explanation = "Explanation",
                                instructions = "Instructions",
                                canDeepLink = true,
                            ),
                        ),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun MockLocationSetupScreenReadyPreview() {
    RouteForgeTheme {
        MockLocationSetupScreen(
            state = MockLocationSetupState(isReady = true),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun MockLocationSetupScreenBlockedPreview() {
    RouteForgeTheme {
        MockLocationSetupScreen(
            state = MockLocationSetupState(isBlockedByPolicy = true),
            onAction = {},
        )
    }
}
