package com.inventory.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryFormUiState(
    val name: String = "",
    val description: String = "",
    val icon: String = "category",
    val color: String = "#6c757d",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val nameError: String? = null,
    val nameErrorTrigger: Int = 0,
    val isEditing: Boolean = false,
    val editingId: Long? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryFormUiState())
    val uiState = _uiState.asStateFlow()

    fun loadCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.getById(id)?.let { cat ->
                _uiState.update {
                    it.copy(
                        name = cat.name,
                        description = cat.description ?: "",
                        icon = cat.icon ?: "category",
                        color = cat.color ?: "#6c757d",
                        sortOrder = cat.sortOrder,
                        isActive = cat.isActive,
                        isEditing = true,
                        editingId = cat.id
                    )
                }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun updateIcon(value: String) {
        _uiState.update { it.copy(icon = value) }
    }

    fun updateColor(value: String) {
        _uiState.update { it.copy(color = value) }
    }

    fun updateSortOrder(value: Int) {
        _uiState.update { it.copy(sortOrder = value) }
    }

    fun updateIsActive(value: Boolean) {
        _uiState.update { it.copy(isActive = value) }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required", nameErrorTrigger = it.nameErrorTrigger + 1) }
            return
        }

        viewModelScope.launch {
            try {
                if (state.isEditing && state.editingId != null) {
                    categoryRepository.getById(state.editingId)?.let { existing ->
                        categoryRepository.updateCategory(
                            existing.copy(
                                name = state.name.trim(),
                                description = state.description.ifBlank { null },
                                icon = state.icon,
                                color = state.color.ifBlank { null },
                                sortOrder = state.sortOrder,
                                isActive = state.isActive
                            )
                        )
                    }
                } else {
                    categoryRepository.insertCategory(
                        CategoryEntity(
                            name = state.name.trim(),
                            description = state.description.ifBlank { null },
                            icon = state.icon,
                            color = state.color.ifBlank { null },
                            sortOrder = state.sortOrder,
                            isActive = state.isActive
                        )
                    )
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(nameError = "Failed to save: ${e.message}") }
            }
        }
    }
}
