package com.routeforge.addresssearch.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.model.PlaceSearchResult
import com.routeforge.designsystem.components.ConfirmationBottomSheet
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp
private val SubmitIndicatorSize = 24.dp
private const val RESULTS_HEIGHT_FRACTION = 0.75f

@Composable
fun AddressSearchRoot(
    sourceContext: SearchSourceContext,
    referenceLatitude: Double?,
    referenceLongitude: Double?,
    onNavigateBack: () -> Unit,
    viewModel: AddressSearchViewModel =
        koinViewModel { parametersOf(sourceContext, referenceLatitude, referenceLongitude) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddressSearchEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    AddressSearchScreen(state = state, onAction = viewModel::onAction, onNavigateBack = onNavigateBack)
}

/** A single bottom sheet (no map preview) — a search field up top, and once a search has been
 *  submitted, a scrollable results list capped at [RESULTS_HEIGHT_FRACTION] of the available
 *  height, matching [com.routeforge.designsystem.waypointedit.WaypointEditSheet]'s established
 *  `BoxWithConstraints`-based fixed-fraction pattern for the same reason: a reactively-measured
 *  height would deadlock here just as it did there. */
@Composable
fun AddressSearchScreen(
    state: AddressSearchState,
    onAction: (AddressSearchAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onNavigateBack, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(ScreenContentPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onAction(AddressSearchAction.OnQueryChange(it)) },
                placeholder = { Text(stringResource(R.string.address_search_query_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onAction(AddressSearchAction.OnSubmit) }),
                trailingIcon = {
                    IconButton(onClick = { onAction(AddressSearchAction.OnSubmit) }, enabled = !state.isLoading) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(SubmitIndicatorSize))
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.address_search_submit_button))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.hasSearched) {
                Spacer(modifier = Modifier.height(ControlsRowSpacing))
                when {
                    state.errorType != null ->
                        Text(
                            stringResource(R.string.address_search_error_message),
                            modifier = Modifier.padding(top = ScreenContentPadding),
                        )
                    state.results.isEmpty() ->
                        Text(
                            stringResource(R.string.address_search_no_results_message),
                            modifier = Modifier.padding(top = ScreenContentPadding),
                        )
                    else ->
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val resultsHeight = maxHeight * RESULTS_HEIGHT_FRACTION
                            LazyColumn(modifier = Modifier.fillMaxWidth().height(resultsHeight)) {
                                itemsIndexed(state.results) { index, result ->
                                    ListItem(
                                        headlineContent = { Text(result.name) },
                                        supportingContent = { Text(result.formattedAddress) },
                                        modifier = Modifier.clickable { onAction(AddressSearchAction.OnSelectResult(index)) },
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    val choiceResult = state.choiceForResult
    if (choiceResult != null) {
        if (!state.isNamingFavorite) {
            TeleportOrFavoriteChoiceSheet(
                result = choiceResult,
                onChooseTeleport = { onAction(AddressSearchAction.OnChooseTeleport) },
                onChooseSaveAsFavorite = { onAction(AddressSearchAction.OnChooseSaveAsFavorite) },
                onDismiss = { onAction(AddressSearchAction.OnDismissChoice) },
            )
        } else {
            FavoriteNameSheet(
                nameInput = state.favoriteNameInput,
                onNameChange = { onAction(AddressSearchAction.OnFavoriteNameChange(it)) },
                onConfirm = { onAction(AddressSearchAction.OnConfirmFavoriteName) },
                onDismiss = { onAction(AddressSearchAction.OnDismissFavoriteName) },
            )
        }
    }
}

@Composable
private fun TeleportOrFavoriteChoiceSheet(
    result: PlaceSearchResult,
    onChooseTeleport: () -> Unit,
    onChooseSaveAsFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = result.name, onDismiss = onDismiss) {
        Button(onClick = onChooseTeleport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.address_search_teleport_button))
        }
        TextButton(onClick = onChooseSaveAsFavorite, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.address_search_save_favorite_button))
        }
    }
}

@Composable
private fun FavoriteNameSheet(
    nameInput: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = stringResource(R.string.address_search_favorite_name_title), onDismiss = onDismiss) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.address_search_favorite_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Button(onClick = onConfirm, enabled = nameInput.isNotBlank()) {
                Text(stringResource(R.string.address_search_favorite_save_button))
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.address_search_cancel_button)) }
        }
    }
}
