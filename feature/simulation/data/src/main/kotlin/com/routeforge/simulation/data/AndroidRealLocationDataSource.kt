package com.routeforge.simulation.data

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.routeforge.coredomain.model.RealLocation
import com.routeforge.simulation.domain.RealLocationDataSource
import com.routeforge.simulation.domain.RealLocationFailure
import com.routeforge.simulation.domain.RealLocationUpdate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val LOCATION_UPDATE_INTERVAL_MILLIS = 5_000L

/** GPS alone needs a clear sky view and can never lock indoors — NETWORK_PROVIDER (Wi-Fi/cell
 *  based) is raced alongside it so a fix is still possible indoors or without a SIM. */
private val realLocationProviders = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

class AndroidRealLocationDataSource(
    private val context: Context,
) : RealLocationDataSource {
    override fun observeLocation(): Flow<RealLocationUpdate> =
        callbackFlow {
            val locationManager = context.getSystemService(LocationManager::class.java)
            val enabledProviders = realLocationProviders.filter { locationManager.isProviderEnabled(it) }
            if (enabledProviders.isEmpty()) {
                trySend(RealLocationUpdate.Unavailable(RealLocationFailure.ProviderDisabled))
                close()
                return@callbackFlow
            }

            enabledProviders.forEach { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)?.let { lastKnownLocation ->
                        if (!LocationCompat.isMock(lastKnownLocation)) {
                            trySend(RealLocationUpdate.Fix(RealLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)))
                        }
                    }
                } catch (_: SecurityException) {
                }
            }

            val listener =
                LocationListenerCompat { location: Location ->
                    if (!LocationCompat.isMock(location)) {
                        trySend(RealLocationUpdate.Fix(RealLocation(location.latitude, location.longitude)))
                    }
                }

            val registeredAnyProvider =
                enabledProviders.fold(false) { registeredSoFar, provider ->
                    try {
                        LocationManagerCompat.requestLocationUpdates(
                            locationManager,
                            provider,
                            LocationRequestCompat.Builder(LOCATION_UPDATE_INTERVAL_MILLIS).build(),
                            ContextCompat.getMainExecutor(context),
                            listener,
                        )
                        true
                    } catch (_: SecurityException) {
                        registeredSoFar
                    }
                }

            if (!registeredAnyProvider) {
                trySend(RealLocationUpdate.Unavailable(RealLocationFailure.PermissionDenied))
                close()
                return@callbackFlow
            }

            awaitClose { LocationManagerCompat.removeUpdates(locationManager, listener) }
        }
}
