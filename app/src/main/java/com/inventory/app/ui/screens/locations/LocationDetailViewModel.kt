package com.inventory.app.ui.screens.locations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.StorageLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationDetailUiState(
    val locationName: String? = null,
    val description: String? = null,
    val temperatureZone: String? = null,
    val items: List<ItemWithDetails> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val locationRepository: StorageLocationRepository,
    private val itemDao: ItemDao
) : ViewModel() {

    private val locationId: Long = savedStateHandle["locationId"] ?: 0L
    private val _uiState = MutableStateFlow(LocationDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (locationId == 0L) {
            _uiState.update { it.copy(isLoading = false, error = "Location not found") }
        } else {
            viewModelScope.launch {
                try {
                    locationRepository.getById(locationId)?.let { loc ->
                        _uiState.update {
                            it.copy(
                                locationName = loc.name,
                                description = loc.description,
                                temperatureZone = loc.temperatureZone
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed to load location: ${e.message}", isLoading = false) }
                }
            }
            viewModelScope.launch {
                try {
                    itemDao.getByLocation(locationId).collect { items ->
                        _uiState.update { it.copy(items = items, isLoading = false) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed to load items: ${e.message}", isLoading = false) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
