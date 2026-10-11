package com.routeforge.mocklocationsetup.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.components.IconBadge
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.mocklocationsetup.domain.model.SetupStepId
import org.koin.androidx.compose.koinViewModel

private val ScreenPadding = 24.dp
private val CardSpacing = 16.dp
internal val IconBadgeSize = 72.dp

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
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
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
    val completedSteps = SetupStepId.Count - steps.size
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        item {
            SetupHeader(completedSteps = completedSteps, totalSteps = SetupStepId.Count)
        }
        items(steps, key = { it.id }) { step ->
            SetupStepCard(step = step, onAction = onAction)
        }
    }
}

@Composable
private fun StatusMessageContent(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon = icon, size = IconBadgeSize, tint = iconTint, containerColor = iconContainerColor)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ReadyContent(modifier: Modifier = Modifier) {
    StatusMessageContent(
        icon = Icons.Filled.CheckCircle,
        title = stringResource(R.string.setup_ready_title),
        subtitle = stringResource(R.string.setup_ready_subtitle),
        modifier = modifier,
    )
}

@Composable
private fun PolicyBlockedContent(modifier: Modifier = Modifier) {
    StatusMessageContent(
        icon = Icons.Filled.Block,
        title = stringResource(R.string.setup_blocked_title),
        subtitle = stringResource(R.string.setup_blocked_message),
        modifier = modifier,
        iconTint = MaterialTheme.colorScheme.error,
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
    )
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
                                deepLinkIntent = Intent(Intent.ACTION_MAIN),
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
