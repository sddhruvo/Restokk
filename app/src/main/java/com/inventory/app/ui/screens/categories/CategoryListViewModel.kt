package com.inventory.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.CategoryWithItemCountRow
import com.inventory.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryListUiState(
    val categories: List<CategoryWithItemCountRow> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryListUiState())

    val uiState = categoryRepository.getAllWithItemCount()
        .combine(_uiState) { categories, current ->
            current.copy(
                categories = categories,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CategoryListUiState()
        )

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id)
        }
    }

    fun restoreCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.restoreCategory(id)
        }
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        // Atomically read-modify-write to avoid stale snapshot races
        val reorderedPairs = mutableListOf<Pair<Long, Int>>()
        _uiState.update { state ->
            val currentList = state.categories.toMutableList()
            if (fromIndex < 0 || toIndex < 0 || fromIndex >= currentList.size || toIndex >= currentList.size) {
                return@update state
            }
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            reorderedPairs.addAll(currentList.mapIndexed { index, cat -> cat.id to index })
            state.copy(categories = currentList)
        }
        if (reorderedPairs.isNotEmpty()) {
            viewModelScope.launch {
                categoryRepository.updateSortOrders(reorderedPairs)
            }
        }
    }
}
