package com.inventory.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.ChartDataRow
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.PantryHealthRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.domain.model.HomeScoreCalculator
import com.inventory.app.domain.model.KitchenStoryMission
import com.inventory.app.domain.model.KitchenStoryMissions
import com.inventory.app.domain.model.KitchenStoryState
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalItems: Int = 0,
    val expiringSoon: Int = 0,
    val lowStock: Int = 0,
    val totalValue: Double = 0.0,
    val expiringItems: List<ItemWithDetails> = emptyList(),
    val lowStockItems: List<ItemWithDetails> = emptyList(),
    val itemsByCategory: List<ChartDataRow> = emptyList(),
    val itemsByLocation: List<ChartDataRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currencySymbol: String = "",
    val homeScore: Int = 0,
    val homeScoreLabel: String = "No Items",
    val expiredCount: Int = 0,
    val outOfStockCount: Int = 0,
    val lastScanItemCount: Int = 0,
    val lastScanAreaCount: Int = 0,
    val shoppingActive: Int = 0,
    val shoppingPurchased: Int = 0,
    val savedRecipeCount: Int = 0,
    val userPreference: String = "INVENTORY",
    val kitchenStory: KitchenStoryState = KitchenStoryState()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val pantryHealthRepository: PantryHealthRepository,
    private val savedRecipeRepository: SavedRecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var kitchenStoryJob: Job? = null

    init {
        loadData()
        loadKitchenStory()
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
        launch {
            val currency = settingsRepository.getCurrencySymbol()
            val lastItems = settingsRepository.getInt("last_scan_item_count", 0)
            val lastAreas = settingsRepository.getInt("last_scan_area_count", 0)
            val preference = settingsRepository.getString(OnboardingViewModel.KEY_USER_PREFERENCE, "INVENTORY")
            _uiState.update { it.copy(
                currencySymbol = currency,
                lastScanItemCount = lastItems,
                lastScanAreaCount = lastAreas,
                userPreference = preference
            ) }
        }
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        launch {
            try {
                settingsRepository.getIntFlow(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 7)
                    .flatMapLatest { warningDays ->
                        combine(
                            itemRepository.getExpiringSoonCount(warningDays),
                            itemRepository.getExpiringSoon(warningDays)
                        ) { count, items -> Pair(count, items) }
                    }
                    .collect { (count, items) ->
                        _uiState.update { it.copy(expiringSoon = count, expiringItems = items, isLoading = false) }
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to load expiry data: ${e.message}", isLoading = false) }
            }
        }

        launch {
            try {
                itemRepository.getTotalItemCount().collect { count ->
                    _uiState.update { it.copy(totalItems = count, isLoading = false) }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to load dashboard: ${e.message}", isLoading = false) }
            }
        }
        launch {
            try {
                itemRepository.getLowStockCount().collect { count ->
                    _uiState.update { it.copy(lowStock = count, isLoading = false) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        launch {
            try {
                itemRepository.getTotalValue().collect { value ->
                    _uiState.update { it.copy(totalValue = value, isLoading = false) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        launch {
            try {
                itemRepository.getLowStockItems().collect { items ->
                    _uiState.update { it.copy(lowStockItems = items, isLoading = false) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        launch {
            try {
                itemRepository.getItemCountByCategory().collect { data ->
                    _uiState.update { it.copy(itemsByCategory = data, isLoading = false) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        launch {
            try {
                itemRepository.getItemCountByLocation().collect { data ->
                    _uiState.update { it.copy(itemsByLocation = data, isLoading = false) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        // Home Score
        launch {
            try {
                combine(
                    itemRepository.getTotalItemCount(),
                    itemRepository.getItemsWithCategoryCount(),
                    itemRepository.getItemsWithLocationCount(),
                    itemRepository.getItemsWithExpiryCount(),
                    itemRepository.getExpiredCount(),
                    itemRepository.getOutOfStockCount(),
                    shoppingListRepository.getActiveCount(),
                    shoppingListRepository.getPurchasedCount()
                ) { values ->
                    if (values.size < 8) return@combine null
                    val totalItems = values[0] as Int
                    val withCategory = values[1] as Int
                    val withLocation = values[2] as Int
                    val withExpiry = values[3] as Int
                    val expired = values[4] as Int
                    val outOfStock = values[5] as Int
                    val shoppingActive = values[6] as Int
                    val shoppingPurchased = values[7] as Int

                    val expiringSoon = _uiState.value.expiringSoon
                    val lowStock = _uiState.value.lowStock
                    val totalShopping = shoppingActive + shoppingPurchased
                    val completionPct = if (totalShopping > 0) shoppingPurchased.toFloat() / totalShopping else 0f

                    val breakdown = HomeScoreCalculator.compute(
                        totalItems = totalItems,
                        itemsWithCategory = withCategory,
                        itemsWithLocation = withLocation,
                        itemsWithExpiry = withExpiry,
                        expiredCount = expired,
                        expiringSoonCount = expiringSoon,
                        lowStockCount = lowStock,
                        outOfStockCount = outOfStock,
                        shoppingActive = shoppingActive,
                        shoppingPurchased = shoppingPurchased
                    )

                    data class ScoreData(
                        val score: Int, val label: String, val expired: Int, val outOfStock: Int,
                        val expiringSoon: Int, val lowStock: Int, val completionPct: Float,
                        val totalItems: Int, val engagementScore: Int, val conditionScore: Int,
                        val shoppingActive: Int, val shoppingPurchased: Int
                    )
                    ScoreData(breakdown.finalScore, breakdown.label, expired, outOfStock, expiringSoon, lowStock, completionPct, totalItems, breakdown.engagementScore, breakdown.conditionScore, shoppingActive, shoppingPurchased)
                }.collect { data ->
                    data ?: return@collect
                    _uiState.update { it.copy(
                        homeScore = data.score,
                        homeScoreLabel = data.label,
                        expiredCount = data.expired,
                        outOfStockCount = data.outOfStock,
                        shoppingActive = data.shoppingActive,
                        shoppingPurchased = data.shoppingPurchased
                    ) }
                    try {
                        pantryHealthRepository.recordSnapshot(
                            score = data.score,
                            expiredCount = data.expired,
                            expiringSoonCount = data.expiringSoon,
                            lowStockCount = data.lowStock,
                            outOfStockCount = data.outOfStock,
                            shoppingCompletionPct = data.completionPct,
                            totalItems = data.totalItems,
                            engagementScore = data.engagementScore,
                            conditionScore = data.conditionScore
                        )
                    } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        // Saved recipe count
        launch {
            try {
                savedRecipeRepository.getCount().collect { count ->
                    _uiState.update { it.copy(savedRecipeCount = count) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        } // end loadJob
    }

    private fun loadKitchenStory() {
        kitchenStoryJob?.cancel()
        kitchenStoryJob = viewModelScope.launch {
            val dismissed = settingsRepository.getBoolean(KitchenStoryMissions.KEY_DISMISSED, false)
            val onboardingDone = settingsRepository.getBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false)
            if (dismissed || !onboardingDone) {
                _uiState.update { it.copy(kitchenStory = KitchenStoryState(isDismissed = dismissed, onboardingCompleted = onboardingDone)) }
                return@launch
            }

            val preference = settingsRepository.getString(OnboardingViewModel.KEY_USER_PREFERENCE, "INVENTORY")
            val definitions = KitchenStoryMissions.getOrderedMissions(preference)

            // Reactive combine: item count, shopping count, recipe viewed, reports viewed, expiry count, smart defaults flag
            combine(
                itemRepository.getTotalItemCount(),
                shoppingListRepository.getActiveCount(),
                settingsRepository.getBooleanFlow("recipe_result_viewed", false),
                settingsRepository.getBooleanFlow("reports_viewed", false),
                itemRepository.getItemsWithExpiryCount(),
                settingsRepository.getBooleanFlow(KitchenStoryMissions.KEY_SMART_DEFAULTS_SHOWN, false)
            ) { values ->
                val itemCount = values[0] as Int
                val shoppingCount = values[1] as Int
                val recipeViewed = values[2] as Boolean
                val reportsViewed = values[3] as Boolean
                val expiryCount = values[4] as Int
                val smartDefaultsShown = values[5] as Boolean

                val missions = definitions.mapIndexed { idx, def ->
                    val alreadyDone = settingsRepository.getBoolean(def.settingsKey, false)
                    val triggered = KitchenStoryMissions.evaluateTrigger(
                        def.triggerType, itemCount, shoppingCount, recipeViewed, reportsViewed, expiryCount
                    )
                    val completed = alreadyDone || triggered
                    // Auto-persist: one-way false→true
                    if (triggered && !alreadyDone) {
                        settingsRepository.setBoolean(def.settingsKey, true)
                    }
                    val subtitle = if (!completed) {
                        KitchenStoryMissions.dynamicSubtitle(def.triggerType, itemCount, shoppingCount, expiryCount)
                    } else null

                    KitchenStoryMission(idx, def.text, completed, def.settingsKey, def.navTarget, subtitle)
                }
                val completedCount = missions.count { it.isCompleted }

                // Show Smart Defaults education when mission 1 (Stock your shelves) first completes
                val stockMissionComplete = missions.firstOrNull()?.isCompleted == true
                val showSmartDefaults = stockMissionComplete && !smartDefaultsShown

                KitchenStoryState(
                    missions = missions,
                    isDismissed = false,
                    onboardingCompleted = true,
                    cardSubtitle = KitchenStoryMissions.cardSubtitle(completedCount),
                    showSmartDefaultsEducation = showSmartDefaults
                )
            }.collect { story ->
                _uiState.update { it.copy(kitchenStory = story) }
            }
        }
    }

    fun dismissSmartDefaultsEducation() {
        viewModelScope.launch {
            settingsRepository.setBoolean(KitchenStoryMissions.KEY_SMART_DEFAULTS_SHOWN, true)
            _uiState.update { it.copy(kitchenStory = it.kitchenStory.copy(showSmartDefaultsEducation = false)) }
        }
    }

    /** Navigate to most recently added item in tour mode. Returns item ID or null. */
    suspend fun startSmartDefaultsTour(): Long? {
        val itemId = itemRepository.getLastAddedItemId() ?: return null
        com.inventory.app.ui.screens.items.ItemFormViewModel.pendingTourMode = true
        dismissSmartDefaultsEducation()
        return itemId
    }

    fun dismissKitchenStory() {
        kitchenStoryJob?.cancel() // Stop combine from overwriting isDismissed
        _uiState.update { it.copy(kitchenStory = it.kitchenStory.copy(isDismissed = true)) }
        viewModelScope.launch {
            settingsRepository.setBoolean(KitchenStoryMissions.KEY_DISMISSED, true)
        }
    }

    fun completeKitchenStory() {
        kitchenStoryJob?.cancel()
        _uiState.update { it.copy(kitchenStory = it.kitchenStory.copy(isDismissed = true)) }
        viewModelScope.launch {
            settingsRepository.setBoolean(KitchenStoryMissions.KEY_DISMISSED, true)
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadData()
    }

    fun pauseItem(itemId: Long) {
        viewModelScope.launch {
            itemRepository.pauseItem(itemId)
        }
    }

    fun unpauseItem(itemId: Long) {
        viewModelScope.launch {
            itemRepository.unpauseItem(itemId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
