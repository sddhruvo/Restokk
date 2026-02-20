package com.inventory.app.ui.screens.pantryhealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.PantryHealthRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.domain.model.HomeScoreCalculator
import com.inventory.app.domain.model.ScoreFactor
import com.inventory.app.domain.tips.Tip
import com.inventory.app.domain.tips.TipCategory
import com.inventory.app.domain.tips.TipsEngine
import com.inventory.app.ui.components.DailyChartEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PantryHealthUiState(
    val score: Int = 0,
    val scoreLabel: String = "No Items",
    val engagementScore: Int = 0,
    val conditionScore: Int = 0,
    val motivationText: String = "",
    val trendDelta: Int = 0,
    val engagementFactors: List<ScoreFactor> = emptyList(),
    val conditionFactors: List<ScoreFactor> = emptyList(),
    val chartEntries: List<DailyChartEntry> = emptyList(),
    val selectedPeriod: Int = 7,
    val tips: List<Tip> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PantryHealthViewModel @Inject constructor(
    private val pantryHealthRepository: PantryHealthRepository,
    private val itemRepository: ItemRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val tipsEngine: TipsEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryHealthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadScoreFactors()
        loadChart(7)
        loadTips()
    }

    private fun loadScoreFactors() {
        viewModelScope.launch {
            try {
                combine(
                    itemRepository.getTotalItemCount(),
                    itemRepository.getItemsWithCategoryCount(),
                    itemRepository.getItemsWithLocationCount(),
                    itemRepository.getItemsWithExpiryCount(),
                    itemRepository.getExpiredCount(),
                    itemRepository.getExpiringSoonCount(7),
                    itemRepository.getLowStockCount(),
                    itemRepository.getOutOfStockCount(),
                    shoppingListRepository.getActiveCount(),
                    shoppingListRepository.getPurchasedCount()
                ) { values ->
                    if (values.size < 10) return@combine null
                    val totalItems = values[0] as Int
                    val withCategory = values[1] as Int
                    val withLocation = values[2] as Int
                    val withExpiry = values[3] as Int
                    val expired = values[4] as Int
                    val expiringSoon = values[5] as Int
                    val lowStock = values[6] as Int
                    val outOfStock = values[7] as Int
                    val shoppingActive = values[8] as Int
                    val shoppingPurchased = values[9] as Int

                    HomeScoreCalculator.compute(
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
                }.collect { breakdown ->
                    breakdown ?: return@collect
                    val nextThreshold = when {
                        breakdown.finalScore >= 85 -> null
                        breakdown.finalScore >= 70 -> 85
                        breakdown.finalScore >= 50 -> 70
                        breakdown.finalScore >= 30 -> 50
                        breakdown.finalScore >= 1 -> 30
                        else -> null
                    }
                    val nextLabel = when {
                        breakdown.finalScore >= 85 -> null
                        breakdown.finalScore >= 70 -> "Excellent"
                        breakdown.finalScore >= 50 -> "Great"
                        breakdown.finalScore >= 30 -> "Good"
                        breakdown.finalScore >= 1 -> "Getting There"
                        else -> null
                    }
                    val motivation = when {
                        breakdown.finalScore == 0 -> "Add items to start building your Home Score!"
                        nextThreshold != null && nextLabel != null ->
                            "${nextThreshold - breakdown.finalScore} points to $nextLabel!"
                        else -> "Excellent! Your home inventory is in top shape."
                    }

                    _uiState.update { it.copy(
                        score = breakdown.finalScore,
                        scoreLabel = breakdown.label,
                        engagementScore = breakdown.engagementScore,
                        conditionScore = breakdown.conditionScore,
                        motivationText = motivation,
                        engagementFactors = breakdown.engagementFactors,
                        conditionFactors = breakdown.conditionFactors,
                        isLoading = false
                    ) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        // Load trend
        viewModelScope.launch {
            try {
                pantryHealthRepository.getLogsSince(LocalDate.now().minusDays(8))
                    .collect { logs ->
                        if (logs.size >= 2) {
                            val oldestScore = logs.first().score
                            val newestScore = logs.last().score
                            _uiState.update { it.copy(trendDelta = newestScore - oldestScore) }
                        }
                    }
            } catch (_: Exception) { }
        }
    }

    fun selectPeriod(days: Int) {
        _uiState.update { it.copy(selectedPeriod = days) }
        loadChart(days)
    }

    private fun loadChart(days: Int) {
        viewModelScope.launch {
            try {
                val sinceDate = LocalDate.now().minusDays(days.toLong())
                pantryHealthRepository.getLogsSince(sinceDate).collect { logs ->
                    val formatter = DateTimeFormatter.ofPattern("M/d")
                    val entries = logs.map { log ->
                        DailyChartEntry(
                            label = log.date.format(formatter),
                            value = log.score.toFloat()
                        )
                    }
                    _uiState.update { it.copy(chartEntries = entries) }
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadTips() {
        viewModelScope.launch {
            try {
                val tips = tipsEngine.getTips(category = TipCategory.PANTRY_HEALTH, limit = 5)
                _uiState.update { it.copy(tips = tips) }
            } catch (_: Exception) { }
        }
    }
}
