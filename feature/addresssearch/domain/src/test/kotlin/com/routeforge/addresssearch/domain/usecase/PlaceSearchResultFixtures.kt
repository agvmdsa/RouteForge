package com.routeforge.addresssearch.domain.usecase

import com.routeforge.coredomain.model.PlaceSearchResult

/** Shared by every test in this package that needs a [PlaceSearchResult] without caring about its
 *  [PlaceSearchResult.formattedAddress] — used to be redeclared identically per test file. */
internal fun place(
    name: String,
    latitude: Double,
    longitude: Double,
    importance: Double = 0.5,
) = PlaceSearchResult(name = name, formattedAddress = name, latitude = latitude, longitude = longitude, importance = importance)
