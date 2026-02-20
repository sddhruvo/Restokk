package com.inventory.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.StorageLocationEntity
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.StorageLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val items: List<ItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val locations: List<StorageLocationEntity> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: StorageLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(items = emptyList(), categories = emptyList(), locations = emptyList(), hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            try {
                val items = itemRepository.search(query)
                val categories = categoryRepository.search(query)
                val locations = locationRepository.search(query)
                _uiState.update {
                    it.copy(
                        items = items,
                        categories = categories,
                        locations = locations,
                        isSearching = false,
                        hasSearched = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, hasSearched = true) }
            }
        }
    }
}
