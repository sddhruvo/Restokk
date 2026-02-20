package com.inventory.app.ui.screens.categories

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

data class SubcategoryFormUiState(
    val name: String = "",
    val description: String = "",
    val nameError: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class SubcategoryFormViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubcategoryFormUiState())
    val uiState = _uiState.asStateFlow()

    private var editingId: Long? = null
    private var parentCategoryId: Long = 0
    private var initialized = false

    fun init(subcategoryId: Long?, categoryId: Long) {
        if (initialized) return
        initialized = true
        parentCategoryId = categoryId
        subcategoryId?.let { id ->
            editingId = id
            viewModelScope.launch {
                categoryRepository.getSubcategoryById(id)?.let { sub ->
                    _uiState.update {
                        it.copy(
                            name = sub.name,
                            description = sub.description ?: ""
                        )
                    }
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

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }

        viewModelScope.launch {
            val id = editingId
            if (id != null) {
                categoryRepository.getSubcategoryById(id)?.let { existing ->
                    categoryRepository.updateSubcategory(
                        existing.copy(
                            name = state.name.trim(),
                            description = state.description.ifBlank { null }
                        )
                    )
                }
            } else {
                categoryRepository.insertSubcategory(
                    SubcategoryEntity(
                        name = state.name.trim(),
                        description = state.description.ifBlank { null },
                        categoryId = parentCategoryId
                    )
                )
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
