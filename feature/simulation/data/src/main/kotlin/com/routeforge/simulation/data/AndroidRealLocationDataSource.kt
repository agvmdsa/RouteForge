package com.routeforge.simulation.data

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.routeforge.simulation.domain.RealLocationDataSource
import com.routeforge.simulation.domain.model.RealLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val LOCATION_UPDATE_INTERVAL_MILLIS = 5_000L

class AndroidRealLocationDataSource(
    private val context: Context,
) : RealLocationDataSource {
    override fun observeLocation(): Flow<RealLocation> =
        callbackFlow {
            val locationManager = context.getSystemService(LocationManager::class.java)
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                close()
                return@callbackFlow
            }

            val listener =
                LocationListenerCompat { location: Location ->
                    if (!LocationCompat.isMock(location)) {
                        trySend(RealLocation(location.latitude, location.longitude))
                    }
                }

            try {
                LocationManagerCompat.requestLocationUpdates(
                    locationManager,
                    LocationManager.GPS_PROVIDER,
                    LocationRequestCompat.Builder(LOCATION_UPDATE_INTERVAL_MILLIS).build(),
                    ContextCompat.getMainExecutor(context),
                    listener,
                )
            } catch (_: SecurityException) {
                close()
            }

            awaitClose { LocationManagerCompat.removeUpdates(locationManager, listener) }
        }
}
