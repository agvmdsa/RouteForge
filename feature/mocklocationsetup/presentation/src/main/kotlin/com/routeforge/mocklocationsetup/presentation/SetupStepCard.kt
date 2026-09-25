package com.routeforge.mocklocationsetup.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.components.IconBadge
import com.routeforge.mocklocationsetup.domain.model.SetupStepId

private val SectionSpacing = 32.dp
private val CardPadding = 24.dp
private val CardShape = RoundedCornerShape(24.dp)
private val StepIconBadgeSize = 44.dp

@Composable
internal fun SetupHeader(
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
internal fun SetupStepCard(
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

private fun SetupStepId.toIcon(): ImageVector =
    when (this) {
        SetupStepId.ENABLE_DEVELOPER_OPTIONS -> Icons.Filled.DeveloperMode
        SetupStepId.SELECT_MOCK_LOCATION_APP -> Icons.Filled.MyLocation
    }
