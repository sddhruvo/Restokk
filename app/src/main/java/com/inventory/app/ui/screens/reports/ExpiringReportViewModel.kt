package com.inventory.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpiringReportUiState(
    val expiredItems: List<ItemWithDetails> = emptyList(),
    val expiringItems: List<ItemWithDetails> = emptyList(),
    val expiredCount: Int = 0,
    val expiringCount: Int = 0,
    val warningDays: Int = 7,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExpiringReportViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpiringReportUiState())
    val uiState = _uiState.asStateFlow()

    private var dataJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            val days = settingsRepository.getInt(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 7)
            _uiState.update { it.copy(warningDays = days) }
            loadData()
        }
    }

    private fun loadData() {
        dataJobs.forEach { it.cancel() }
        dataJobs.clear()

        val days = _uiState.value.warningDays
        dataJobs.add(viewModelScope.launch {
            try {
                itemRepository.getExpiredItems().collect { items ->
                    _uiState.update { it.copy(expiredItems = items, expiredCount = items.size) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load expired items: ${e.message}", isLoading = false) }
            }
        })
        dataJobs.add(viewModelScope.launch {
            try {
                itemRepository.getExpiringItemsReport(days).collect { items ->
                    _uiState.update { it.copy(expiringItems = items, expiringCount = items.size, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load expiring items: ${e.message}", isLoading = false) }
            }
        })
    }

    fun updateWarningDays(days: Int) {
        _uiState.update { it.copy(warningDays = days, isLoading = true) }
        loadData()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
