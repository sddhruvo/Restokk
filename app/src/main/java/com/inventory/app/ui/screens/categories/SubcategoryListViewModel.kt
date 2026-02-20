package com.inventory.app.ui.screens.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.SubcategoryEntity
import com.inventory.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubcategoryListUiState(
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val categoryName: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SubcategoryListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle["categoryId"] ?: 0L
    private val _uiState = MutableStateFlow(SubcategoryListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getById(categoryId)?.let { cat ->
                _uiState.update { it.copy(categoryName = cat.name) }
            }
        }
        viewModelScope.launch {
            categoryRepository.getSubcategories(categoryId).collect { subs ->
                _uiState.update {
                    it.copy(subcategories = subs, isLoading = false)
                }
            }
        }
    }

    fun deleteSubcategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.deleteSubcategory(id)
        }
    }
}
