package com.inventory.app.ui.screens.cook

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.domain.model.CuisineData
import com.inventory.app.domain.model.RegionalCuisine
import com.inventory.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ── Enums for structured input ─────────────────────────────────────────

enum class MealMood(val label: String, val emoji: String, val aiHint: String) {
    QUICK("Quick & Easy", "⚡", "Quick, minimal steps, under 20 minutes prep"),
    COMFORT("Comfort Food", "🍲", "Hearty, warming, nostalgic, soul-satisfying home cooking"),
    IMPRESS("Impress Someone", "✨", "Elevated presentation, restaurant-quality plating, refined flavors"),
    HEALTHY("Healthy & Light", "🥗", "Low-calorie, fresh, balanced, nutritious"),
    USE_UP("Use What's Expiring", "⏳", "Prioritize items nearest expiry date"),
    KID_FRIENDLY("Kid Friendly", "⭐", "Simple flavors, fun presentation, mild spice, familiar textures"),
    SURPRISE("Surprise Me", "🎲", "Random cuisine and style — full culinary adventure")
}

enum class SpiceLevel(val label: String, val aiHint: String) {
    MILD("Mild", "No heat, gentle spices only"),
    MEDIUM("Medium", "Moderate warmth, balanced heat"),
    HOT("Hot", "Spicy, bold heat, chili-forward")
}

enum class EffortLevel(val label: String, val minutes: String, val aiHint: String) {
    QUICK("Quick", "~15 min", "Under 15 minutes, minimal cooking"),
    STANDARD("Standard", "~45 min", "Around 30-45 minutes, moderate effort"),
    ELABORATE("Elaborate", "1hr+", "Over an hour, multi-step, worth the effort")
}

enum class StyleLevel(val label: String, val aiHint: String) {
    CASUAL("Casual", "Everyday home cooking, simple plating"),
    ELEVATED("Elevated", "Above-average presentation, a step up from everyday"),
    FINE_DINING("Fine Dining", "Restaurant-quality, refined technique and plating")
}

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    DESSERT("Dessert")
}

enum class DietaryFilter(val label: String) {
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    HALAL("Halal"),
    KOSHER("Kosher"),
    GLUTEN_FREE("Gluten-Free"),
    DAIRY_FREE("Dairy-Free"),
    NUT_FREE("Nut-Free"),
    PESCATARIAN("Pescatarian"),
    KETO("Keto"),
    LOW_CARB("Low-Carb")
}

enum class Equipment(val label: String) {
    STOVETOP("Stovetop"),
    OVEN("Oven"),
    AIR_FRYER("Air Fryer"),
    INSTANT_POT("Instant Pot"),
    MICROWAVE("Microwave"),
    GRILL("Grill"),
    NO_COOK("No-Cook")
}

// ── Data models ────────────────────────────────────────────────────────

data class RecipeIngredient(
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val have_it: Boolean = true
)

data class SuggestedRecipe(
    val name: String = "",
    val cuisine_origin: String = "",
    val description: String = "",
    val time_minutes: Int = 0,
    val difficulty: String = "easy",
    val servings: Int = 2,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tips: String? = null
) {
    val matchedCount: Int get() = ingredients.count { it.have_it }
    val totalCount: Int get() = ingredients.size
    val matchPercentage: Int get() = if (totalCount > 0) (matchedCount * 100) / totalCount else 0
    val missingIngredients: List<RecipeIngredient> get() = ingredients.filter { !it.have_it }
}

data class InventoryItemSummary(
    val id: Long,
    val name: String,
    val quantity: Double,
    val unit: String?,
    val daysUntilExpiry: Long? = null
)

// ── Smart Tips ────────────────────────────────────────────────────────

enum class CookTipId { INVENTORY_EMPTY, INVENTORY_LOW, NO_EXPIRY, FIRST_COOK, POST_SAVE, LOW_MATCH, GREAT_MATCH }

data class CookTip(
    val id: CookTipId,
    val message: String,
    val ctaLabel: String? = null,
    val ctaRoute: String? = null
)

// ── Settings snapshot for "Cook Again" ────────────────────────────────

data class CookSettingsSnapshot(
    val mood: String? = null,
    val cuisine: String? = null,
    val cuisineCountry: String? = null,
    val spice: String = "MEDIUM",
    val effort: String = "STANDARD",
    val style: String = "CASUAL",
    val mealType: String = "DINNER",
    val servings: Int = 2,
    val dietary: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val flexible: Boolean = true
)

// ── UI State ───────────────────────────────────────────────────────────

data class CookUiState(
    // Configurator
    val selectedMood: MealMood? = null,
    val selectedCuisine: RegionalCuisine? = null,
    val spiceLevel: SpiceLevel = SpiceLevel.MEDIUM,
    val effortLevel: EffortLevel = EffortLevel.STANDARD,
    val styleLevel: StyleLevel = StyleLevel.CASUAL,
    val mealType: MealType = MealType.DINNER,
    val servings: Int = 2,
    val dietaryFilters: Set<DietaryFilter> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
    val flexibleIngredients: Boolean = true,
    val heroIngredients: List<InventoryItemSummary> = emptyList(),
    val showMoreOptions: Boolean = false,

    // Inventory
    val inventoryItems: List<InventoryItemSummary> = emptyList(),

    // Cuisine browser
    val showCuisinePassport: Boolean = false,
    val cuisineSearchQuery: String = "",
    val selectedContinent: String? = null,
    val expandedCountry: String? = null,

    // Hero ingredient picker
    val showHeroPicker: Boolean = false,

    // Results
    val recipes: List<SuggestedRecipe> = emptyList(),
    val showResults: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val previousRecipeNames: List<String> = emptyList(),

    // Refinement
    val refinementHint: String? = null,

    // Shopping list feedback
    val addedToShoppingList: Set<String> = emptySet(),

    // Saved recipes
    val savedRecipeNames: Set<String> = emptySet(),
    val savedRecipeCount: Int = 0,
    val lastSavedRecipeName: String? = null,
    val lastSavedRecipeToken: Int = 0,

    // Smart tips
    val currentTip: CookTip? = null,
    val dismissedTips: Set<CookTipId> = emptySet(),

) {
    val canCook: Boolean get() = selectedMood != null
}

// ── ViewModel ──────────────────────────────────────────────────────────

@HiltViewModel
class CookViewModel @Inject constructor(
    private val grokRepository: GrokRepository,
    private val itemRepository: ItemRepository,
    private val savedRecipeRepository: SavedRecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInventory()
        loadSavedRecipeState()
        // Consume "Cook Again" settings if pending
        pendingCookAgainSettings?.let { json ->
            pendingCookAgainSettings = null
            loadFromSettings(json)
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            try {
                val items = itemRepository.getAllActiveWithDetails().first()
                val summaries = items.map { iwd ->
                    val daysUntilExpiry = iwd.item.expiryDate?.let {
                        ChronoUnit.DAYS.between(LocalDate.now(), it)
                    }
                    InventoryItemSummary(
                        id = iwd.item.id,
                        name = iwd.item.name,
                        quantity = iwd.item.quantity,
                        unit = iwd.unit?.abbreviation,
                        daysUntilExpiry = daysUntilExpiry
                    )
                }.sortedWith(
                    compareBy<InventoryItemSummary> { it.daysUntilExpiry ?: Long.MAX_VALUE }
                        .thenBy { it.name }
                )
                _uiState.update { it.copy(inventoryItems = summaries) }
                evaluateConfiguratorTips()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load inventory: ${e.message}")
            }
        }
    }

    private fun loadSavedRecipeState() {
        viewModelScope.launch {
            savedRecipeRepository.getAllActive().collect { recipes ->
                _uiState.update { it.copy(
                    savedRecipeNames = recipes.map { r -> r.name }.toSet(),
                    savedRecipeCount = recipes.size
                )}
            }
        }
    }

    // ── Save / unsave recipes ──────────────────────────────────────

    fun saveRecipe(recipe: SuggestedRecipe) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val snapshot = CookSettingsSnapshot(
                    mood = state.selectedMood?.name,
                    cuisine = state.selectedCuisine?.name,
                    cuisineCountry = state.selectedCuisine?.country,
                    spice = state.spiceLevel.name,
                    effort = state.effortLevel.name,
                    style = state.styleLevel.name,
                    mealType = state.mealType.name,
                    servings = state.servings,
                    dietary = state.dietaryFilters.map { it.name },
                    equipment = state.equipment.map { it.name },
                    flexible = state.flexibleIngredients
                )
                val entity = SavedRecipeEntity(
                    name = recipe.name,
                    description = recipe.description,
                    cuisineOrigin = recipe.cuisine_origin,
                    timeMinutes = recipe.time_minutes,
                    difficulty = recipe.difficulty,
                    servings = recipe.servings,
                    ingredientsJson = gson.toJson(recipe.ingredients),
                    stepsJson = gson.toJson(recipe.steps),
                    tips = recipe.tips,
                    sourceSettingsJson = gson.toJson(snapshot)
                )
                // Upsert: update existing recipe if one with same name exists
                val existing = savedRecipeRepository.findByName(recipe.name)
                val isUpdate = existing != null
                if (existing != null) {
                    savedRecipeRepository.insert(entity.copy(id = existing.id, isFavorite = existing.isFavorite, personalNotes = existing.personalNotes, rating = existing.rating))
                } else {
                    savedRecipeRepository.insert(entity)
                }
                _uiState.update { st ->
                    val message = if (isUpdate) "Recipe updated!" else "Recipe saved! Find it anytime in My Recipes, even offline"
                    val postSaveTip = if (CookTipId.POST_SAVE !in st.dismissedTips) {
                        CookTip(CookTipId.POST_SAVE, message, "View", Screen.SavedRecipes.route)
                    } else null
                    st.copy(lastSavedRecipeName = recipe.name, lastSavedRecipeToken = st.lastSavedRecipeToken + 1, currentTip = postSaveTip ?: st.currentTip)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save recipe: ${e.message}")
            }
        }
    }

    fun unsaveRecipe(recipeName: String) {
        viewModelScope.launch {
            try {
                savedRecipeRepository.deleteByName(recipeName)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unsave recipe: ${e.message}")
            }
        }
    }

    fun clearLastSaved() {
        _uiState.update { it.copy(lastSavedRecipeName = null) }
    }

    fun loadFromSettings(json: String) {
        try {
            val snapshot = gson.fromJson(json, CookSettingsSnapshot::class.java) ?: return
            _uiState.update { state ->
                state.copy(
                    selectedMood = snapshot.mood?.let { m -> MealMood.entries.find { it.name == m } },
                    selectedCuisine = if (snapshot.cuisine != null && snapshot.cuisineCountry != null) {
                        CuisineData.search(snapshot.cuisine).firstOrNull()
                            ?: RegionalCuisine(snapshot.cuisine, snapshot.cuisineCountry, "")
                    } else null,
                    spiceLevel = SpiceLevel.entries.find { it.name == snapshot.spice } ?: SpiceLevel.MEDIUM,
                    effortLevel = EffortLevel.entries.find { it.name == snapshot.effort } ?: EffortLevel.STANDARD,
                    styleLevel = StyleLevel.entries.find { it.name == snapshot.style } ?: StyleLevel.CASUAL,
                    mealType = MealType.entries.find { it.name == snapshot.mealType } ?: MealType.DINNER,
                    servings = snapshot.servings,
                    dietaryFilters = snapshot.dietary.mapNotNull { d -> DietaryFilter.entries.find { it.name == d } }.toSet(),
                    equipment = snapshot.equipment.mapNotNull { e -> Equipment.entries.find { it.name == e } }.toSet(),
                    flexibleIngredients = snapshot.flexible
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load settings: ${e.message}")
        }
    }

    // ── Smart tips evaluation ──────────────────────────────────────

    private fun evaluateConfiguratorTips() {
        val state = _uiState.value
        val tip = when {
            CookTipId.INVENTORY_EMPTY !in state.dismissedTips && state.inventoryItems.isEmpty() ->
                CookTip(CookTipId.INVENTORY_EMPTY, "Your kitchen is empty! Scan your fridge or add items for personalized recipes", "Scan Kitchen", Screen.FridgeScan.route)
            CookTipId.INVENTORY_LOW !in state.dismissedTips && state.inventoryItems.size in 1..4 ->
                CookTip(CookTipId.INVENTORY_LOW, "Add more items to your kitchen — more items = better recipe matches!", "Add Items", Screen.ItemForm.createRoute())
            CookTipId.NO_EXPIRY !in state.dismissedTips && state.inventoryItems.isNotEmpty() && state.inventoryItems.all { it.daysUntilExpiry == null } ->
                CookTip(CookTipId.NO_EXPIRY, "Pro tip: Add expiry dates to get 'use it up before it expires' suggestions")
            CookTipId.FIRST_COOK !in state.dismissedTips && state.savedRecipeCount == 0 && state.selectedMood == null ->
                CookTip(CookTipId.FIRST_COOK, "Pick a mood to get started! We'll match 3 recipes to what's in your kitchen")
            else -> null
        }
        _uiState.update { it.copy(currentTip = tip) }
    }

    private fun evaluateResultsTips() {
        val state = _uiState.value
        if (state.recipes.isEmpty()) return
        val tip = when {
            CookTipId.LOW_MATCH !in state.dismissedTips && state.recipes.all { it.matchPercentage < 40 } ->
                CookTip(CookTipId.LOW_MATCH, "Most ingredients are missing — try enabling 'Suggest extra ingredients'")
            CookTipId.GREAT_MATCH !in state.dismissedTips && state.recipes.any { it.matchPercentage > 80 } ->
                CookTip(CookTipId.GREAT_MATCH, "Great match! You have almost everything needed")
            else -> null
        }
        _uiState.update { it.copy(currentTip = tip) }
    }

    fun dismissTip() {
        val tip = _uiState.value.currentTip ?: return
        _uiState.update { it.copy(
            currentTip = null,
            dismissedTips = it.dismissedTips + tip.id
        )}
    }

    // ── Configurator actions ───────────────────────────────────────

    fun selectMood(mood: MealMood) {
        _uiState.update {
            // Pre-tune defaults based on mood
            val effort = when (mood) {
                MealMood.QUICK -> EffortLevel.QUICK
                MealMood.IMPRESS, MealMood.COMFORT -> EffortLevel.STANDARD
                else -> it.effortLevel
            }
            val spice = when (mood) {
                MealMood.KID_FRIENDLY -> SpiceLevel.MILD
                else -> it.spiceLevel
            }
            val style = when (mood) {
                MealMood.IMPRESS -> StyleLevel.ELEVATED
                MealMood.QUICK, MealMood.KID_FRIENDLY -> StyleLevel.CASUAL
                else -> it.styleLevel
            }
            it.copy(
                selectedMood = mood,
                effortLevel = effort,
                spiceLevel = spice,
                styleLevel = style
            )
        }
    }

    fun selectCuisine(cuisine: RegionalCuisine?) {
        _uiState.update { it.copy(selectedCuisine = cuisine, showCuisinePassport = false) }
    }

    fun setSpiceLevel(level: SpiceLevel) {
        _uiState.update { it.copy(spiceLevel = level) }
    }

    fun setEffortLevel(level: EffortLevel) {
        _uiState.update { it.copy(effortLevel = level) }
    }

    fun setStyleLevel(level: StyleLevel) {
        _uiState.update { it.copy(styleLevel = level) }
    }

    fun setMealType(type: MealType) {
        _uiState.update { it.copy(mealType = type) }
    }

    fun setServings(count: Int) {
        _uiState.update { it.copy(servings = count.coerceIn(1, 8)) }
    }

    fun toggleDietary(filter: DietaryFilter) {
        _uiState.update {
            val newSet = it.dietaryFilters.toMutableSet()
            if (filter in newSet) newSet.remove(filter) else newSet.add(filter)
            it.copy(dietaryFilters = newSet)
        }
    }

    fun toggleEquipment(equip: Equipment) {
        _uiState.update {
            val newSet = it.equipment.toMutableSet()
            if (equip in newSet) newSet.remove(equip) else newSet.add(equip)
            it.copy(equipment = newSet)
        }
    }

    fun setFlexibleIngredients(flexible: Boolean) {
        _uiState.update { it.copy(flexibleIngredients = flexible) }
    }

    fun toggleHeroIngredient(item: InventoryItemSummary) {
        _uiState.update { state ->
            val current = state.heroIngredients.toMutableList()
            if (current.any { it.id == item.id }) {
                current.removeAll { it.id == item.id }
            } else if (current.size < 3) {
                current.add(item)
            }
            state.copy(heroIngredients = current)
        }
    }

    fun toggleMoreOptions() {
        _uiState.update { it.copy(showMoreOptions = !it.showMoreOptions) }
    }

    fun showCuisinePassport() {
        _uiState.update { it.copy(showCuisinePassport = true, cuisineSearchQuery = "", selectedContinent = null, expandedCountry = null) }
    }

    fun dismissCuisinePassport() {
        _uiState.update { it.copy(showCuisinePassport = false) }
    }

    fun setCuisineSearch(query: String) {
        _uiState.update { it.copy(cuisineSearchQuery = query) }
    }

    fun selectContinent(continent: String) {
        _uiState.update { it.copy(selectedContinent = continent, expandedCountry = null) }
    }

    fun toggleCountry(country: String) {
        _uiState.update {
            it.copy(expandedCountry = if (it.expandedCountry == country) null else country)
        }
    }

    fun showHeroPicker() {
        _uiState.update { it.copy(showHeroPicker = true) }
    }

    fun dismissHeroPicker() {
        _uiState.update { it.copy(showHeroPicker = false) }
    }

    // ── Generate recipes ───────────────────────────────────────────

    fun cook(refinement: String? = null) {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.selectedMood == null) return

        // Use passed refinement directly (avoids stale state race in tryAgain)
        val effectiveState = if (refinement != null) state.copy(refinementHint = refinement) else state
        _uiState.update { it.copy(isLoading = true, error = null, showResults = true, refinementHint = refinement) }

        viewModelScope.launch {
            try {
                val prompt = buildPrompt(effectiveState)
                val result = grokRepository.chatCompletion(
                    systemPrompt = SYSTEM_PROMPT,
                    userPrompt = prompt,
                    temperature = 0.3,
                    maxTokens = 6000
                )
                result.fold(
                    onSuccess = { response ->
                        val recipes = parseRecipes(response, state.inventoryItems)
                        if (recipes.isEmpty()) {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "Couldn't generate recipes. Try different settings or check your inventory."
                            )}
                        } else {
                            _uiState.update { it.copy(
                                recipes = recipes,
                                isLoading = false,
                                previousRecipeNames = (it.previousRecipeNames + recipes.map { r ->
                                    r.name.replace(Regex("[\\n\\r]"), "").take(80).trim()
                                }).takeLast(15)
                            )}
                            evaluateResultsTips()
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to generate recipes"
                        )}
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unexpected error") }
            }
        }
    }

    fun tryAgain(refinement: String? = null) {
        _uiState.update { it.copy(recipes = emptyList()) }
        cook(refinement = refinement)
    }

    fun backToConfigurator() {
        _uiState.update { it.copy(showResults = false, recipes = emptyList(), error = null, refinementHint = null, previousRecipeNames = emptyList(), addedToShoppingList = emptySet()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Shopping list integration ──────────────────────────────────

    fun addMissingToShoppingList(recipe: SuggestedRecipe) {
        viewModelScope.launch {
            recipe.missingIngredients.forEach { ingredient ->
                try {
                    shoppingListRepository.addItem(
                        ShoppingListItemEntity(
                            customName = ingredient.name,
                            quantity = 1.0,
                            notes = "For: ${recipe.name}"
                        )
                    )
                    _uiState.update {
                        it.copy(addedToShoppingList = it.addedToShoppingList + ingredient.name)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to add ${ingredient.name} to shopping list: ${e.message}")
                }
            }
        }
    }

    fun addSingleToShoppingList(ingredientName: String, recipeName: String) {
        viewModelScope.launch {
            try {
                shoppingListRepository.addItem(
                    ShoppingListItemEntity(
                        customName = ingredientName,
                        quantity = 1.0,
                        notes = "For: $recipeName"
                    )
                )
                _uiState.update {
                    it.copy(addedToShoppingList = it.addedToShoppingList + ingredientName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add $ingredientName to shopping list: ${e.message}")
            }
        }
    }

    // ── Prompt sanitization ───────────────────────────────────────

    private fun sanitizeForPrompt(str: String, maxLen: Int = 60): String {
        return str.replace(Regex("[\\n\\r{}\\[\\]]"), "").take(maxLen).trim()
    }

    // ── Prompt construction ────────────────────────────────────────

    private fun buildPrompt(state: CookUiState): String {
        val inventorySection = if (state.inventoryItems.isNotEmpty()) {
            val items = state.inventoryItems.joinToString("\n") { item ->
                val unitStr = item.unit?.let { " $it" } ?: ""
                val expiryStr = item.daysUntilExpiry?.let { days ->
                    when {
                        days <= 0 -> " (EXPIRED)"
                        days <= 2 -> " (expires in $days day${if (days != 1L) "s" else ""}!)"
                        days <= 7 -> " (expires in $days days)"
                        else -> ""
                    }
                } ?: ""
                "- ${item.name}: ${item.quantity}$unitStr$expiryStr"
            }
            "Available ingredients in kitchen:\n$items"
        } else {
            "No inventory items found. Suggest simple, common recipes."
        }

        val cuisineStr = state.selectedCuisine?.let {
            "Cuisine: ${sanitizeForPrompt(it.name)} (${sanitizeForPrompt(it.country)}) — authentic regional preparation"
        } ?: if (state.selectedMood == MealMood.SURPRISE) {
            "Cuisine: Any — surprise the user with something exciting"
        } else {
            "Cuisine: Any cuisine that works well with the available ingredients"
        }

        val moodStr = state.selectedMood?.let { "Mood: ${it.label} — ${it.aiHint}" } ?: ""
        val spiceStr = "Spice level: ${state.spiceLevel.label} — ${state.spiceLevel.aiHint}"
        val effortStr = "Effort: ${state.effortLevel.label} (${state.effortLevel.minutes}) — ${state.effortLevel.aiHint}"
        val styleStr = "Style: ${state.styleLevel.label} — ${state.styleLevel.aiHint}"
        val mealStr = "Meal type: ${state.mealType.label}"
        val servingsStr = "Serves: ${state.servings}"

        val dietaryStr = if (state.dietaryFilters.isNotEmpty()) {
            "Dietary requirements: ${state.dietaryFilters.joinToString(", ") { it.label }}"
        } else ""

        val equipStr = if (state.equipment.isNotEmpty()) {
            "Available equipment: ${state.equipment.joinToString(", ") { it.label }}"
        } else ""

        val heroStr = if (state.heroIngredients.isNotEmpty()) {
            "Hero ingredient(s) — MUST be central to every recipe: ${state.heroIngredients.joinToString(", ") { sanitizeForPrompt(it.name) }}"
        } else ""

        val flexStr = if (state.flexibleIngredients) {
            "Ingredient flexibility: May include up to 2 additional common, easily available ingredients. Mark them clearly with have_it=false."
        } else {
            "Ingredient flexibility: ONLY use ingredients from the available list. No additional purchases."
        }

        val excludeStr = if (state.previousRecipeNames.isNotEmpty()) {
            "Do NOT suggest these recipes (already shown): ${state.previousRecipeNames.joinToString(", ") { sanitizeForPrompt(it, 80) }}"
        } else ""

        val refinementStr = state.refinementHint?.let {
            "Additional preference: ${sanitizeForPrompt(it, 120)}"
        } ?: ""

        val month = LocalDate.now().month.name.lowercase().replaceFirstChar { it.uppercase() }

        return """
$inventorySection

$cuisineStr
$moodStr
$spiceStr
$effortStr
$styleStr
$mealStr
$servingsStr
${if (dietaryStr.isNotEmpty()) dietaryStr else ""}
${if (equipStr.isNotEmpty()) equipStr else ""}
${if (heroStr.isNotEmpty()) heroStr else ""}
$flexStr
Season: $month (suggest seasonally appropriate dishes)
${if (excludeStr.isNotEmpty()) excludeStr else ""}
${if (refinementStr.isNotEmpty()) refinementStr else ""}

Suggest exactly 3 recipes.
""".trimIndent().lines().filter { it.isNotBlank() }.joinToString("\n")
    }

    // ── Response parsing ───────────────────────────────────────────

    private fun parseRecipes(response: String, inventory: List<InventoryItemSummary>): List<SuggestedRecipe> {
        val cleaned = response.replace("```json", "").replace("```", "").trim()
        val jsonStr = extractJsonArray(cleaned) ?: cleaned

        return try {
            val type = object : TypeToken<List<SuggestedRecipe>>() {}.type
            val recipes: List<SuggestedRecipe> = gson.fromJson(jsonStr, type) ?: emptyList()

            // Post-process: match ingredients against actual inventory (word-boundary matching)
            recipes.map { recipe ->
                val matchedIngredients = recipe.ingredients.map { ing ->
                    val haveIt = ing.have_it || inventory.any { inv ->
                        ingredientMatch(inv.name, ing.name)
                    }
                    ing.copy(have_it = haveIt)
                }
                recipe.copy(ingredients = matchedIngredients)
            }.filter { it.name.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse recipes: ${e.message}")
            Log.d(TAG, "Response was: ${jsonStr.take(500)}")
            emptyList()
        }
    }

    private fun extractJsonArray(text: String): String? {
        val start = text.indexOf('[')
        if (start == -1) return null
        val end = text.lastIndexOf(']')
        if (end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    companion object {
        private const val TAG = "CookVM"

        /** Temporary holder for "Cook Again" settings passed between SavedRecipes → Cook screens */
        var pendingCookAgainSettings: String? = null

        /** Word-boundary ingredient matching — prevents "salt" matching "basalt" */
        fun ingredientMatch(inventoryName: String, ingredientName: String): Boolean {
            val invWords = inventoryName.lowercase().split(" ")
            val ingWords = ingredientName.lowercase().split(" ")
            return invWords.any { it in ingWords } || ingWords.any { it in invWords }
        }

        private val SYSTEM_PROMPT = """
You are an expert culinary advisor specializing in authentic regional cuisine from around the world.

═══ CRITICAL: REAL DISHES ONLY ═══
• Every recipe MUST be a REAL, well-known dish that actually exists in the specified cuisine.
• Use the dish's traditional name (e.g., "Dal Makhani", "Khao Soi", "Cacio e Pepe") — NOT invented combinations.
• NEVER create fake dishes by combining an inventory item with a cuisine label (e.g., "Nepali Frankfurter Curry" is NOT a real dish — do not invent things like this).
• If the user's ingredients don't fit the cuisine well, suggest real dishes and list the missing authentic ingredients as have_it=false. It is BETTER to need extra ingredients than to invent a fake dish.
• If an ingredient from the inventory has no place in the cuisine (e.g., frankfurters in Nepali food), simply IGNORE that ingredient. Do not force it into a recipe.

═══ RECIPE RULES ═══
• Suggest exactly 3 recipes in the requested cuisine style
• Prioritize recipes where the user already has the key ingredients
• For "have_it": true = user has it, false = needs to buy it
• Match ingredient names against the provided inventory — set have_it=true for matches
• Steps should be clear, numbered, and concise
• Tips should include substitution ideas or technique hints
• Time = total time (prep + cook)

RESPOND ONLY with a JSON array. No explanation, no markdown code blocks.

Format:
[{
  "name": "Actual Traditional Dish Name",
  "cuisine_origin": "Regional Cuisine (Country)",
  "description": "1-2 sentence description",
  "time_minutes": 30,
  "difficulty": "easy|medium|hard",
  "servings": 4,
  "ingredients": [
    {"name": "Chicken thighs", "amount": "500", "unit": "g", "have_it": true},
    {"name": "Coconut milk", "amount": "400", "unit": "ml", "have_it": false}
  ],
  "steps": ["Step 1...", "Step 2...", "Step 3..."],
  "tips": "Optional tips or substitutions"
}]
""".trimIndent()
    }
}
