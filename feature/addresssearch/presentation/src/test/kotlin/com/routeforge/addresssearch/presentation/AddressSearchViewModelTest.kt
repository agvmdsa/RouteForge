package com.routeforge.addresssearch.presentation

import com.routeforge.addresssearch.domain.GeocodeClient
import com.routeforge.addresssearch.domain.usecase.RankSearchResultsUseCase
import com.routeforge.addresssearch.domain.usecase.SearchPlacesUseCase
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.holder.PendingSearchWaypointHolder
import com.routeforge.coredomain.holder.PendingTeleportTargetHolder
import com.routeforge.coredomain.model.FavoriteWaypoint
import com.routeforge.coredomain.model.PlaceSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private fun place(
    name: String = "Place",
    latitude: Double = 1.0,
    longitude: Double = 2.0,
) = PlaceSearchResult(name = name, formattedAddress = name, latitude = latitude, longitude = longitude, importance = 0.5)

private class FakeGeocodeClient(
    var result: Result<List<PlaceSearchResult>, DataError.Network> = Result.Success(listOf(place())),
) : GeocodeClient {
    override suspend fun search(query: String): Result<List<PlaceSearchResult>, DataError.Network> = result
}

private class FakeFavoriteWaypointsRepository : FavoriteWaypointsRepository {
    val added = mutableListOf<Triple<String, Double, Double>>()
    private val _favorites = MutableStateFlow(emptyList<FavoriteWaypoint>())

    override fun observeFavorites(): StateFlow<List<FavoriteWaypoint>> = _favorites

    override fun add(
        name: String,
        latitude: Double,
        longitude: Double,
    ): FavoriteWaypoint {
        added += Triple(name, latitude, longitude)
        return FavoriteWaypoint(id = "fake", name = name, latitude = latitude, longitude = longitude)
    }

    override fun update(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
    ) = Unit

    override fun delete(id: String) = Unit
}

class AddressSearchViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val geocodeClient = FakeGeocodeClient()
    private val pendingSearchWaypointHolder = PendingSearchWaypointHolder()
    private val pendingTeleportTargetHolder = PendingTeleportTargetHolder()
    private val favoriteWaypointsRepository = FakeFavoriteWaypointsRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(sourceContext: SearchSourceContext = SearchSourceContext.TELEPORT_OR_FAVORITE): AddressSearchViewModel =
        AddressSearchViewModel(
            sourceContext = sourceContext,
            referenceLatitude = null,
            referenceLongitude = null,
            searchPlaces = SearchPlacesUseCase(geocodeClient, RankSearchResultsUseCase(), LastKnownRealLocationHolder()),
            pendingSearchWaypointHolder = pendingSearchWaypointHolder,
            pendingTeleportTargetHolder = pendingTeleportTargetHolder,
            favoriteWaypointsRepository = favoriteWaypointsRepository,
        )

    @Test
    fun `a blank query does not trigger a search`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnSubmit)

            assertFalse(viewModel.state.value.hasSearched)
        }

    @Test
    fun `a successful search populates results`() =
        runTest {
            val result = place(name = "Found place")
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)

            assertEquals(listOf(result), viewModel.state.value.results)
            assertTrue(viewModel.state.value.hasSearched)
            assertNull(viewModel.state.value.errorType)
        }

    @Test
    fun `a failed search surfaces a network error`() =
        runTest {
            geocodeClient.result = Result.Error(DataError.Network.NO_INTERNET)
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)

            assertEquals(AddressSearchError.NETWORK_FAILURE, viewModel.state.value.errorType)
            assertTrue(viewModel.state.value.results.isEmpty())
        }

    @Test
    fun `selecting a result when opened from Plan Route hands it off without a choice prompt`() =
        runTest {
            val result = place()
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel(sourceContext = SearchSourceContext.PLAN_ROUTE)
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnSelectResult(0))

            assertEquals(result, pendingSearchWaypointHolder.selected.value)
            assertNull(viewModel.state.value.pendingChoiceResult)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `selecting a result when opened from elsewhere prompts a teleport-or-favorite choice`() =
        runTest {
            val result = place()
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel(sourceContext = SearchSourceContext.TELEPORT_OR_FAVORITE)
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)

            viewModel.onAction(AddressSearchAction.OnSelectResult(0))

            assertEquals(result, viewModel.state.value.pendingChoiceResult)
            assertNull(pendingSearchWaypointHolder.selected.value)
        }

    @Test
    fun `selecting an out-of-range result index is a no-op`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnSelectResult(0))

            assertNull(viewModel.state.value.pendingChoiceResult)
        }

    @Test
    fun `choosing teleport hands off the result's coordinates as a route point`() =
        runTest {
            val result = place(latitude = 10.0, longitude = 20.0)
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnChooseTeleport)

            assertEquals(10.0, pendingTeleportTargetHolder.target.value?.latitude)
            assertEquals(20.0, pendingTeleportTargetHolder.target.value?.longitude)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `confirming a blank favorite name does not save it`() =
        runTest {
            val result = place()
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
            viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("   "))

            viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)

            assertTrue(favoriteWaypointsRepository.added.isEmpty())
        }

    @Test
    fun `confirming a favorite name saves it under the result's coordinates`() =
        runTest {
            val result = place(latitude = 5.0, longitude = 6.0)
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
            viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("Home"))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)

            assertEquals(listOf(Triple("Home", 5.0, 6.0)), favoriteWaypointsRepository.added)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }
}
