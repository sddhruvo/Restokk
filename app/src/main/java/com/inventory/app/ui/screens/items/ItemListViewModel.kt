package com.inventory.app.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.StorageLocationEntity
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.data.repository.UnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ViewMode { GRID, LIST }

data class ItemListUiState(
    val items: List<ItemEntity> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val selectedLocationId: Long? = null,
    val sortBy: String = "updated",
    val viewMode: ViewMode = ViewMode.GRID,
    val categories: List<CategoryEntity> = emptyList(),
    val locations: List<StorageLocationEntity> = emptyList(),
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val unitMap: Map<Long, String> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: StorageLocationRepository,
    private val unitRepository: UnitRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())

    private data class FilterState(
        val search: String? = null,
        val categoryId: Long? = null,
        val locationId: Long? = null,
        val sortBy: String = "updated"
    )

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    private val _searchQuery = MutableStateFlow("")
    private val _selectionMode = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow(emptySet<Long>())

    val uiState = combine(
        _filterState.flatMapLatest { filter ->
            itemRepository.getFiltered(
                search = filter.search,
                categoryId = filter.categoryId,
                locationId = filter.locationId,
                sortBy = filter.sortBy
            )
        },
        categoryRepository.getAllActive(),
        locationRepository.getAllActive(),
        _viewMode,
        combine(_searchQuery, _selectionMode, _selectedIds, unitRepository.getAllActive()) { a, b, c, units ->
            data class Extra(val searchQuery: String, val selectionMode: Boolean, val selectedIds: Set<Long>, val unitMap: Map<Long, String>)
            Extra(a, b, c, units.associate { it.id to it.abbreviation })
        }
    ) { items, categories, locations, viewMode, extra ->
        ItemListUiState(
            items = items,
            isLoading = false,
            searchQuery = extra.searchQuery,
            selectedCategoryId = _filterState.value.categoryId,
            selectedLocationId = _filterState.value.locationId,
            sortBy = _filterState.value.sortBy,
            viewMode = viewMode,
            categories = categories,
            locations = locations,
            selectionMode = extra.selectionMode,
            selectedIds = extra.selectedIds,
            unitMap = extra.unitMap
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ItemListUiState()
    )

    fun updateSearch(query: String) {
        _searchQuery.value = query
        _filterState.update { it.copy(search = query.ifBlank { null }) }
    }

    fun selectCategory(id: Long?) {
        _filterState.update { it.copy(categoryId = id) }
    }

    fun selectLocation(id: Long?) {
        _filterState.update { it.copy(locationId = id) }
    }

    fun updateSort(sortBy: String) {
        _filterState.update { it.copy(sortBy = sortBy) }
    }

    fun toggleViewMode() {
        _viewMode.update { if (it == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { itemRepository.softDelete(id) }
    }

    fun restoreItem(id: Long) {
        viewModelScope.launch { itemRepository.restore(id) }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { itemRepository.toggleFavorite(id) }
    }

    // Batch selection
    fun enterSelectionMode(firstId: Long) {
        _selectionMode.value = true
        _selectedIds.value = setOf(firstId)
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        _selectedIds.update { ids ->
            if (id in ids) ids - id else ids + id
        }
        if (_selectedIds.value.isEmpty()) {
            _selectionMode.value = false
        }
    }

    fun selectAll() {
        _selectedIds.value = uiState.value.items.map { it.id }.toSet()
    }

    fun refresh() {
        // Force re-emission by resetting filter state
        _filterState.update { it.copy() }
    }

    fun deleteSelected() {
        val snapshot = _selectedIds.value.toSet()
        viewModelScope.launch {
            snapshot.forEach { id ->
                itemRepository.softDelete(id)
            }
            _selectedIds.update { current ->
                current - snapshot
            }
            if (_selectedIds.value.isEmpty()) {
                _selectionMode.value = false
            }
        }
    }
}
