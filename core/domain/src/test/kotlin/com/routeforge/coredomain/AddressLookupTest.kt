package com.routeforge.coredomain

import com.routeforge.coredomain.model.AddressLabel
import com.routeforge.coredomain.testing.FakeReverseGeocoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AddressLookupTest {
    private val label = AddressLabel(full = "Pariser Platz 1, Mitte, Berlin", short = "Pariser Platz 1")
    private val geocoder = FakeReverseGeocoder(answer = Result.Success(label))
    private val lookup = AddressLookup(geocoder)

    @Test
    fun `an address found after the pause is delivered once`() =
        runTest {
            val found = mutableListOf<AddressLabel>()

            lookup.start(backgroundScope, latitude = 52.5, longitude = 13.4) { found.add(it) }
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
            runCurrent()

            assertEquals(listOf(label), found)
            assertEquals(listOf(52.5 to 13.4), geocoder.requests)
        }

    @Test
    fun `canceling before the pause ends never asks the geocoder`() =
        runTest {
            val found = mutableListOf<AddressLabel>()
            lookup.start(backgroundScope, latitude = 52.5, longitude = 13.4) { found.add(it) }
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS - 1)

            lookup.cancel()
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS * 2)
            runCurrent()

            assertTrue(geocoder.requests.isEmpty())
            assertTrue(found.isEmpty())
        }

    @Test
    fun `canceling while the geocoder is answering delivers nothing`() =
        runTest {
            val found = mutableListOf<AddressLabel>()
            geocoder.hold()
            lookup.start(backgroundScope, latitude = 52.5, longitude = 13.4) { found.add(it) }
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
            runCurrent()
            assertEquals(1, geocoder.requests.size)

            lookup.cancel()
            geocoder.release()
            runCurrent()

            assertTrue(found.isEmpty())
        }

    @Test
    fun `starting for another spot before the first answers lets only the second deliver`() =
        runTest {
            val first = mutableListOf<AddressLabel>()
            val second = mutableListOf<AddressLabel>()
            geocoder.hold()
            lookup.start(backgroundScope, latitude = 1.0, longitude = 1.0) { first.add(it) }
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
            runCurrent()

            lookup.start(backgroundScope, latitude = 2.0, longitude = 2.0) { second.add(it) }
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
            runCurrent()
            geocoder.release()
            runCurrent()

            assertTrue(first.isEmpty())
            assertEquals(listOf(label), second)
        }

    @Test
    fun `an answer of nothing found delivers nothing`() =
        runTest {
            val found = mutableListOf<AddressLabel>()
            geocoder.answer = Result.Success(null)

            lookup.start(backgroundScope, latitude = 0.0, longitude = -30.0) { found.add(it) }
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
            runCurrent()

            assertEquals(1, geocoder.requests.size)
            assertTrue(found.isEmpty())
        }

    @Test
    fun `a failed lookup delivers nothing and throws nothing`() =
        runTest {
            val found = mutableListOf<AddressLabel>()
            geocoder.answer = Result.Error(DataError.Network.NO_INTERNET)

            lookup.start(backgroundScope, latitude = 52.5, longitude = 13.4) { found.add(it) }
            advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
            runCurrent()

            assertEquals(1, geocoder.requests.size)
            assertTrue(found.isEmpty())
        }

    @Test
    fun `a canceled scope ends the lookup quietly`() =
        runTest {
            val found = mutableListOf<AddressLabel>()
            val scope = CoroutineScope(Job() + StandardTestDispatcher(testScheduler))
            lookup.start(scope, latitude = 52.5, longitude = 13.4) { found.add(it) }

            scope.cancel()
            advanceUntilIdle()

            assertTrue(geocoder.requests.isEmpty())
            assertTrue(found.isEmpty())
        }
}
