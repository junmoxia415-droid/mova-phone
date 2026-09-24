package com.studiolexair.movaphone.feature.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.data.automation.geofence.GeofenceCheck
import com.studiolexair.movaphone.data.automation.geofence.GeofenceMonitor
import com.studiolexair.movaphone.data.automation.geofence.GeofencePlace
import com.studiolexair.movaphone.data.automation.geofence.GeofenceStore
import com.studiolexair.movaphone.data.location.model.LocationResult
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lugares («geovallas») para las automatizaciones de entrada y salida.
 *
 * MOVA calcula la distancia en el propio teléfono, sin Google Play Services: nada de
 * geovallas de Google. El usuario guarda el sitio una vez y las reglas «Llego a un lugar»
 * o «Salgo de un lugar» se disparan de verdad.
 */
class PlacesViewModel(
    private val store: GeofenceStore,
    private val monitor: GeofenceMonitor,
    private val locationRepository: LocationRepositoryImpl
) : ViewModel() {

    val places: StateFlow<List<GeofencePlace>> = store.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val messageState = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = messageState.asStateFlow()

    private val busyState = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = busyState.asStateFlow()

    fun clearMessage() {
        messageState.value = null
    }

    /** Guarda el sitio donde está el usuario ahora mismo. */
    fun addPlaceHere(name: String, radiusMeters: Double) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            messageState.value = "Escribe un nombre para el lugar (por ejemplo «Casa»)."
            return
        }
        viewModelScope.launch {
            busyState.value = true
            when (val result = locationRepository.currentLocation()) {
                is LocationResult.Available -> {
                    val place = GeofencePlace(
                        id = 0,
                        name = cleanName,
                        latitude = result.fix.latitude,
                        longitude = result.fix.longitude,
                        radiusMeters = radiusMeters,
                        createdAt = System.currentTimeMillis()
                    )
                    val id = store.save(place)
                    // Se fija el estado inicial para no disparar nada en el primer momento.
                    monitor.prime(place.copy(id = id))
                    messageState.value = "«$cleanName» guardado. Ya puedes usar las reglas de entrar o salir."
                }
                is LocationResult.Unavailable -> messageState.value = result.reason
            }
            busyState.value = false
        }
    }

    fun toggle(place: GeofencePlace) {
        viewModelScope.launch {
            store.save(place.copy(enabled = !place.enabled, lastInside = null))
            messageState.value = if (place.enabled) "«${place.name}» desactivado." else "«${place.name}» activado."
        }
    }

    fun updateRadius(place: GeofencePlace, radiusMeters: Double) {
        viewModelScope.launch { store.save(place.copy(radiusMeters = radiusMeters, lastInside = null)) }
    }

    fun delete(place: GeofencePlace) {
        viewModelScope.launch {
            store.delete(place.id)
            messageState.value = "«${place.name}» borrado."
        }
    }

    /** Comprueba ahora mismo (sin esperar a la tarea del sistema). */
    fun checkNow() {
        viewModelScope.launch {
            busyState.value = true
            if (places.value.none { it.enabled }) {
                messageState.value = "No hay ningún lugar activo que comprobar."
            } else if (!locationRepository.hasPermission()) {
                messageState.value = "Falta el permiso de ubicación para poder comprobar los lugares."
            } else {
                val checks = runCatching { monitor.check() }.getOrElse { emptyList<GeofenceCheck>() }
                val moved = checks.count { it.transition != com.studiolexair.movaphone.data.automation.geofence.GeofenceTransition.NONE }
                messageState.value = when {
                    checks.isEmpty() -> "No se pudo obtener tu ubicación ahora mismo."
                    moved == 0 -> "Comprobado: sigues en el mismo estado que antes (${checks.size} lugar/es vigilado/s)."
                    else -> "¡Movimiento detectado! Se han disparado $moved automatización/es."
                }
            }
            busyState.value = false
        }
    }

    fun hasLocationPermission(): Boolean = locationRepository.hasPermission()

    companion object {
        fun factory(
            store: GeofenceStore,
            monitor: GeofenceMonitor,
            locationRepository: LocationRepositoryImpl
        ) = viewModelFactory {
            initializer { PlacesViewModel(store, monitor, locationRepository) }
        }
    }
}
