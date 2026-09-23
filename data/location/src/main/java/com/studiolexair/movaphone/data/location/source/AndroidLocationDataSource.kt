package com.studiolexair.movaphone.data.location.source

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.location.model.LocationFix
import com.studiolexair.movaphone.data.location.model.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Ubicación mediante LocationManager (Android SDK).
 * Deliberadamente no depende de Google Play Services ni de Google Maps:
 * el módulo funciona en cualquier dispositivo y admite otros proveedores en el futuro.
 */
class AndroidLocationDataSource(private val context: Context) {

    private val locationManager: LocationManager?
        get() = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean = try {
        locationManager?.isLocationEnabled ?: false
    } catch (t: Throwable) {
        false
    }

    fun lastKnown(): LocationFix? {
        if (!hasPermission()) return null
        val manager = locationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        providers.forEach { provider ->
            try {
                val candidate = manager.getLastKnownLocation(provider)
                if (candidate != null && (best == null || candidate.time > best!!.time)) best = candidate
            } catch (t: SecurityException) {
                MovaLog.w(TAG, "Sin permiso de ubicación para $provider")
            } catch (t: Throwable) {
                MovaLog.w(TAG, "Proveedor $provider no disponible")
            }
        }
        return best?.toFix()
    }

    /** Pide una ubicación fresca con tiempo máximo de espera. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(timeoutMillis: Long = 12_000L): LocationResult {
        if (!hasPermission()) {
            return LocationResult.Unavailable("MOVA Phone no tiene permiso de ubicación.")
        }
        if (!isLocationEnabled()) {
            return LocationResult.Unavailable("La ubicación del dispositivo está desactivada.")
        }
        val manager = locationManager
            ?: return LocationResult.Unavailable("Este dispositivo no tiene servicio de ubicación.")

        val fresh = withTimeoutOrNull(timeoutMillis) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        manager.getCurrentLocation(
                            LocationManager.GPS_PROVIDER,
                            null,
                            context.mainExecutor
                        ) { location ->
                            if (continuation.isActive) continuation.resume(location?.toFix())
                        }
                    } catch (t: Throwable) {
                        MovaLog.e(TAG, "getCurrentLocation falló", t)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.toFix()
            }
        }

        return when {
            fresh != null -> LocationResult.Available(fresh)
            lastKnown() != null -> LocationResult.Available(lastKnown()!!)
            else -> LocationResult.Unavailable("No se pudo obtener la ubicación. Inténtalo al aire libre.")
        }
    }

    private fun Location.toFix(): LocationFix = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else 0f,
        provider = provider ?: "desconocido",
        timestamp = time
    )

    private companion object {
        const val TAG = "AndroidLocation"
    }
}
