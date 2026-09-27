package com.routeforge.routing.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.model.FavoriteWaypoint
import com.routeforge.designsystem.components.ConfirmationBottomSheet
import com.routeforge.designsystem.components.IconBadge
import com.routeforge.designsystem.theme.RouteForgeTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenContentPadding = 16.dp
private val CardSpacing = 12.dp
private val CardShape = RoundedCornerShape(20.dp)
private val CardPadding = 16.dp
private val EmptyStateIconBadgeSize = 72.dp

@Composable
fun FavoritesRoot(
    isPickerMode: Boolean,
    onDone: () -> Unit,
    viewModel: FavoritesViewModel = koinViewModel { parametersOf(isPickerMode) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FavoritesEvent.NavigateBack -> onDone()
            }
        }
    }
    FavoritesScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun FavoritesScreen(
    state: FavoritesState,
    onAction: (FavoritesAction) -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text(
                text = stringResource(R.string.favorites_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(ScreenContentPadding),
            )
            if (state.favorites.isEmpty()) {
                EmptyFavoritesContent(modifier = Modifier.weight(1f).fillMaxWidth())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = ScreenContentPadding),
                    verticalArrangement = Arrangement.spacedBy(CardSpacing),
                ) {
                    items(state.favorites, key = { it.id }) { favorite ->
                        FavoriteCard(
                            favorite = favorite,
                            isPickerMode = state.isPickerMode,
                            onSelected = { onAction(FavoritesAction.OnFavoriteSelected(favorite.id)) },
                            onEditClick = { onAction(FavoritesAction.OnEditClick(favorite.id)) },
                            onDeleteClick = { onAction(FavoritesAction.OnDeleteClick(favorite.id)) },
                            onMockHereClick = { onAction(FavoritesAction.OnMockHereClick(favorite.id)) },
                        )
                    }
                }
            }
        }
    }

    state.pendingTeleportFavorite?.let { favorite ->
        ConfirmationBottomSheet(
            title = stringResource(R.string.favorites_mock_here_confirm_title, favorite.name),
            onDismiss = { onAction(FavoritesAction.OnDismissMockHere) },
        ) {
            Text(stringResource(R.string.favorites_mock_here_confirm_message))
            if (state.isPendingTeleportBlockedOffline) {
                Text(
                    text = stringResource(R.string.favorites_mock_here_blocked_offline_message),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAction(FavoritesAction.OnConfirmMockHere) }, enabled = !state.isPendingTeleportBlockedOffline) {
                    Text(stringResource(R.string.favorites_mock_here_confirm_button))
                }
                TextButton(onClick = { onAction(FavoritesAction.OnDismissMockHere) }) {
                    Text(stringResource(R.string.favorites_mock_here_cancel_button))
                }
            }
        }
    }

    if (state.editingId != null) {
        ModalBottomSheet(
            onDismissRequest = { onAction(FavoritesAction.OnDismissEdit) },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(ScreenContentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.favorites_edit_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.editNameInput,
                    onValueChange = { onAction(FavoritesAction.OnEditNameChange(it)) },
                    label = { Text(stringResource(R.string.routing_favorite_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.editLatitudeInput,
                        onValueChange = { onAction(FavoritesAction.OnEditLatitudeChange(it)) },
                        label = { Text(stringResource(R.string.routing_latitude_label)) },
                        modifier = Modifier.fillMaxWidth(0.5f),
                    )
                    OutlinedTextField(
                        value = state.editLongitudeInput,
                        onValueChange = { onAction(FavoritesAction.OnEditLongitudeChange(it)) },
                        label = { Text(stringResource(R.string.routing_longitude_label)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.editError != null) {
                    Text(text = state.editError.toMessage(), color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(FavoritesAction.OnConfirmEdit) }) { Text(stringResource(R.string.routing_save_button)) }
                    TextButton(onClick = { onAction(FavoritesAction.OnDismissEdit) }) { Text(stringResource(R.string.routing_cancel_button)) }
                }
            }
        }
    }

    if (state.pendingDeleteId != null) {
        ConfirmationBottomSheet(
            title = stringResource(R.string.favorites_delete_confirm_title),
            onDismiss = { onAction(FavoritesAction.OnDismissDelete) },
        ) {
            Text(stringResource(R.string.favorites_delete_confirm_message))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAction(FavoritesAction.OnConfirmDelete) }) { Text(stringResource(R.string.routing_delete_button)) }
                TextButton(onClick = { onAction(FavoritesAction.OnDismissDelete) }) { Text(stringResource(R.string.routing_cancel_button)) }
            }
        }
    }
}

@Composable
private fun FavoritesError.toMessage(): String =
    when (this) {
        FavoritesError.INVALID_NAME -> stringResource(R.string.favorites_error_invalid_name)
        FavoritesError.INVALID_COORDINATES -> stringResource(R.string.favorites_error_invalid_coordinates)
    }

@Composable
private fun FavoriteCard(
    favorite: FavoriteWaypoint,
    isPickerMode: Boolean,
    onSelected: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMockHereClick: () -> Unit,
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().let { if (isPickerMode) it.clickable(onClick = onSelected) else it },
    ) {
        Row(
            modifier = Modifier.padding(CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "%.5f, %.5f".format(favorite.latitude, favorite.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            if (isPickerMode) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.favorites_use_as_waypoint_label))
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.favorites_edit_button))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.routing_delete_button))
                }
                IconButton(onClick = onMockHereClick) {
                    Icon(Icons.Filled.PinDrop, contentDescription = stringResource(R.string.favorites_mock_here_button))
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(icon = Icons.Filled.PinDrop, size = EmptyStateIconBadgeSize)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.favorites_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.favorites_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun FavoritesScreenPreview() {
    RouteForgeTheme {
        FavoritesScreen(
            state =
                FavoritesState(
                    favorites =
                        listOf(
                            FavoriteWaypoint(id = "1", name = "Home", latitude = 52.5, longitude = 13.4),
                            FavoriteWaypoint(id = "2", name = "Work", latitude = 52.51, longitude = 13.41),
                        ),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun FavoritesScreenEmptyPreview() {
    RouteForgeTheme {
        FavoritesScreen(state = FavoritesState(), onAction = {})
    }
}
