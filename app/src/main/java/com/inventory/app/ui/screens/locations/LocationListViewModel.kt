package com.inventory.app.ui.screens.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.LocationWithItemCountRow
import com.inventory.app.data.repository.StorageLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationListUiState(
    val locations: List<LocationWithItemCountRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LocationListViewModel @Inject constructor(
    private val locationRepository: StorageLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationListUiState())

    val uiState = locationRepository.getAllWithItemCount()
        .combine(_uiState) { locations, current ->
            current.copy(
                locations = locations,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            LocationListUiState()
        )

    fun deleteLocation(id: Long) {
        viewModelScope.launch {
            try {
                locationRepository.delete(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete location: ${e.message}") }
            }
        }
    }

    fun restoreLocation(id: Long) {
        viewModelScope.launch {
            try {
                locationRepository.restore(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to restore location: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun moveLocation(fromIndex: Int, toIndex: Int) {
        // Atomically read-modify-write to avoid stale snapshot races
        val reorderedPairs = mutableListOf<Pair<Long, Int>>()
        _uiState.update { state ->
            val currentList = state.locations.toMutableList()
            if (fromIndex < 0 || toIndex < 0 || fromIndex >= currentList.size || toIndex >= currentList.size) {
                return@update state
            }
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            reorderedPairs.addAll(currentList.mapIndexed { index, loc -> loc.id to index })
            state.copy(locations = currentList)
        }
        if (reorderedPairs.isNotEmpty()) {
            viewModelScope.launch {
                locationRepository.updateSortOrders(reorderedPairs)
            }
        }
    }
}
