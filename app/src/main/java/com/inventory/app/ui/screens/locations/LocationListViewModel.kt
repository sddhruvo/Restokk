package com.inventory.app.ui.screens.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.LocationWithItemCountRow
import com.inventory.app.data.repository.StorageLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationListUiState(
    val locations: List<LocationWithItemCountRow> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class LocationListViewModel @Inject constructor(
    private val locationRepository: StorageLocationRepository
) : ViewModel() {

    val uiState = locationRepository.getAllWithItemCount()
        .map { locations ->
            LocationListUiState(
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
            locationRepository.delete(id)
        }
    }

    fun moveLocation(fromIndex: Int, toIndex: Int) {
        val currentList = uiState.value.locations.toMutableList()
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= currentList.size || toIndex >= currentList.size) return
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        viewModelScope.launch {
            locationRepository.updateSortOrders(
                currentList.mapIndexed { index, loc -> loc.id to index }
            )
        }
    }
}
