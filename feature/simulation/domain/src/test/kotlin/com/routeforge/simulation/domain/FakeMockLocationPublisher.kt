package com.routeforge.simulation.domain

class FakeMockLocationPublisher : MockLocationPublisher {
    data class PublishedFix(
        val latitude: Double,
        val longitude: Double,
        val bearingDegrees: Float,
        val speedMetersPerSecond: Float,
    )

    val publishedFixes = mutableListOf<PublishedFix>()
    var clearCallCount = 0
        private set

    override fun publish(
        latitude: Double,
        longitude: Double,
        bearingDegrees: Float,
        speedMetersPerSecond: Float,
    ) {
        publishedFixes.add(PublishedFix(latitude, longitude, bearingDegrees, speedMetersPerSecond))
    }

    override fun clear() {
        clearCallCount++
    }
}
