package com.routeforge.addresssearch.presentation.di

import com.routeforge.addresssearch.domain.usecase.RankSearchResultsUseCase
import com.routeforge.addresssearch.domain.usecase.SearchPlacesUseCase
import com.routeforge.addresssearch.presentation.AddressSearchViewModel
import com.routeforge.addresssearch.presentation.SearchSourceContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val addressSearchPresentationModule =
    module {
        single { RankSearchResultsUseCase() }
        single { SearchPlacesUseCase(geocodeClient = get(), rankSearchResults = get(), lastKnownRealLocationHolder = get()) }
        viewModel { (sourceContext: SearchSourceContext, referenceLatitude: Double?, referenceLongitude: Double?) ->
            AddressSearchViewModel(
                sourceContext = sourceContext,
                referenceLatitude = referenceLatitude,
                referenceLongitude = referenceLongitude,
                searchPlaces = get(),
                pendingSearchWaypointHolder = get(),
                pendingTeleportTargetHolder = get(),
                favoriteWaypointsRepository = get(),
            )
        }
    }
