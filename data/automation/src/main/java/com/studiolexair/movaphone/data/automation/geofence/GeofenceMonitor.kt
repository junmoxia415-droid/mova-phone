package com.studiolexair.movaphone.data.automation.geofence

import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import com.studiolexair.movaphone.domain.automation.repository.TriggerPayload

/**
 * Comprueba los lugares guardados y lanza el automatismo al entrar o salir.
 *
 * Se llama en tres momentos para que de verdad se dispare:
 *  1. cada vez que MOVA recibe una posición nueva (con la app en marcha o en segundo plano),
 *  2. por la tarea periódica del sistema (aunque la app esté cerrada),
 *  3. al abrir la app, para recuperar el estado si el teléfono estuvo sin cobertura.
 *
 * No usa Google Play Services: sólo la ubicación del propio teléfono y este cálculo local.
 */
class GeofenceMonitor(
    private val store: GeofenceStore,
    private val engine: AutomationEngine,
    private val location: suspend () -> Pair<Double, Double>?,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    /** Comprueba todos los lugares. Devuelve las transiciones que ha habido. */
    suspend fun check(): List<GeofenceCheck> {
        val places = store.places().filter { it.enabled }
        if (places.isEmpty()) return emptyList()

        val fix = location()
        if (fix == null) {
            MovaLog.i(TAG, "Sin ubicación disponible: los lugares se comprobarán más tarde")
            return emptyList()
        }
        val (latitude, longitude) = fix

        val results = places.map { place ->
            val check = GeofenceEvaluator.evaluate(place, latitude, longitude)
            store.updateState(place.id, check.inside, clock())
            when (check.transition) {
                GeofenceTransition.ENTER -> fire(TriggerType.LOCATION_ENTER, place)
                GeofenceTransition.EXIT -> fire(TriggerType.LOCATION_EXIT, place)
                GeofenceTransition.NONE -> Unit
            }
            check
        }

        MovaLog.i(
            TAG,
            "Lugares comprobados: ${results.size}, entradas/salidas: ${results.count { it.transition != GeofenceTransition.NONE }}"
        )
        return results
    }

    /** Comprueba un lugar concreto (útil al crearlo, para fijar el estado inicial). */
    suspend fun prime(place: GeofencePlace) {
        val fix = location() ?: return
        val check = GeofenceEvaluator.evaluate(place, fix.first, fix.second)
        store.updateState(place.id, check.inside, clock())
    }

    private suspend fun fire(type: TriggerType, place: GeofencePlace) {
        MovaLog.i(TAG, "Disparador $type en el lugar «${place.name}»")
        engine.onTrigger(
            type,
            TriggerPayload(
                contactName = place.name,
                message = place.name,
                hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            )
        )
    }

    private companion object {
        const val TAG = "GeofenceMonitor"
    }
}

/** Punto de encuentro para que la tarea en segundo plano encuentre el monitor. */
object GeofenceServiceDependencies {
    @Volatile
    var monitor: GeofenceMonitor? = null
}
