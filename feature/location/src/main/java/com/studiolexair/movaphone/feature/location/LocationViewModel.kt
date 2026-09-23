package com.studiolexair.movaphone.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.database.entity.LocationRecordEntity
import com.studiolexair.movaphone.data.location.model.LocationResult
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocationUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val provider: String? = null,
    val updatedAt: Long = 0L,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val permissionGranted: Boolean = false,
    val isLocationEnabled: Boolean = false
) {
    val hasFix: Boolean get() = latitude != null && longitude != null
    val coordinates: String get() = if (hasFix) TextFormatters.coordinates(latitude!!, longitude!!) else ""
    val mapLink: String get() = if (hasFix) PhoneNumbers.mapsLink(latitude!!, longitude!!) else ""
}

/** Ubicación: coordenadas reales, precisión, última conocida e historial opcional (requisito 15). */
class LocationViewModel(
    private val repository: LocationRepositoryImpl
) : ViewModel() {

    private val state = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = state.asStateFlow()

    val history: StateFlow<List<LocationRecordEntity>> = repository.observeHistory(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshLastKnown()
    }

    fun refreshLastKnown() {
        val last = repository.lastKnown()
        state.value = state.value.copy(
            latitude = last?.latitude,
            longitude = last?.longitude,
            accuracyMeters = last?.accuracyMeters,
            provider = last?.provider,
            updatedAt = last?.timestamp ?: 0L,
            permissionGranted = repository.hasPermission(),
            isLocationEnabled = repository.isLocationEnabled()
        )
    }

    fun requestFreshLocation() {
        state.value = state.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.currentLocation()) {
                is LocationResult.Available -> {
                    val fix = result.fix
                    repository.record(fix, source = "manual")
                    state.value = state.value.copy(
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        accuracyMeters = fix.accuracyMeters,
                        provider = fix.provider,
                        updatedAt = fix.timestamp,
                        loading = false,
                        permissionGranted = repository.hasPermission(),
                        isLocationEnabled = repository.isLocationEnabled()
                    )
                }
                is LocationResult.Unavailable -> state.value = state.value.copy(
                    loading = false,
                    errorMessage = result.reason,
                    permissionGranted = repository.hasPermission(),
                    isLocationEnabled = repository.isLocationEnabled()
                )
            }
        }
    }

    fun shareText(): String = repository.shareText(
        state.value.takeIf { it.hasFix }?.let {
            com.studiolexair.movaphone.data.location.model.LocationFix(
                latitude = it.latitude!!,
                longitude = it.longitude!!,
                accuracyMeters = it.accuracyMeters ?: 0f,
                provider = it.provider ?: "desconocido",
                timestamp = it.updatedAt
            )
        }
    )

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    companion object {
        fun factory(repository: LocationRepositoryImpl) = viewModelFactory {
            initializer { LocationViewModel(repository) }
        }
    }
}
