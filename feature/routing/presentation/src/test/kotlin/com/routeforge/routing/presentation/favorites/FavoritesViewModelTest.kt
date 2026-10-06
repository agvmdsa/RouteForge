package com.routeforge.routing.presentation.favorites

import com.routeforge.coredomain.holder.PendingTeleportTargetHolder
import com.routeforge.coredomain.holder.SelectedFavoriteWaypointHolder
import com.routeforge.coredomain.model.FavoriteWaypoint
import com.routeforge.routing.presentation.FakeFavoriteWaypointsRepository
import com.routeforge.routing.presentation.FakeNetworkConnectivityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val home = FavoriteWaypoint(id = "1", name = "Home", latitude = 1.0, longitude = 2.0)

class FavoritesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeFavoriteWaypointsRepository(initialFavorites = listOf(home))
    private val pendingTeleportTargetHolder = PendingTeleportTargetHolder()
    private val selectedFavoriteWaypointHolder = SelectedFavoriteWaypointHolder()
    private val networkConnectivityChecker = FakeNetworkConnectivityChecker()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(isPickerMode: Boolean = false): FavoritesViewModel =
        FavoritesViewModel(
            isPickerMode = isPickerMode,
            favoriteWaypointsRepository = repository,
            pendingTeleportTargetHolder = pendingTeleportTargetHolder,
            selectedFavoriteWaypointHolder = selectedFavoriteWaypointHolder,
            networkConnectivityChecker = networkConnectivityChecker,
        )

    @Test
    fun `initial state reflects the repository's current favorites`() {
        val viewModel = createViewModel()

        assertEquals(listOf(home), viewModel.state.value.favorites)
    }

    @Test
    fun `clicking mock here sets a pending teleport favorite without writing the holder yet`() {
        val viewModel = createViewModel()

        viewModel.onAction(FavoritesAction.OnMockHereClick(home.id))

        assertEquals(home, viewModel.state.value.pendingTeleportFavorite)
        assertNull(pendingTeleportTargetHolder.target.value)
    }

    @Test
    fun `confirming mock here writes the pending teleport holder and navigates back`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            viewModel.onAction(FavoritesAction.OnMockHereClick(home.id))

            val eventDeferred = async { viewModel.events.first() }
            viewModel.onAction(FavoritesAction.OnConfirmMockHere)

            assertEquals(home.latitude, pendingTeleportTargetHolder.target.value?.latitude)
            assertEquals(home.longitude, pendingTeleportTargetHolder.target.value?.longitude)
            assertEquals(FavoritesEvent.NavigateBack, eventDeferred.await())
            assertNull(viewModel.state.value.pendingTeleportFavorite)
        }

    @Test
    fun `dismissing mock here clears the pending state without writing the holder`() {
        val viewModel = createViewModel()
        viewModel.onAction(FavoritesAction.OnMockHereClick(home.id))

        viewModel.onAction(FavoritesAction.OnDismissMockHere)

        assertNull(viewModel.state.value.pendingTeleportFavorite)
        assertNull(pendingTeleportTargetHolder.target.value)
    }

    @Test
    fun `being offline blocks confirming mock here`() {
        networkConnectivityChecker.connected = false
        val viewModel = createViewModel()
        viewModel.onAction(FavoritesAction.OnMockHereClick(home.id))
        assertTrue(viewModel.state.value.isPendingTeleportBlockedOffline)

        viewModel.onAction(FavoritesAction.OnConfirmMockHere)

        assertNull(pendingTeleportTargetHolder.target.value)
        assertNotNull(viewModel.state.value.pendingTeleportFavorite)
    }

    @Test
    fun `selecting a favorite in picker mode writes the selected-favorite holder and navigates back`() =
        runTest(dispatcher) {
            val viewModel = createViewModel(isPickerMode = true)

            val eventDeferred = async { viewModel.events.first() }
            viewModel.onAction(FavoritesAction.OnFavoriteSelected(home.id))

            assertEquals(home.latitude, selectedFavoriteWaypointHolder.selected.value?.latitude)
            assertEquals(home.longitude, selectedFavoriteWaypointHolder.selected.value?.longitude)
            assertEquals(FavoritesEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `selecting a favorite outside picker mode does nothing`() {
        val viewModel = createViewModel(isPickerMode = false)

        viewModel.onAction(FavoritesAction.OnFavoriteSelected(home.id))

        assertNull(selectedFavoriteWaypointHolder.selected.value)
    }

    @Test
    fun `editing a favorite updates its name and coordinate`() {
        val viewModel = createViewModel()
        viewModel.onAction(FavoritesAction.OnEditClick(home.id))

        viewModel.onAction(FavoritesAction.OnEditNameChange("Work"))
        viewModel.onAction(FavoritesAction.OnEditLatitudeChange("9.0"))
        viewModel.onAction(FavoritesAction.OnEditLongitudeChange("10.0"))
        viewModel.onAction(FavoritesAction.OnConfirmEdit)

        val updated = viewModel.state.value.favorites.first { it.id == home.id }
        assertEquals("Work", updated.name)
        assertEquals(9.0, updated.latitude)
        assertEquals(10.0, updated.longitude)
        assertNull(viewModel.state.value.editingId)
    }

    @Test
    fun `confirming an edit with a blank name is rejected`() {
        val viewModel = createViewModel()
        viewModel.onAction(FavoritesAction.OnEditClick(home.id))
        viewModel.onAction(FavoritesAction.OnEditNameChange(""))

        viewModel.onAction(FavoritesAction.OnConfirmEdit)

        assertEquals(FavoritesError.INVALID_NAME, viewModel.state.value.editError)
        assertEquals(home, viewModel.state.value.favorites.first { it.id == home.id })
    }

    @Test
    fun `confirming an edit with an unparseable coordinate is rejected`() {
        val viewModel = createViewModel()
        viewModel.onAction(FavoritesAction.OnEditClick(home.id))
        viewModel.onAction(FavoritesAction.OnEditLatitudeChange("not-a-number"))

        viewModel.onAction(FavoritesAction.OnConfirmEdit)

        assertEquals(FavoritesError.INVALID_COORDINATES, viewModel.state.value.editError)
        assertEquals(home, viewModel.state.value.favorites.first { it.id == home.id })
    }

    @Test
    fun `confirming a delete removes the favorite`() {
        val viewModel = createViewModel()
        viewModel.onAction(FavoritesAction.OnDeleteClick(home.id))

        viewModel.onAction(FavoritesAction.OnConfirmDelete)

        assertTrue(viewModel.state.value.favorites.isEmpty())
        assertNull(viewModel.state.value.pendingDeleteId)
    }

    @Test
    fun `dismissing a delete keeps the favorite`() {
        val viewModel = createViewModel()
        viewModel.onAction(FavoritesAction.OnDeleteClick(home.id))

        viewModel.onAction(FavoritesAction.OnDismissDelete)

        assertEquals(listOf(home), viewModel.state.value.favorites)
        assertNull(viewModel.state.value.pendingDeleteId)
    }
}
