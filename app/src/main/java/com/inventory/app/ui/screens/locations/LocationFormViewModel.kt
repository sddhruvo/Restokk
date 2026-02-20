package com.inventory.app.ui.screens.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.StorageLocationEntity
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.domain.model.TemperatureZone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationFormUiState(
    val name: String = "",
    val description: String = "",
    val color: String = "#6c757d",
    val temperatureZone: TemperatureZone? = null,
    val isActive: Boolean = true,
    val nameError: String? = null,
    val isEditing: Boolean = false,
    val editingId: Long? = null,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocationFormViewModel @Inject constructor(
    private val locationRepository: StorageLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationFormUiState())
    val uiState = _uiState.asStateFlow()

    fun loadLocation(id: Long) {
        viewModelScope.launch {
            locationRepository.getById(id)?.let { loc ->
                _uiState.update {
                    it.copy(
                        name = loc.name,
                        description = loc.description ?: "",
                        color = loc.color ?: "#6c757d",
                        temperatureZone = TemperatureZone.fromValue(loc.temperatureZone),
                        isActive = loc.isActive,
                        isEditing = true,
                        editingId = loc.id
                    )
                }
            }
        }
    }

    fun updateName(value: String) { _uiState.update { it.copy(name = value, nameError = null) } }
    fun updateDescription(value: String) { _uiState.update { it.copy(description = value) } }
    fun updateColor(value: String) { _uiState.update { it.copy(color = value) } }
    fun updateTemperatureZone(value: TemperatureZone?) { _uiState.update { it.copy(temperatureZone = value) } }
    fun updateIsActive(value: Boolean) { _uiState.update { it.copy(isActive = value) } }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }

        viewModelScope.launch {
            try {
                if (state.isEditing && state.editingId != null) {
                    locationRepository.getById(state.editingId)?.let { existing ->
                        locationRepository.update(
                            existing.copy(
                                name = state.name.trim(),
                                description = state.description.ifBlank { null },
                                color = state.color.ifBlank { null },
                                temperatureZone = state.temperatureZone?.value,
                                isActive = state.isActive
                            )
                        )
                    }
                } else {
                    locationRepository.insert(
                        StorageLocationEntity(
                            name = state.name.trim(),
                            description = state.description.ifBlank { null },
                            color = state.color.ifBlank { null },
                            temperatureZone = state.temperatureZone?.value,
                            isActive = state.isActive
                        )
                    )
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save location: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
