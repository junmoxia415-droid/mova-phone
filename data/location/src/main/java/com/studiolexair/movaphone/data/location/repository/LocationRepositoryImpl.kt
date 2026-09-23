package com.studiolexair.movaphone.data.location.repository

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.database.dao.LocationDao
import com.studiolexair.movaphone.core.database.entity.LocationRecordEntity
import com.studiolexair.movaphone.data.location.model.LocationFix
import com.studiolexair.movaphone.data.location.model.LocationResult
import com.studiolexair.movaphone.data.location.source.AndroidLocationDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de ubicación: obtiene posiciones reales, las guarda si el usuario
 * habilita el historial y genera el texto/enlace para compartir.
 */
class LocationRepositoryImpl(
    private val locationSource: AndroidLocationDataSource,
    private val locationDao: LocationDao,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun currentLocation(): LocationResult = locationSource.currentLocation()

    fun lastKnown(): LocationFix? = locationSource.lastKnown()

    fun hasPermission(): Boolean = locationSource.hasPermission()

    fun isLocationEnabled(): Boolean = locationSource.isLocationEnabled()

    fun observeHistory(limit: Int = 50): Flow<List<LocationRecordEntity>> = locationDao.observeRecent(limit)

    fun observeSosHistory(limit: Int = 20): Flow<List<LocationRecordEntity>> =
        locationDao.observeBySource("sos", limit)

    suspend fun lastRecorded(): LocationRecordEntity? = locationDao.lastKnown()

    suspend fun record(fix: LocationFix, source: String, eventId: Long? = null): Long = locationDao.insert(
        LocationRecordEntity(
            latitude = fix.latitude,
            longitude = fix.longitude,
            accuracyMeters = fix.accuracyMeters,
            provider = fix.provider,
            source = source,
            eventId = eventId,
            recordedAt = fix.timestamp.takeIf { it > 0 } ?: timeProvider()
        )
    )

    suspend fun purgeHistory(before: Long) = locationDao.purgeBefore(before)

    suspend fun clearHistory() = locationDao.clear()

    /** Texto listo para compartir por SMS u otra app. */
    fun shareText(fix: LocationFix?): String = if (fix == null) {
        "No hay ubicación disponible en este momento."
    } else {
        """Mi ubicación actual (MOVA Phone):
${TextFormatters.coordinates(fix.latitude, fix.longitude)}
Precisión aproximada: ${String.format("%.0f", fix.accuracyMeters)} m
Ver en el mapa: ${PhoneNumbers.mapsLink(fix.latitude, fix.longitude)}"""
    }
}
