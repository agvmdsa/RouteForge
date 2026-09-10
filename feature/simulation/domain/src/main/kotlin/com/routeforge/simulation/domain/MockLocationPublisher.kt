package com.routeforge.simulation.domain

interface MockLocationPublisher {
    fun publish(
        latitude: Double,
        longitude: Double,
        bearingDegrees: Float,
        speedMetersPerSecond: Float,
    )

    fun clear()
}
