package com.inventory.app.ui.screens.cook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.repository.CookingLogRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class DraftInfo(
    val recipeId: Long,
    val recipeName: String,
    val stepCount: Int,
    val lastModified: Long
)

data class LastCookedInfo(
    val recipeName: String,
    val recipeId: Long,
    val daysAgo: Int
)

data class RecentRecipeInfo(
    val id: Long,
    val name: String,
    val timeMinutes: Int
)

data class ResumeSessionInfo(
    val recipeId: Long,
    val recipeName: String,
    val stepIndex: Int
)

data class CookHubUiState(
    val draftRecipe: DraftInfo? = null,
    val lastCooked: LastCookedInfo? = null,
    val savedRecipeCount: Int = 0,
    val expiringItemCount: Int = 0,
    val expiringItemIds: List<Long> = emptyList(),
    val recentRecipes: List<RecentRecipeInfo> = emptyList(),
    val resumeSession: ResumeSessionInfo? = null,
    val isLoading: Boolean = true
) {
    /** Hide capture card when a resume session is active — would be confusing to show both. */
    val showCaptureCard: Boolean get() = resumeSession == null
}

private const val PLAYBACK_RECIPE_ID = "playback_recipe_id"
private const val PLAYBACK_STEP_INDEX = "playback_step_index"
private const val PLAYBACK_TIMESTAMP = "playback_timestamp"
private const val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L

@HiltViewModel
class CookHubViewModel @Inject constructor(
    private val savedRecipeRepository: SavedRecipeRepository,
    private val cookingLogRepository: CookingLogRepository,
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookHubUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
        checkResumeSession()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            settingsRepository.getIntFlow(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 3)
                .flatMapLatest { warningDays ->
                    combine(
                        savedRecipeRepository.getDrafts(),
                        savedRecipeRepository.getActiveNonDrafts(),
                        cookingLogRepository.getMostRecentWithName(),
                        itemRepository.getExpiringSoon(warningDays, limit = 5),
                        cookingLogRepository.getCookCountMap()
                    ) { drafts, activeRecipes, mostRecent, expiringItems, cookCountMap ->
                        // Draft: first one (most recently modified)
                        val draft = drafts.firstOrNull()?.let { entity ->
                            DraftInfo(
                                recipeId = entity.id,
                                recipeName = entity.name,
                                stepCount = estimateStepCount(entity),
                                lastModified = entity.updatedAt
                            )
                        }

                        // Last cooked: uses JOIN result directly, no activeRecipes lookup needed
                        val lastCookedInfo = mostRecent?.let { result ->
                            val daysAgo = ChronoUnit.DAYS.between(
                                Instant.ofEpochMilli(result.cookedDate)
                                    .atZone(ZoneId.systemDefault()).toLocalDate(),
                                LocalDate.now()
                            ).toInt()
                            LastCookedInfo(
                                recipeName = result.recipeName,
                                recipeId = result.recipeId,
                                daysAgo = daysAgo
                            )
                        }

                        // Recent recipes sorted by cook frequency (U-2)
                        val recent = activeRecipes
                            .sortedWith(
                                compareByDescending<SavedRecipeEntity> { cookCountMap[it.id] ?: 0 }
                                    .thenByDescending { it.updatedAt }
                            )
                            .take(5)
                            .map { RecentRecipeInfo(it.id, it.name, it.timeMinutes) }

                        CookHubUiState(
                            draftRecipe = draft,
                            lastCooked = lastCookedInfo,
                            savedRecipeCount = activeRecipes.size,
                            expiringItemCount = expiringItems.size,
                            expiringItemIds = expiringItems
                                .sortedBy { it.item.expiryDate }
                                .take(3)
                                .map { it.item.id },
                            recentRecipes = recent,
                            isLoading = false
                        )
                    }
                }
                .collect { state ->
                    _uiState.update { current ->
                        state.copy(resumeSession = current.resumeSession)
                    }
                }
        }
    }

    private fun checkResumeSession() {
        viewModelScope.launch {
            try {
                val timestamp = settingsRepository.getString(PLAYBACK_TIMESTAMP, "0").toLongOrNull() ?: 0L
                if (timestamp == 0L || System.currentTimeMillis() - timestamp > FOUR_HOURS_MS) return@launch

                val recipeId = settingsRepository.getString(PLAYBACK_RECIPE_ID, "0").toLongOrNull() ?: 0L
                if (recipeId == 0L) return@launch

                val stepIndex = settingsRepository.getString(PLAYBACK_STEP_INDEX, "0").toIntOrNull() ?: 0
                val entity = savedRecipeRepository.getById(recipeId) ?: return@launch

                _uiState.update { it.copy(
                    resumeSession = ResumeSessionInfo(
                        recipeId = recipeId,
                        recipeName = entity.name,
                        stepIndex = stepIndex
                    )
                )}
            } catch (_: Exception) {
                // Safe fallback — resume session is optional
            }
        }
    }

    private fun estimateStepCount(entity: SavedRecipeEntity): Int {
        // Quick heuristic: count occurrences of step delimiters in stepsJson
        val json = entity.stepsJson
        if (json.isBlank() || json == "[]") return 0
        return json.split("instruction").size - 1
    }
}
