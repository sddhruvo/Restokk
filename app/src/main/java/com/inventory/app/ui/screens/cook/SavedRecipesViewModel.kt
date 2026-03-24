package com.inventory.app.ui.screens.cook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.repository.CookingLogRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.domain.model.IngredientMatcher
import com.inventory.app.domain.model.RecipeIngredient
import com.inventory.app.domain.model.RecipeStep
import com.inventory.app.domain.model.parseStepsJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── One-shot events ───────────────────────────────────────────────────────

sealed class SavedRecipesEvent {
    data class ShoppingAdded(val count: Int) : SavedRecipesEvent()
}

// ── Display model ─────────────────────────────────────────────────────────

data class SavedRecipeDisplay(
    val entity: SavedRecipeEntity,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStep>,
    val matchPercentage: Int,
    val matchedCount: Int,
    val totalCount: Int,
    val cookCount: Int = 0
)

// ── UI state ──────────────────────────────────────────────────────────────

data class SavedRecipesUiState(
    val recipes: List<SavedRecipeDisplay> = emptyList(),
    val favorites: List<SavedRecipeDisplay> = emptyList(),
    val drafts: List<SavedRecipeDisplay> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val expandedRecipeId: Long? = null,
    val editingNotesId: Long? = null,
    val lastDeletedRecipeId: Long? = null,
    val lastDeletedRecipeName: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────

@HiltViewModel
class SavedRecipesViewModel @Inject constructor(
    private val savedRecipeRepository: SavedRecipeRepository,
    private val itemRepository: ItemRepository,
    private val cookingLogRepository: CookingLogRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedRecipesUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SavedRecipesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var collectionJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadData()
    }

    // ── Load data: combine 3 flows with race-condition guard ──────────────

    private fun loadData() {
        collectionJob?.cancel()

        collectionJob = viewModelScope.launch {
            combine(
                savedRecipeRepository.getActiveNonDrafts(),
                savedRecipeRepository.getDrafts(),
                cookingLogRepository.getCookCountMap(),
                itemRepository.getAllActiveWithDetails()
            ) { recipes, drafts, countMap, inventoryItems ->
                val inventoryNames = inventoryItems.map { it.item.name.lowercase() }
                val recipeDisplays = recipes.map { toDisplay(it, countMap, inventoryNames) }
                val draftDisplays = drafts.map { toDisplay(it, countMap, inventoryNames) }
                val favs = recipeDisplays.filter { it.entity.isFavorite }

                _uiState.update {
                    it.copy(
                        recipes = recipeDisplays,
                        favorites = favs,
                        drafts = draftDisplays,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    // ── Display conversion ────────────────────────────────────────────────

    private fun toDisplay(entity: SavedRecipeEntity, countMap: Map<Long, Int>, inventoryNames: List<String>): SavedRecipeDisplay {
        val rawIngredients: List<RecipeIngredient> = try {
            val type = object : TypeToken<List<RecipeIngredient>>() {}.type
            gson.fromJson(entity.ingredientsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val steps: List<RecipeStep> = parseStepsJson(entity.stepsJson, gson)

        val ingredients = rawIngredients.map { ing ->
            val haveIt = inventoryNames.any { inv ->
                IngredientMatcher.matches(inv, ing.name)
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
            totalCount = total,
            cookCount = countMap[entity.id] ?: 0
        )
    }

    // ── Search ────────────────────────────────────────────────────────────

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
        searchJob?.cancel()
        if (query.isBlank()) {
            loadData()
        } else {
            collectionJob?.cancel()
            searchJob = viewModelScope.launch {
                combine(
                    savedRecipeRepository.search(query),
                    cookingLogRepository.getCookCountMap(),
                    itemRepository.getAllActiveWithDetails()
                ) { results, countMap, inventoryItems ->
                    val inventoryNames = inventoryItems.map { it.item.name.lowercase() }
                    val displays = results.map { toDisplay(it, countMap, inventoryNames) }
                    val favs = displays.filter { it.entity.isFavorite }
                    // Drafts stay pinned regardless of search query
                    _uiState.update { it.copy(recipes = displays, favorites = favs, isLoading = false) }
                }.collect {}
            }
        }
    }

    // ── Standard actions ──────────────────────────────────────────────────

    fun toggleExpanded(recipeId: Long) {
        _uiState.update {
            it.copy(expandedRecipeId = if (it.expandedRecipeId == recipeId) null else recipeId)
        }
    }

    fun toggleFavorite(recipeId: Long) {
        viewModelScope.launch { savedRecipeRepository.toggleFavorite(recipeId) }
    }

    fun deleteRecipe(recipeId: Long) {
        val name = _uiState.value.recipes.find { it.entity.id == recipeId }?.entity?.name
            ?: _uiState.value.drafts.find { it.entity.id == recipeId }?.entity?.name
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
        viewModelScope.launch { savedRecipeRepository.updateNotes(recipeId, notes) }
    }

    fun updateRating(recipeId: Long, rating: Int) {
        viewModelScope.launch { savedRecipeRepository.updateRating(recipeId, rating) }
    }

    fun setEditingNotes(recipeId: Long?) {
        _uiState.update { it.copy(editingNotesId = recipeId) }
    }

    fun getCookAgainSettingsJson(recipe: SavedRecipeDisplay): String? {
        return recipe.entity.sourceSettingsJson
    }

    // ── Add missing ingredients to shopping list ──────────────────────────

    fun addMissingToShoppingList(recipe: SavedRecipeDisplay) {
        val missing = recipe.ingredients.filter { !it.have_it }
        if (missing.isEmpty()) return

        viewModelScope.launch {
            missing.forEach { ing ->
                shoppingListRepository.addItem(
                    ShoppingListItemEntity(
                        customName = ing.name,
                        notes = "For: ${recipe.entity.name}",
                        quantity = 1.0
                    )
                )
            }
            _events.send(SavedRecipesEvent.ShoppingAdded(missing.size))
        }
    }

    // ── Share text formatter ──────────────────────────────────────────────

    fun getShareText(recipe: SavedRecipeDisplay): String {
        return buildString {
            appendLine(recipe.entity.name)

            val meta = listOfNotNull(
                recipe.entity.cuisineOrigin.takeIf { it.isNotBlank() },
                if (recipe.entity.timeMinutes > 0) "${recipe.entity.timeMinutes} min" else null,
                "Serves ${recipe.entity.servings}"
            ).joinToString(" · ")
            if (meta.isNotBlank()) appendLine(meta)

            if (recipe.ingredients.isNotEmpty()) {
                appendLine()
                appendLine("INGREDIENTS")
                recipe.ingredients.forEach { ing ->
                    val amount = "${ing.amount} ${ing.unit}".trim()
                    appendLine("- ${if (amount.isNotBlank()) "$amount " else ""}${ing.name}")
                }
            }

            if (recipe.steps.isNotEmpty()) {
                appendLine()
                appendLine("STEPS")
                recipe.steps.forEachIndexed { i, step ->
                    appendLine("${i + 1}. ${step.instruction}")
                }
            }

            if (!recipe.entity.tips.isNullOrBlank()) {
                appendLine()
                appendLine("TIPS")
                appendLine(recipe.entity.tips)
            }

            appendLine()
            append("— Shared from Restokk")
        }
    }

}
