package com.inventory.app.ui.screens.cook

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SavedRecipeDisplay(
    val entity: SavedRecipeEntity,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val matchPercentage: Int,
    val matchedCount: Int,
    val totalCount: Int
)

data class SavedRecipesUiState(
    val recipes: List<SavedRecipeDisplay> = emptyList(),
    val favorites: List<SavedRecipeDisplay> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val expandedRecipeId: Long? = null,
    val editingNotesId: Long? = null,
    val lastDeletedRecipeId: Long? = null,
    val lastDeletedRecipeName: String? = null
)

@HiltViewModel
class SavedRecipesViewModel @Inject constructor(
    private val savedRecipeRepository: SavedRecipeRepository,
    private val itemRepository: ItemRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedRecipesUiState())
    val uiState = _uiState.asStateFlow()

    private var currentInventoryNames: List<String> = emptyList()
    private var collectionJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            // Load inventory names for match calculation
            try {
                val items = itemRepository.getAllActiveWithDetails().first()
                currentInventoryNames = items.map { it.item.name.lowercase() }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load inventory: ${e.message}")
            }

            // Observe saved recipes
            savedRecipeRepository.getAllActive().collect { entities ->
                val displays = entities.map { toDisplay(it) }
                val favs = displays.filter { it.entity.isFavorite }
                _uiState.update { it.copy(recipes = displays, favorites = favs) }
            }
        }
    }

    private fun toDisplay(entity: SavedRecipeEntity): SavedRecipeDisplay {
        val rawIngredients: List<RecipeIngredient> = try {
            val type = object : TypeToken<List<RecipeIngredient>>() {}.type
            gson.fromJson(entity.ingredientsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val steps: List<String> = try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(entity.stepsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        // Live-match each ingredient against current inventory (word-boundary matching)
        val ingredients = rawIngredients.map { ing ->
            val haveIt = currentInventoryNames.any { inv ->
                CookViewModel.ingredientMatch(inv, ing.name)
            }
            ing.copy(have_it = haveIt)
        }
        val matched = ingredients.count { it.have_it }
        val total = ingredients.size
        val pct = if (total > 0) (matched * 100) / total else 0

        return SavedRecipeDisplay(
            entity = entity,
            ingredients = ingredients,
            steps = steps,
            matchPercentage = pct,
            matchedCount = matched,
            totalCount = total
        )
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
        searchJob?.cancel()
        if (query.isBlank()) {
            loadData()
        } else {
            collectionJob?.cancel()
            searchJob = viewModelScope.launch {
                savedRecipeRepository.search(query).collect { entities ->
                    val displays = entities.map { toDisplay(it) }
                    val favs = displays.filter { it.entity.isFavorite }
                    _uiState.update { it.copy(recipes = displays, favorites = favs) }
                }
            }
        }
    }

    fun toggleExpanded(recipeId: Long) {
        _uiState.update {
            it.copy(expandedRecipeId = if (it.expandedRecipeId == recipeId) null else recipeId)
        }
    }

    fun toggleFavorite(recipeId: Long) {
        viewModelScope.launch {
            savedRecipeRepository.toggleFavorite(recipeId)
        }
    }

    fun deleteRecipe(recipeId: Long) {
        // Find name before deleting (for undo snackbar)
        val name = _uiState.value.recipes.find { it.entity.id == recipeId }?.entity?.name
        viewModelScope.launch {
            savedRecipeRepository.softDelete(recipeId)
            _uiState.update { it.copy(lastDeletedRecipeId = recipeId, lastDeletedRecipeName = name) }
        }
    }

    fun restoreRecipe(recipeId: Long) {
        viewModelScope.launch {
            savedRecipeRepository.restore(recipeId)
            _uiState.update { it.copy(lastDeletedRecipeId = null, lastDeletedRecipeName = null) }
        }
    }

    fun clearDeletedState() {
        _uiState.update { it.copy(lastDeletedRecipeId = null, lastDeletedRecipeName = null) }
    }

    fun updateNotes(recipeId: Long, notes: String?) {
        viewModelScope.launch {
            savedRecipeRepository.updateNotes(recipeId, notes)
        }
    }

    fun updateRating(recipeId: Long, rating: Int) {
        viewModelScope.launch {
            savedRecipeRepository.updateRating(recipeId, rating)
        }
    }

    fun setEditingNotes(recipeId: Long?) {
        _uiState.update { it.copy(editingNotesId = recipeId) }
    }

    fun getCookAgainSettingsJson(recipe: SavedRecipeDisplay): String? {
        return recipe.entity.sourceSettingsJson
    }

    companion object {
        private const val TAG = "SavedRecipesVM"
    }
}
