package com.studiolexair.movaphone.domain.automation.repository

/**
 * Puertos usados por el motor de automatización.
 * Cada uno se implementa fuera del dominio: así el motor no depende de Android
 * ni de módulos de servicio concretos (requisito 18 y 19).
 */
interface AutomationNotifier {
    suspend fun notify(title: String, message: String)
}

interface DrivingModeController {
    fun isDrivingModeActive(): Boolean
    suspend fun setDrivingMode(enabled: Boolean)
}

interface AutomationClock {
    fun now(): Long
    fun hourOfDay(): Int
}

interface BatteryLevelReader {
    fun batteryPercent(): Int?
}
