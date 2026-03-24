package com.inventory.app.ui.screens.cook

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.gson.Gson
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.domain.model.IngredientMatcher
import com.inventory.app.domain.model.RecipeIngredient
import com.inventory.app.domain.model.RecipeSource
import com.inventory.app.domain.model.convertStepsToRichFormat
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.util.NonFoodCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ── Style tags ─────────────────────────────────────────────────────────────

enum class StyleTag(val label: String, val aiHint: String) {
    QUICK("Quick", "ready in under 30 minutes"),
    VEGETARIAN("Vegetarian", "no meat or seafood"),
    VEGAN("Vegan", "no animal products"),
    COMFORT("Comfort", "hearty, warming, satisfying"),
    BUDGET("Budget-friendly", "uses cheap, common ingredients"),
    HEALTHY("Healthy", "nutritious, low-calorie"),
    SPICY("Spicy", "bold flavors with heat")
}

// ── UI models ──────────────────────────────────────────────────────────────

data class GeneratedRecipePreview(
    val name: String,
    val cuisineOrigin: String,
    val description: String,
    val timeMinutes: Int,
    val difficulty: String,
    val servings: Int,
    val ingredients: List<RecipeIngredient>,  // have_it cross-referenced against pantry
    val steps: List<String>,
    val tips: String?
)

data class AiRecipeDescriptionUiState(
    val description: String = "",
    val servings: Int = 2,
    val selectedTags: Set<StyleTag> = emptySet(),
    val isGenerating: Boolean = false,
    val generatedRecipe: GeneratedRecipePreview? = null,
    val error: String? = null,
    val isSavingToBuilder: Boolean = false
)

// ── Gson deserialization helper ────────────────────────────────────────────

private data class AiRawRecipe(
    val name: String = "",
    val cuisine_origin: String? = null,
    val description: String? = null,
    val time_minutes: Int? = null,
    val difficulty: String? = null,
    val servings: Int? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tips: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────

@HiltViewModel
class AiRecipeDescriptionViewModel @Inject constructor(
    private val grokRepository: GrokRepository,
    private val itemRepository: ItemRepository,
    private val savedRecipeRepository: SavedRecipeRepository,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiRecipeDescriptionUiState())
    val uiState = _uiState.asStateFlow()

    fun setDescription(text: String) {
        _uiState.update { it.copy(description = text, error = null) }
    }

    fun setServings(n: Int) {
        _uiState.update { it.copy(servings = n.coerceIn(1, 12)) }
    }

    fun toggleTag(tag: StyleTag) {
        _uiState.update { state ->
            val updated = state.selectedTags.toMutableSet()
            if (tag in updated) updated.remove(tag) else updated.add(tag)
            state.copy(selectedTags = updated)
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(generatedRecipe = null, error = null) }
    }

    fun generateRecipe() {
        val state = _uiState.value
        if (state.description.isBlank() || state.isGenerating) return

        // Network check
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!isOnline) {
            _uiState.update { it.copy(error = "No connection — recipe generation requires internet") }
            return
        }

        _uiState.update { it.copy(isGenerating = true, error = null) }

        viewModelScope.launch {
            try {
                // Fetch inventory
                val allItems = itemRepository.getAllActiveWithDetails().first()
                val today = LocalDate.now()

                val inventoryItems = allItems
                    .filter { iwd -> !NonFoodCategories.isNonFood(iwd.category?.name ?: "") }
                    .sortedBy { iwd ->
                        iwd.item.expiryDate?.let { ChronoUnit.DAYS.between(today, it) } ?: Long.MAX_VALUE
                    }
                    .take(100)

                val inventorySection = if (inventoryItems.isNotEmpty()) {
                    val lines = inventoryItems.joinToString("\n") { iwd ->
                        val unitStr = iwd.unit?.abbreviation?.let { " $it" } ?: ""
                        val days = iwd.item.expiryDate?.let { ChronoUnit.DAYS.between(today, it) }
                        val expiryStr = days?.let { d ->
                            when {
                                d <= 0 -> " (EXPIRED)"
                                d <= 2 -> " (expires in $d day${if (d != 1L) "s" else ""}!)"
                                d <= 7 -> " (expires in $d days)"
                                else -> ""
                            }
                        } ?: ""
                        "- ${iwd.item.name}: ${iwd.item.quantity}$unitStr$expiryStr"
                    }
                    "Available ingredients in kitchen:\n$lines"
                } else {
                    "No inventory items found."
                }

                val styleStr = if (state.selectedTags.isEmpty()) "any style"
                else state.selectedTags.joinToString(", ") { it.aiHint }

                val systemPrompt = "You are a recipe assistant. Return ONLY valid JSON. No markdown, no explanation."
                val userPrompt = buildString {
                    appendLine("Generate 1 recipe for: \"${state.description}\"")
                    appendLine("Style: $styleStr")
                    appendLine("Servings: ${state.servings}")
                    appendLine()
                    appendLine(inventorySection)
                    appendLine()
                    appendLine("Rules:")
                    appendLine("- Generate exactly 1 recipe matching the description")
                    appendLine("- set have_it=true for ingredients the user has (match against available list)")
                    appendLine("- set have_it=false for ingredients they need to buy")
                    appendLine("- Steps must be clear and action-oriented, 6-12 steps total")
                    appendLine()
                    appendLine("Return ONLY a single JSON object:")
                    appendLine("{")
                    appendLine("  \"name\": \"...\",")
                    appendLine("  \"cuisine_origin\": \"...\",")
                    appendLine("  \"description\": \"1-2 sentences\",")
                    appendLine("  \"time_minutes\": 30,")
                    appendLine("  \"difficulty\": \"easy|medium|hard\",")
                    appendLine("  \"servings\": ${state.servings},")
                    appendLine("  \"ingredients\": [{\"name\":\"...\",\"amount\":\"...\",\"unit\":\"...\",\"have_it\":true}],")
                    appendLine("  \"steps\": [\"Step 1...\", \"Step 2...\"],")
                    append("  \"tips\": \"optional\"")
                    append("\n}")
                }

                val result = grokRepository.chatCompletion(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = 0.3,
                    maxTokens = 3000
                )

                result.fold(
                    onSuccess = { raw ->
                        val cleaned = raw.trim()
                            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                        // Try parse as single object; fallback to array and take first
                        val aiRecipe: AiRawRecipe = try {
                            gson.fromJson(cleaned, AiRawRecipe::class.java)
                        } catch (e: Exception) {
                            try {
                                val type = object : com.google.gson.reflect.TypeToken<List<AiRawRecipe>>() {}.type
                                val list: List<AiRawRecipe> = gson.fromJson(cleaned, type)
                                list.firstOrNull() ?: throw IllegalStateException("Empty array")
                            } catch (e2: Exception) {
                                throw IllegalStateException("JSON parse failed: ${e2.message}")
                            }
                        }

                        if (aiRecipe.name.isBlank() || aiRecipe.steps.isEmpty()) {
                            throw IllegalStateException("Incomplete recipe response from AI")
                        }

                        // Cross-reference ingredients against actual pantry (override AI's have_it)
                        val inventoryNames = allItems.map { it.item.name }
                        val enrichedIngredients = aiRecipe.ingredients.map { ing ->
                            ing.copy(
                                have_it = inventoryNames.any { inv ->
                                    IngredientMatcher.matches(inv, ing.name)
                                }
                            )
                        }

                        val preview = GeneratedRecipePreview(
                            name = aiRecipe.name,
                            cuisineOrigin = aiRecipe.cuisine_origin ?: "",
                            description = aiRecipe.description ?: "",
                            timeMinutes = aiRecipe.time_minutes ?: 0,
                            difficulty = aiRecipe.difficulty ?: "easy",
                            servings = aiRecipe.servings ?: state.servings,
                            ingredients = enrichedIngredients,
                            steps = aiRecipe.steps,
                            tips = aiRecipe.tips
                        )
                        _uiState.update { it.copy(isGenerating = false, generatedRecipe = preview) }
                    },
                    onFailure = { e ->
                        Log.w(TAG, "Recipe generation failed: ${e.message}")
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                error = when {
                                    e.message?.contains("timeout", ignoreCase = true) == true ->
                                        "Request timed out — please try again"
                                    else -> "Couldn't generate recipe — please try again"
                                }
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected error in generateRecipe: ${e.message}")
                _uiState.update {
                    it.copy(isGenerating = false, error = "Couldn't generate recipe — please try again")
                }
            }
        }
    }

    fun openInBuilder(navController: NavController) {
        val preview = _uiState.value.generatedRecipe ?: return
        if (_uiState.value.isSavingToBuilder) return
        _uiState.update { it.copy(isSavingToBuilder = true) }

        viewModelScope.launch {
            try {
                val richSteps = convertStepsToRichFormat(preview.steps)
                val entity = SavedRecipeEntity(
                    name = preview.name,
                    description = preview.description,
                    cuisineOrigin = preview.cuisineOrigin,
                    timeMinutes = preview.timeMinutes,
                    difficulty = preview.difficulty.ifBlank { "easy" },
                    servings = preview.servings,
                    ingredientsJson = gson.toJson(preview.ingredients),
                    stepsJson = gson.toJson(richSteps),
                    tips = preview.tips,
                    source = RecipeSource.AI.value,
                    isDraft = true
                )
                val recipeId = savedRecipeRepository.insert(entity)
                navController.navigate(Screen.RecipeBuilder.createRoute(recipeId = recipeId))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save recipe draft: ${e.message}")
                _uiState.update { it.copy(isSavingToBuilder = false, error = "Failed to open builder — please try again") }
            }
        }
    }

    companion object {
        private const val TAG = "AiRecipeDescVM"
    }
}
