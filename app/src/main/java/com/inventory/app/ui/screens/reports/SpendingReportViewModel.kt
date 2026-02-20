package com.inventory.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.DailySpendingRow
import com.inventory.app.data.local.dao.FrequentPurchaseRow
import com.inventory.app.data.local.dao.PurchaseWithItemDetails
import com.inventory.app.data.local.dao.SpendingByCategoryRow
import com.inventory.app.data.local.dao.SpendingByStoreRow
import com.inventory.app.data.repository.PurchaseRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SpendingReportUiState(
    val totalSpending: Double = 0.0,
    val previousPeriodSpending: Double = 0.0,
    val spendingByCategory: List<SpendingByCategoryRow> = emptyList(),
    val spendingByStore: List<SpendingByStoreRow> = emptyList(),
    val dailySpending: List<DailySpendingRow> = emptyList(),
    val previousPeriodDailySpending: List<DailySpendingRow> = emptyList(),
    val allPurchasesForPeriod: List<PurchaseWithItemDetails> = emptyList(),
    val mostBoughtItems: List<FrequentPurchaseRow> = emptyList(),
    val averagePerDay: Double = 0.0,
    val fairAvgPerDay: Double = 0.0,
    val previousFairAvgPerDay: Double = 0.0,
    val purchaseCount: Int = 0,
    val selectedPeriod: SpendingPeriod = SpendingPeriod.THIS_MONTH,
    val isLoading: Boolean = true,
    val currencySymbol: String = "",
    val error: String? = null
)

enum class SpendingPeriod(val label: String, val days: Long) {
    THIS_WEEK("Week", 7),
    THIS_MONTH("Month", 30),
    THREE_MONTHS("3 Months", 90),
    THIS_YEAR("Year", 365)
}

@HiltViewModel
class SpendingReportViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpendingReportUiState())
    val uiState = _uiState.asStateFlow()

    private var dataJobs = mutableListOf<Job>()

    init {
        loadData()
        viewModelScope.launch {
            settingsRepository.getStringFlow(SettingsRepository.KEY_CURRENCY_SYMBOL, FormatUtils.getDefaultCurrencySymbol())
                .collect { symbol ->
                    _uiState.update { it.copy(currencySymbol = symbol) }
                }
        }
    }

    private fun loadData() {
        dataJobs.forEach { it.cancel() }
        dataJobs.clear()

        val period = _uiState.value.selectedPeriod
        val since = LocalDate.now().minusDays(period.days)
        val previousStart = since.minusDays(period.days)

        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getTotalSpendingSince(since).collect { total ->
                    _uiState.update {
                        it.copy(
                            totalSpending = total,
                            averagePerDay = if (period.days > 0) total / period.days else 0.0
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load spending data: ${e.message}", isLoading = false) }
            }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getTotalSpendingBetween(previousStart, since).collect { total ->
                    _uiState.update { it.copy(previousPeriodSpending = total) }
                }
            } catch (_: Exception) { }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getSpendingByCategory(since).collect { data ->
                    _uiState.update { it.copy(spendingByCategory = data, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load spending data: ${e.message}", isLoading = false) }
            }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getSpendingByStore(since).collect { data ->
                    _uiState.update { it.copy(spendingByStore = data) }
                }
            } catch (_: Exception) { }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getDailySpending(since).collect { data ->
                    val activeDays = data.count { it.amount > 0 }.coerceAtLeast(1)
                    val total = data.sumOf { it.amount }
                    _uiState.update { it.copy(
                        dailySpending = data,
                        fairAvgPerDay = total / activeDays
                    ) }
                }
            } catch (_: Exception) { }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getDailySpendingBetween(previousStart, since).collect { data ->
                    val activeDays = data.count { it.amount > 0 }.coerceAtLeast(1)
                    val total = data.sumOf { it.amount }
                    _uiState.update { it.copy(
                        previousPeriodDailySpending = data,
                        previousFairAvgPerDay = total / activeDays
                    ) }
                }
            } catch (_: Exception) { }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getAllWithDetailsSince(since).collect { data ->
                    _uiState.update { it.copy(allPurchasesForPeriod = data) }
                }
            } catch (_: Exception) { }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                purchaseRepository.getPurchaseCount(since).collect { count ->
                    _uiState.update { it.copy(purchaseCount = count) }
                }
            } catch (_: Exception) { }
        })
        // Most bought - all-time, one-shot
        dataJobs.add(viewModelScope.launch {
            try {
                val items = purchaseRepository.getFrequentlyPurchasedItems(10)
                _uiState.update { it.copy(mostBoughtItems = items) }
            } catch (_: Exception) { }
        })
    }

    fun updatePeriod(period: SpendingPeriod) {
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }
        loadData()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
