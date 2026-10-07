package com.routeforge.routing.presentation.saved

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.routeforge.routing.presentation.R
import com.routeforge.routing.presentation.favorites.FavoritesRoot
import com.routeforge.routing.presentation.savedroutes.SavedRoutesRoot

private enum class SavedSubSection { ROUTES, FAVORITES }

/** The Saved tab's root screen: a secondary [TabRow] switching between the existing "Routes" and
 *  "Favorites" screens, reused as-is. [onGoToSimulate] fires when the user taps "use" on a saved
 *  route or "mock here" on a favorite (both today's `onDone`) — both are already explicit actions,
 *  so they switch straight to the Simulate tab with no extra confirmation (unlike US2's automatic
 *  route-planning hand-off, which specifically needed one).
 *
 *  [selectedSection] is a plain `rememberSaveable` — because this composable is the Saved tab's own
 *  nested-graph root, Foundational's `saveState`/`restoreState` tab-switch wiring already preserves
 *  it across tab switches for free (FR-004); "Routes" is only the default the very first time this
 *  screen is composed in a session. */
@Composable
fun SavedRoot(
    onGoToSimulate: () -> Unit,
    onOpenRegionCatalog: () -> Unit,
) {
    var selectedSection by rememberSaveable { mutableStateOf(SavedSubSection.ROUTES) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSection.ordinal) {
            Tab(
                selected = selectedSection == SavedSubSection.ROUTES,
                onClick = { selectedSection = SavedSubSection.ROUTES },
                text = { Text(stringResource(R.string.saved_section_routes_label)) },
            )
            Tab(
                selected = selectedSection == SavedSubSection.FAVORITES,
                onClick = { selectedSection = SavedSubSection.FAVORITES },
                text = { Text(stringResource(R.string.saved_section_favorites_label)) },
            )
        }
        when (selectedSection) {
            SavedSubSection.ROUTES ->
                SavedRoutesRoot(
                    onDone = onGoToSimulate,
                    onOpenRegionCatalog = onOpenRegionCatalog,
                )
            SavedSubSection.FAVORITES ->
                FavoritesRoot(
                    isPickerMode = false,
                    onDone = onGoToSimulate,
                )
        }
    }
}
