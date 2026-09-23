package com.studiolexair.movaphone.core.common.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Capacidades reales del dispositivo.
 * Requisito del proyecto: cuando una función no está disponible debe informarse,
 * nunca simularse. Ver docs/COMPATIBILIDAD.md.
 */
data class DeviceCapabilities(
    val hasTelephony: Boolean,
    val hasSms: Boolean,
    val hasLocation: Boolean,
    val hasBiometric: Boolean,
    val hasBluetooth: Boolean,
    val supportsCallScreening: Boolean,
    val supportsCallRecordingApi: Boolean,
    val sdkInt: Int
) {
    val canRecordCalls: Boolean get() = supportsCallRecordingApi
}

object DeviceCapabilitiesReader {

    fun read(context: Context): DeviceCapabilities {
        val pm = context.packageManager
        return DeviceCapabilities(
            hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            hasSms = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
                pm.hasSystemFeature("android.hardware.telephony.messaging"),
            hasLocation = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) ||
                pm.hasSystemFeature(PackageManager.FEATURE_LOCATION),
            hasBiometric = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                pm.hasSystemFeature(PackageManager.FEATURE_IRIS),
            hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            supportsCallScreening = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            // CALL_RECORDING: API pública sólo desde Android 10 y limitada por fabricante
            // en Android 11+. Nunca se sortean restricciones del sistema (ver docs).
            supportsCallRecordingApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            sdkInt = Build.VERSION.SDK_INT
        )
    }
}
