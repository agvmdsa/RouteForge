package com.routeforge.mocklocationsetup.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.mocklocationsetup.domain.model.SetupStepId
import org.koin.androidx.compose.koinViewModel

private val ScreenPadding = 24.dp
private val SectionSpacing = 32.dp
private val CardSpacing = 16.dp
private val CardPadding = 24.dp
private val CardShape = RoundedCornerShape(24.dp)
private val IconBadgeSize = 72.dp
private val StepIconBadgeSize = 44.dp
private val TotalSetupSteps = SetupStepId.entries.size

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
    val completedSteps = TotalSetupSteps - steps.size
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        item {
            SetupHeader(completedSteps = completedSteps, totalSteps = TotalSetupSteps)
        }
        items(steps, key = { it.id }) { step ->
            SetupStepCard(step = step, onAction = onAction)
        }
    }
}

@Composable
private fun SetupHeader(
    completedSteps: Int,
    totalSteps: Int,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = SectionSpacing)) {
        IconBadge(icon = Icons.Filled.MyLocation, size = IconBadgeSize)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.setup_header_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(
                progress = { if (totalSteps == 0) 1f else completedSteps.toFloat() / totalSteps },
                modifier = Modifier.weight(1f).height(8.dp),
                strokeCap = StrokeCap.Round,
            )
            Text(
                text = stringResource(R.string.setup_progress_label, completedSteps, totalSteps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SetupStepCard(
    step: SetupStepUi,
    onAction: (MockLocationSetupAction) -> Unit,
) {
    val stepNumber = SetupStepId.entries.indexOf(step.id) + 1
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBadge(icon = step.id.toIcon(), size = StepIconBadgeSize)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setup_step_number_label, stepNumber, TotalSetupSteps),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = step.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = step.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (step.canDeepLink) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onAction(MockLocationSetupAction.OnDeepLinkClick(step.id)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.setup_open_settings_button))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    size: Dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Surface(shape = CircleShape, color = containerColor, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
private fun ReadyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon = Icons.Filled.CheckCircle, size = IconBadgeSize)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.setup_ready_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_ready_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PolicyBlockedContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            icon = Icons.Filled.Block,
            size = IconBadgeSize,
            tint = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.setup_blocked_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_blocked_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

private fun SetupStepId.toIcon(): ImageVector =
    when (this) {
        SetupStepId.ENABLE_DEVELOPER_OPTIONS -> Icons.Filled.DeveloperMode
        SetupStepId.SELECT_MOCK_LOCATION_APP -> Icons.Filled.MyLocation
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
