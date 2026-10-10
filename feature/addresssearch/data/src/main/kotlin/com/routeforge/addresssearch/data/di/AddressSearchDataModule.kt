package com.routeforge.addresssearch.data.di

import com.routeforge.addresssearch.data.NominatimGeocodeClient
import com.routeforge.addresssearch.domain.GeocodeClient
import org.koin.dsl.bind
import org.koin.dsl.module

val addressSearchDataModule =
    module {
        single { NominatimGeocodeClient(httpClient = get()) }.bind<GeocodeClient>()
    }
