package com.inventory.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.TopUsageItemRow
import com.inventory.app.data.local.dao.UsageByTypeRow
import com.inventory.app.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class UsageReportUiState(
    val totalUsage: Double = 0.0,
    val usageByType: List<UsageByTypeRow> = emptyList(),
    val topConsumed: List<TopUsageItemRow> = emptyList(),
    val topWasted: List<TopUsageItemRow> = emptyList(),
    val selectedPeriod: SpendingPeriod = SpendingPeriod.THIS_MONTH,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class UsageReportViewModel @Inject constructor(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsageReportUiState())
    val uiState = _uiState.asStateFlow()

    private var dataJobs = mutableListOf<Job>()

    init {
        loadData()
    }

    private fun loadData() {
        dataJobs.forEach { it.cancel() }
        dataJobs.clear()

        val period = _uiState.value.selectedPeriod
        val since = LocalDate.now().minusDays(period.days)

        dataJobs.add(viewModelScope.launch {
            try {
                usageRepository.getTotalUsageSince(since).collect { total ->
                    _uiState.update { it.copy(totalUsage = total) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load usage data: ${e.message}", isLoading = false) }
            }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                usageRepository.getUsageByType(since).collect { data ->
                    _uiState.update { it.copy(usageByType = data, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load usage data: ${e.message}", isLoading = false) }
            }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                usageRepository.getTopItemsByUsageType(since, "consumed").collect { data ->
                    _uiState.update { it.copy(topConsumed = data) }
                }
            } catch (e: Exception) { /* non-critical */ }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                usageRepository.getTopItemsByUsageType(since, "wasted").collect { data ->
                    _uiState.update { it.copy(topWasted = data) }
                }
            } catch (e: Exception) { /* non-critical */ }
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
