package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Punto de ubicación registrado (historial opcional y trazabilidad de SOS). */
@Entity(tableName = "location_records", indices = [Index(value = ["recordedAt"])])
data class LocationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val source: String = "manual", // manual | sos | automation | background
    val eventId: Long? = null,
    val recordedAt: Long
)
