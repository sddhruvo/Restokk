package com.inventory.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.ChartDataRow
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.CookingLogRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.PantryHealthRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.ui.theme.VisualStyle
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.domain.model.HomeScoreCalculator
import com.inventory.app.domain.model.KitchenStoryMission
import com.inventory.app.domain.model.KitchenStoryMissions
import com.inventory.app.domain.model.KitchenStoryState
import com.inventory.app.domain.model.UrgencyLevel
import com.inventory.app.domain.model.UrgencyResult
import com.inventory.app.domain.model.UrgencyScorer
import com.inventory.app.domain.model.UrgencyTarget
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
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
    val kitchenStory: KitchenStoryState = KitchenStoryState(),
    val appTheme: AppTheme = AppTheme.CLASSIC_GREEN,
    val visualStyle: VisualStyle = VisualStyle.PAPER_INK,
    val lowStockThreshold: Float = 0.25f,
    val urgencyResult: UrgencyResult = UrgencyResult(UrgencyTarget.NONE, 0, UrgencyLevel.NONE),
    val dashboardHighlightEnabled: Boolean = true,
    // SI-3: Contextual insight
    val contextualInsight: String = "",
    val insightTrendDelta: Int = 0,  // positive = improved, negative = declined
    val expiredItems: List<ItemWithDetails> = emptyList(),
    val manualRecipeCount: Int = 0,
    val lastCookedName: String? = null,
    val lastCookedDaysAgo: Int? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val pantryHealthRepository: PantryHealthRepository,
    private val savedRecipeRepository: SavedRecipeRepository,
    private val cookingLogRepository: CookingLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var kitchenStoryJob: Job? = null
    private val pendingLoaders = AtomicInteger(0)

    private fun markLoaded() {
        if (pendingLoaders.get() > 0) {
            pendingLoaders.decrementAndGet()
        }
        if (pendingLoaders.get() <= 0) {
            _uiState.update { it.copy(isLoading = false) }
            recomputeUrgency()
        }
    }

    private fun recomputeUrgency() {
        val state = _uiState.value
        if (state.isLoading) return
        val expiryDates = state.expiringItems.map { it.item.expiryDate }
        val result = UrgencyScorer.compute(
            expiredCount = state.expiredCount,
            expiringDates = expiryDates,
            lowStockCount = state.lowStock,
            shoppingActiveCount = state.shoppingActive,
            totalItems = state.totalItems
        )
        _uiState.update { it.copy(urgencyResult = result) }
        generateInsight()
    }

    /** SI-3: Generate a contextual one-liner explaining current pantry state. */
    private fun generateInsight() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state.totalItems == 0) {
                    _uiState.update { it.copy(contextualInsight = "Start by adding your first item", insightTrendDelta = 0) }
                    return@launch
                }
                val previous = pantryHealthRepository.getPreviousSnapshot()
                val currentScore = state.homeScore
                val currentLabel = state.homeScoreLabel

                if (previous == null) {
                    // First day — no comparison available
                    _uiState.update { it.copy(
                        contextualInsight = "Tracking ${state.totalItems} item${if (state.totalItems != 1) "s" else ""} \u2014 keep it up!",
                        insightTrendDelta = 0
                    ) }
                    return@launch
                }

                val delta = currentScore - previous.score
                val prevLabel = HomeScoreCalculator.labelForScore(previous.score)

                // Priority cascade: milestone > improved > declined > urgent > stable
                val insight = when {
                    // Milestone: label improved (e.g., "Good" → "Great")
                    currentLabel != prevLabel && delta > 0 ->
                        "You reached '$currentLabel'! \u2191$delta since yesterday"

                    // Score improved
                    delta > 0 -> {
                        val reason = when {
                            state.expiredCount < previous.expiredCount -> "${previous.expiredCount - state.expiredCount} expired item${if (previous.expiredCount - state.expiredCount != 1) "s" else ""} cleared"
                            state.totalItems > previous.totalItems -> "${state.totalItems - previous.totalItems} new item${if (state.totalItems - previous.totalItems != 1) "s" else ""} added"
                            state.lowStock < previous.lowStockCount -> "restocked ${previous.lowStockCount - state.lowStock} item${if (previous.lowStockCount - state.lowStock != 1) "s" else ""}"
                            else -> "kitchen health improving"
                        }
                        "\u2191$delta since yesterday \u2014 $reason"
                    }

                    // Score declined
                    delta < 0 -> {
                        val reason = when {
                            state.expiredCount > previous.expiredCount -> "${state.expiredCount - previous.expiredCount} item${if (state.expiredCount - previous.expiredCount != 1) "s" else ""} expired"
                            state.lowStock > previous.lowStockCount -> "${state.lowStock - previous.lowStockCount} item${if (state.lowStock - previous.lowStockCount != 1) "s" else ""} running low"
                            state.outOfStockCount > previous.outOfStockCount -> "${state.outOfStockCount - previous.outOfStockCount} item${if (state.outOfStockCount - previous.outOfStockCount != 1) "s" else ""} out of stock"
                            else -> "some items need attention"
                        }
                        "\u2193${-delta} since yesterday \u2014 $reason"
                    }

                    // Stable
                    else -> "Steady at $currentScore \u2014 tracking ${state.totalItems} item${if (state.totalItems != 1) "s" else ""}"
                }

                _uiState.update { it.copy(contextualInsight = insight, insightTrendDelta = delta) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Silently fail — subtitle will remain empty and DashboardScreen falls back to prefTagline
            }
        }
    }

    init {
        loadData()
        loadKitchenStory()
    }

    private fun loadData() {
        loadJob?.cancel()
        pendingLoaders.set(7) // 7 flow collectors must emit before isLoading = false
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
        launch {
            val currency = settingsRepository.getCurrencySymbol()
            val dateFormat = settingsRepository.getString(SettingsRepository.KEY_DATE_FORMAT, "")
            com.inventory.app.util.FormatUtils.dateFormatOverride = dateFormat
            val lastItems = settingsRepository.getInt("last_scan_item_count", 0)
            val lastAreas = settingsRepository.getInt("last_scan_area_count", 0)
            val preference = settingsRepository.getString(OnboardingViewModel.KEY_USER_PREFERENCE, "INVENTORY")
            val themeKey = settingsRepository.getString(SettingsRepository.KEY_APP_THEME, AppTheme.CLASSIC_GREEN.key)
            val styleKey = settingsRepository.getString(SettingsRepository.KEY_VISUAL_STYLE, VisualStyle.PAPER_INK.key)
            val highlightEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_DASHBOARD_HIGHLIGHT_ENABLED, true)
            _uiState.update { it.copy(
                currencySymbol = currency,
                lastScanItemCount = lastItems,
                lastScanAreaCount = lastAreas,
                userPreference = preference,
                appTheme = AppTheme.fromKey(themeKey),
                visualStyle = VisualStyle.fromKey(styleKey),
                dashboardHighlightEnabled = highlightEnabled
            ) }
        }
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        launch {
            try {
                settingsRepository.getIntFlow(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 3)
                    .flatMapLatest { warningDays ->
                        combine(
                            itemRepository.getExpiringSoonCount(warningDays),
                            itemRepository.getExpiringSoon(warningDays)
                        ) { count, items -> Pair(count, items) }
                    }
                    .collect { (count, items) ->
                        _uiState.update { it.copy(expiringSoon = count, expiringItems = items) }
                        markLoaded()
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to load expiry data: ${e.message}") }
                markLoaded()
            }
        }

        launch {
            try {
                itemRepository.getTotalItemCount().collect { count ->
                    _uiState.update { it.copy(totalItems = count) }
                    markLoaded()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to load dashboard: ${e.message}") }
                markLoaded()
            }
        }
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        launch {
            try {
                settingsRepository.getStringFlow(SettingsRepository.KEY_LOW_STOCK_THRESHOLD, "25")
                    .map { (it.toDoubleOrNull() ?: 25.0) / 100.0 }
                    .flatMapLatest { ratio ->
                        combine(
                            itemRepository.getLowStockCount(ratio),
                            itemRepository.getLowStockItems(thresholdRatio = ratio)
                        ) { count, items -> Triple(count, items, ratio) }
                    }
                    .collect { (count, items, ratio) ->
                        _uiState.update { it.copy(lowStock = count, lowStockItems = items, lowStockThreshold = ratio.toFloat()) }
                        markLoaded()
                    }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; markLoaded() }
        }
        launch {
            try {
                itemRepository.getTotalValue().collect { value ->
                    _uiState.update { it.copy(totalValue = value) }
                    markLoaded()
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; markLoaded() }
        }
        launch {
            try {
                itemRepository.getItemCountByCategory().collect { data ->
                    _uiState.update { it.copy(itemsByCategory = data) }
                    markLoaded()
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; markLoaded() }
        }
        launch {
            try {
                itemRepository.getItemCountByLocation().collect { data ->
                    _uiState.update { it.copy(itemsByLocation = data) }
                    markLoaded()
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; markLoaded() }
        }
        // Home Score
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        launch {
            try {
                val expiringSoonCountFlow = settingsRepository.getIntFlow(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 3)
                    .flatMapLatest { itemRepository.getExpiringSoonCount(it) }
                val lowStockCountFlow = settingsRepository.getStringFlow(SettingsRepository.KEY_LOW_STOCK_THRESHOLD, "25")
                    .map { (it.toDoubleOrNull() ?: 25.0) / 100.0 }
                    .flatMapLatest { itemRepository.getLowStockCount(it) }

                combine(
                    itemRepository.getTotalItemCount(),
                    itemRepository.getItemsWithCategoryCount(),
                    itemRepository.getItemsWithLocationCount(),
                    itemRepository.getItemsWithExpiryCount(),
                    itemRepository.getExpiredCount(),
                    itemRepository.getOutOfStockCount(),
                    shoppingListRepository.getActiveCount(),
                    shoppingListRepository.getPurchasedCount(),
                    expiringSoonCountFlow,
                    lowStockCountFlow
                ) { values ->
                    if (values.size < 10) return@combine null
                    val totalItems = values[0] as Int
                    val withCategory = values[1] as Int
                    val withLocation = values[2] as Int
                    val withExpiry = values[3] as Int
                    val expired = values[4] as Int
                    val outOfStock = values[5] as Int
                    val shoppingActive = values[6] as Int
                    val shoppingPurchased = values[7] as Int
                    val expiringSoon = values[8] as Int
                    val lowStock = values[9] as Int

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
                    markLoaded()
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
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; markLoaded() }
        }
        // Saved recipe count
        launch {
            try {
                savedRecipeRepository.getCount().collect { count ->
                    _uiState.update { it.copy(savedRecipeCount = count) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        // Expired items (for Hero Zone triage)
        launch {
            try {
                itemRepository.getExpiredItems().collect { items ->
                    _uiState.update { it.copy(expiredItems = items) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        // Manual recipe count (user-created: manual + captured, excludes AI)
        launch {
            try {
                savedRecipeRepository.getManualRecipeCount().collect { count ->
                    _uiState.update { it.copy(manualRecipeCount = count) }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
        }
        // Last cooked name + days ago (for CookCard subtitle)
        launch {
            try {
                cookingLogRepository.getMostRecentWithName().collect { log ->
                    val daysAgo = log?.let {
                        val diffMs = System.currentTimeMillis() - it.cookedDate
                        (diffMs / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    }
                    _uiState.update { it.copy(lastCookedName = log?.recipeName, lastCookedDaysAgo = daysAgo) }
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
        com.inventory.app.ui.screens.items.ItemFormViewModel.pendingTourMode.set(true)
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

    fun tossItem(itemId: Long) {
        viewModelScope.launch {
            itemRepository.softDelete(itemId)
        }
    }

    fun markStillGood(itemId: Long) {
        viewModelScope.launch {
            val warningDays = settingsRepository.getInt(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 3)
            itemRepository.extendExpiry(itemId, warningDays.toLong())
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setAppTheme(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme) }
        viewModelScope.launch {
            settingsRepository.set(SettingsRepository.KEY_APP_THEME, theme.key)
        }
    }

    fun setVisualStyle(style: VisualStyle) {
        _uiState.update { it.copy(visualStyle = style) }
        viewModelScope.launch {
            settingsRepository.set(SettingsRepository.KEY_VISUAL_STYLE, style.key)
        }
    }
}
