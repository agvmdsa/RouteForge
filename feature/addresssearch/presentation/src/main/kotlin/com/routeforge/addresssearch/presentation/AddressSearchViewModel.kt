package com.routeforge.addresssearch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.addresssearch.domain.usecase.SearchPlacesUseCase
import com.routeforge.coredomain.AddressLookup
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.RecentPlacesRepository
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.handoff.PendingSearchWaypointHandoffSender
import com.routeforge.coredomain.handoff.PendingTeleportTargetHandoffSender
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.PlaceSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [sourceContext]/[referenceLatitude]/[referenceLongitude]/[initialPlace]/[initialQuery] come from the [AddressSearchRoute] the
 *  screen was opened with — passed as plain constructor parameters (via Koin's parameterized
 *  `viewModel { ... }` DSL) rather than a `SavedStateHandle`, since they're immutable for this
 *  screen's whole lifetime. */
internal class AddressSearchViewModel(
    sourceContext: SearchSourceContext,
    private val referenceLatitude: Double?,
    private val referenceLongitude: Double?,
    initialPlace: InitialPlace? = null,
    initialQuery: String? = null,
    private val searchPlaces: SearchPlacesUseCase,
    private val pendingSearchWaypointSender: PendingSearchWaypointHandoffSender,
    private val pendingTeleportTargetSender: PendingTeleportTargetHandoffSender,
    private val favoriteWaypointsRepository: FavoriteWaypointsRepository,
    private val recentPlacesRepository: RecentPlacesRepository,
    private val addressLookup: AddressLookup,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            AddressSearchState(sourceContext = sourceContext, query = initialQuery?.takeIf { it.isNotBlank() }.orEmpty()),
        )
    val state = _state.asStateFlow()

    private val _events = Channel<AddressSearchEvent>()
    val events = _events.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        recentPlacesRepository
            .observeRecents()
            .onEach { recents -> _state.update { it.copy(recentPlaces = recents) } }
            .launchIn(viewModelScope)
        initialPlace?.let { place -> choose(placeWithoutAddress(place.name, place.latitude, place.longitude)) }
        // Text that arrived from another app is searched for once, as if it had been typed and submitted.
        if (_state.value.query.isNotBlank()) submit()
    }

    fun onAction(action: AddressSearchAction) {
        when (action) {
            is AddressSearchAction.OnQueryChange -> _state.update { it.copy(query = action.value) }
            AddressSearchAction.OnSubmit -> submit()
            is AddressSearchAction.OnSelectResult -> selectResult(action.index)
            is AddressSearchAction.OnSelectRecent -> selectRecent(action.index)
            is AddressSearchAction.OnRemoveRecent -> removeRecent(action.index)
            AddressSearchAction.OnClearRecentsClick -> _state.update { it.copy(isClearRecentsConfirmOpen = true) }
            AddressSearchAction.OnConfirmClearRecents -> clearRecents()
            AddressSearchAction.OnDismissClearRecents -> _state.update { it.copy(isClearRecentsConfirmOpen = false) }
            AddressSearchAction.OnDismissChoice -> dismissChoice()
            AddressSearchAction.OnChooseTeleport -> chooseTeleport()
            AddressSearchAction.OnChooseSaveAsFavorite ->
                _state.update {
                    // A place with no name starts from its address, when that is already known.
                    val chosenName = it.pendingChoiceResult?.name.orEmpty()
                    val startingName = chosenName.ifBlank { it.choiceAddress?.short.orEmpty() }
                    it.copy(isNamingFavorite = true, isFavoriteNameTouched = false, favoriteNameInput = startingName)
                }
            is AddressSearchAction.OnFavoriteNameChange ->
                _state.update { it.copy(favoriteNameInput = action.value, isFavoriteNameTouched = true) }
            AddressSearchAction.OnConfirmFavoriteName -> confirmFavoriteName()
            AddressSearchAction.OnDismissFavoriteName -> _state.update { it.copy(isNamingFavorite = false) }
        }
    }

    /** Cancels any still-pending prior search before launching a new one (FR-005) — a slow,
     *  now-stale response can never land after a newer query's results. */
    private fun submit() {
        val query = _state.value.query
        if (query.isBlank()) return
        searchJob?.cancel()
        _state.update { it.copy(isLoading = true, errorType = null) }
        searchJob =
            viewModelScope.launch {
                when (val result = searchPlaces(query, referenceLatitude, referenceLongitude)) {
                    is Result.Success ->
                        _state.update {
                            it.copy(isLoading = false, hasSearched = true, results = result.data, errorType = null)
                        }
                    is Result.Error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                hasSearched = true,
                                results = emptyList(),
                                errorType = AddressSearchError.NETWORK_FAILURE,
                            )
                        }
                }
            }
    }

    private fun selectResult(index: Int) {
        val result = _state.value.results.getOrNull(index) ?: return
        choose(result)
    }

    private fun selectRecent(index: Int) {
        val place = _state.value.recentPlaces.getOrNull(index) ?: return
        choose(placeWithoutAddress(place.name, place.latitude, place.longitude))
    }

    private fun removeRecent(index: Int) {
        val place = _state.value.recentPlaces.getOrNull(index) ?: return
        recentPlacesRepository.remove(place)
    }

    private fun clearRecents() {
        recentPlacesRepository.clear()
        _state.update { it.copy(isClearRecentsConfirmOpen = false) }
    }

    /** Picking a result, a recent place, or arriving with a place from another app, leads to the same next step. */
    private fun choose(result: PlaceSearchResult) {
        if (_state.value.sourceContext == SearchSourceContext.PLAN_ROUTE) {
            pendingSearchWaypointSender.send(result)
            viewModelScope.launch { _events.send(AddressSearchEvent.NavigateBack) }
        } else {
            _state.update { it.copy(pendingChoiceResult = result, choiceAddress = null) }
            if (result.name.isBlank()) lookUpAddress(result) else addressLookup.cancel()
        }
    }

    /** Only a place with no name gets its address looked up; it is shown in the choice and offered as the favorite's name. */
    private fun lookUpAddress(result: PlaceSearchResult) {
        addressLookup.start(viewModelScope, result.latitude, result.longitude) { address ->
            _state.update { current ->
                current.copy(
                    choiceAddress = address,
                    favoriteNameInput =
                        if (current.isNamingFavorite && !current.isFavoriteNameTouched) address.short else current.favoriteNameInput,
                )
            }
        }
    }

    private fun dismissChoice() {
        addressLookup.cancel()
        _state.update { it.copy(pendingChoiceResult = null, choiceAddress = null) }
    }

    private fun chooseTeleport() {
        val result = _state.value.pendingChoiceResult ?: return
        addressLookup.cancel()
        _state.update { it.copy(choiceAddress = null) }
        pendingTeleportTargetSender.send(
            NamedPlace(latitude = result.latitude, longitude = result.longitude, name = result.name.takeIf { it.isNotBlank() }),
        )
        viewModelScope.launch { _events.send(AddressSearchEvent.NavigateBack) }
    }

    private fun confirmFavoriteName() {
        val result = _state.value.pendingChoiceResult ?: return
        val name = _state.value.favoriteNameInput
        if (name.isBlank()) return
        addressLookup.cancel()
        favoriteWaypointsRepository.add(name, result.latitude, result.longitude)
        recentPlacesRepository.record(NamedPlace(latitude = result.latitude, longitude = result.longitude, name = name))
        viewModelScope.launch { _events.send(AddressSearchEvent.NavigateBack) }
    }
}

private fun placeWithoutAddress(
    name: String?,
    latitude: Double,
    longitude: Double,
) = PlaceSearchResult(
    name = name.orEmpty(),
    formattedAddress = "",
    latitude = latitude,
    longitude = longitude,
    importance = 0.0,
)
