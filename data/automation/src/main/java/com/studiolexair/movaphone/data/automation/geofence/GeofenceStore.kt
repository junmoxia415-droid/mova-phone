package com.studiolexair.movaphone.data.automation.geofence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studiolexair.movaphone.core.logging.MovaLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda los lugares en el propio teléfono con DataStore.
 *
 * Se eligió DataStore en lugar de una tabla nueva para no obligar a migrar la base de datos:
 * así esta versión se instala encima de la anterior sin tocar los datos del usuario.
 */
private val Context.geofenceDataStore by preferencesDataStore(name = "mova_geofences")

class GeofenceStore(private val context: Context) {

    private val key = stringPreferencesKey("places")

    fun observePlaces(): Flow<List<GeofencePlace>> =
        context.geofenceDataStore.data.map { prefs -> decode(prefs[key].orEmpty()) }

    suspend fun places(): List<GeofencePlace> = decode(context.geofenceDataStore.data.first()[key].orEmpty())

    suspend fun save(place: GeofencePlace): Long {
        val current = places().toMutableList()
        val index = current.indexOfFirst { it.id == place.id }
        val id = if (index >= 0) {
            current[index] = place
            place.id
        } else {
            val newId = nextId(current)
            current.add(place.copy(id = newId))
            newId
        }
        persist(current)
        return id
    }

    suspend fun delete(id: Long) = persist(places().filterNot { it.id == id })

    suspend fun updateState(id: Long, inside: Boolean, checkedAt: Long) = persist(
        places().map { place ->
            if (place.id == id) place.copy(lastInside = inside, lastCheckedAt = checkedAt) else place
        }
    )

    suspend fun clear() = persist(emptyList())

    private suspend fun persist(places: List<GeofencePlace>) {
        context.geofenceDataStore.edit { prefs -> prefs[key] = encode(places) }
    }

    private fun nextId(places: List<GeofencePlace>): Long = (places.maxOfOrNull { it.id } ?: 0L) + 1L

    private fun encode(places: List<GeofencePlace>): String {
        val array = JSONArray()
        places.forEach { place ->
            array.put(
                JSONObject().apply {
                    put("id", place.id)
                    put("name", place.name)
                    put("lat", place.latitude)
                    put("lon", place.longitude)
                    put("radius", place.radiusMeters)
                    put("enabled", place.enabled)
                    place.lastInside?.let { put("inside", it) }
                    put("checkedAt", place.lastCheckedAt)
                    put("createdAt", place.createdAt)
                }
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<GeofencePlace> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                GeofencePlace(
                    id = item.optLong("id"),
                    name = item.optString("name"),
                    latitude = item.optDouble("lat"),
                    longitude = item.optDouble("lon"),
                    radiusMeters = item.optDouble("radius", 200.0),
                    enabled = item.optBoolean("enabled", true),
                    lastInside = if (item.has("inside")) item.optBoolean("inside") else null,
                    lastCheckedAt = item.optLong("checkedAt"),
                    createdAt = item.optLong("createdAt")
                )
            }
        } catch (t: Throwable) {
            MovaLog.e(TAG, "No fue posible leer los lugares guardados", t)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "GeofenceStore"
    }
}
