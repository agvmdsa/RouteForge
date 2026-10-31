package com.routeforge.addresssearch.data.di

import com.routeforge.addresssearch.data.NominatimGeocodeClient
import com.routeforge.addresssearch.data.NominatimReverseGeocoder
import com.routeforge.addresssearch.data.RequestSpacing
import com.routeforge.addresssearch.domain.GeocodeClient
import com.routeforge.coredomain.ReverseGeocoder
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.TimeSource

val addressSearchDataModule =
    module {
        single { NominatimGeocodeClient(httpClient = get()) }.bind<GeocodeClient>()
        single<TimeSource> { TimeSource.Monotonic }
        single { RequestSpacing(timeSource = get()) }
        single { NominatimReverseGeocoder(httpClient = get(), spacing = get()) }.bind<ReverseGeocoder>()
    }
