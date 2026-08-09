package com.nexasense.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.CancellationSignal
import com.nexasense.core.logging.NexaLogger
import com.nexasense.core.permissions.PermissionChecker
import com.nexasense.domain.port.LocationPoint
import com.nexasense.domain.port.LocationProvider
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Location provider built on the platform LocationManager (no Play Services).
 * Used exclusively by the True North feature; returns null when there is no
 * fix or no permission, and never fabricates a location.
 */
@SuppressLint("MissingPermission")
class LocationProviderImpl(context: Context) : LocationProvider {

    private val appContext: Context = context.applicationContext

    private val locationManager: LocationManager? =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "NexaSense-location").apply { isDaemon = true }
    }

    override suspend fun lastKnownLocation(): LocationPoint? = withContext(Dispatchers.IO) {
        val manager = locationManager ?: return@withContext null
        if (!PermissionChecker.hasLocationPermission(appContext)) return@withContext null
        manager.allProviders
            .asSequence()
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.toPoint()
    }

    override suspend fun requestCurrentLocation(timeoutMillis: Long): LocationPoint? =
        withContext(Dispatchers.IO) {
            val manager = locationManager ?: return@withContext null
            if (!PermissionChecker.hasLocationPermission(appContext)) return@withContext null
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            for (provider in providers) {
                if (!runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)) continue
                val fix = requestOnce(manager, provider, timeoutMillis)
                if (fix != null) return@withContext fix
            }
            lastKnownLocation()
        }

    private suspend fun requestOnce(
        manager: LocationManager,
        provider: String,
        timeoutMillis: Long,
    ): LocationPoint? {
        val signal = CancellationSignal()
        val result = try {
            withTimeout(timeoutMillis) {
                suspendCancellableCoroutine<LocationPoint?> { cont ->
                    cont.invokeOnCancellation { signal.cancel() }
                    try {
                        manager.getCurrentLocation(
                            provider,
                            signal,
                            executor,
                            Consumer<Location?> { location ->
                                if (cont.isActive) {
                                    cont.resume(location?.toPoint())
                                }
                            },
                        )
                    } catch (t: Throwable) {
                        NexaLogger.w("Location request failed for $provider: ${t.message}")
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        } catch (t: Throwable) {
            signal.cancel()
            null
        }
        return result
    }

    override fun locationUpdates(
        minDistanceMeters: Float,
        minIntervalMillis: Long,
    ): Flow<LocationPoint> = callbackFlow {
        val manager = locationManager
        if (manager == null || !PermissionChecker.hasLocationPermission(appContext)) {
            close()
            return@callbackFlow
        }
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toPoint())
            }
        }
        for (provider in providers) {
            try {
                manager.requestLocationUpdates(
                    provider,
                    minIntervalMillis,
                    minDistanceMeters,
                    executor,
                    listener,
                )
            } catch (t: Throwable) {
                NexaLogger.w("Location updates failed for $provider: ${t.message}")
            }
        }
        awaitClose {
            try {
                manager.removeUpdates(listener)
            } catch (t: Throwable) {
                NexaLogger.w("Failed to stop location updates: ${t.message}")
            }
        }
    }

    private fun Location.toPoint(): LocationPoint = LocationPoint(
        latitudeDegrees = latitude,
        longitudeDegrees = longitude,
        altitudeMeters = if (hasAltitude()) altitude else 0.0,
        timeMillis = if (time > 0) time else System.currentTimeMillis(),
        accuracyMeters = if (hasAccuracy()) accuracy else null,
    )
}
