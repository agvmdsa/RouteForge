package com.routeforge.simulation.data

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.SystemClock
import com.routeforge.simulation.domain.MockLocationPublisher

private val mockProviders = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

class AndroidMockLocationPublisher(
    private val context: Context,
) : MockLocationPublisher {
    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var providersRegistered = false

    override fun publish(
        latitude: Double,
        longitude: Double,
        bearingDegrees: Float,
        speedMetersPerSecond: Float,
    ) {
        ensureProvidersRegistered()
        val timestamp = System.currentTimeMillis()
        val elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        mockProviders.forEach { provider ->
            val location =
                Location(provider).apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    this.bearing = bearingDegrees
                    this.speed = speedMetersPerSecond
                    this.accuracy = 5f
                    this.time = timestamp
                    this.elapsedRealtimeNanos = elapsedRealtimeNanos
                }
            locationManager.setTestProviderLocation(provider, location)
        }
    }

    override fun clear() {
        if (!providersRegistered) return
        mockProviders.forEach { provider ->
            runCatching { locationManager.removeTestProvider(provider) }
        }
        providersRegistered = false
    }

    @Suppress("DEPRECATION")
    private fun ensureProvidersRegistered() {
        if (providersRegistered) return
        mockProviders.forEach { provider ->
            locationManager.addTestProvider(
                provider,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE,
            )
            locationManager.setTestProviderEnabled(provider, true)
        }
        providersRegistered = true
    }
}
