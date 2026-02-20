package com.inventory.app.ui.screens.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val SS_PAGE_INDEX = "ss_page_index"
        private const val SS_REGION_CODE = "ss_region_code"
        private const val SS_START_CHOICE = "ss_start_choice"
        private const val SS_SHOW_REGION_PICKER = "ss_show_region_picker"
    }

    data class UiState(
        val currentPageIndex: Int = 0,
        val detectedRegion: RegionInfo = detectRegion(),
        val selectedRegion: RegionInfo = detectRegion(),
        val showRegionPicker: Boolean = false,
        val startChoice: StartChoice? = null,
        val isCompleting: Boolean = false
    ) {
        val currentPage: OnboardingPage get() = onboardingPages[currentPageIndex]
        val pageCount: Int get() = onboardingPages.size
        val isFirstPage: Boolean get() = currentPageIndex == 0
        val isLastPage: Boolean get() = currentPageIndex == onboardingPages.lastIndex
        val canSkip: Boolean get() = currentPage.canSkip
    }

    private val _uiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private fun restoreState(): UiState {
        val pageIndex = savedStateHandle.get<Int>(SS_PAGE_INDEX) ?: 0
        val regionCode = savedStateHandle.get<String>(SS_REGION_CODE)
        val choiceName = savedStateHandle.get<String>(SS_START_CHOICE)
        val showPicker = savedStateHandle.get<Boolean>(SS_SHOW_REGION_PICKER) ?: false

        val detected = detectRegion()
        val region = if (regionCode != null) {
            popularRegions.find { it.countryCode == regionCode } ?: detected
        } else detected

        val choice = choiceName?.let {
            try { StartChoice.valueOf(it) } catch (_: Exception) { null }
        }

        return UiState(
            currentPageIndex = pageIndex.coerceIn(0, onboardingPages.lastIndex),
            detectedRegion = detected,
            selectedRegion = region,
            showRegionPicker = showPicker,
            startChoice = choice
        )
    }

    private fun persist(state: UiState) {
        savedStateHandle[SS_PAGE_INDEX] = state.currentPageIndex
        savedStateHandle[SS_REGION_CODE] = state.selectedRegion.countryCode
        savedStateHandle[SS_START_CHOICE] = state.startChoice?.name
        savedStateHandle[SS_SHOW_REGION_PICKER] = state.showRegionPicker
    }

    private inline fun updateState(crossinline transform: (UiState) -> UiState) {
        _uiState.update { old ->
            val new = transform(old)
            persist(new)
            new
        }
    }

    fun goToPage(index: Int) {
        val clamped = index.coerceIn(0, onboardingPages.lastIndex)
        updateState { it.copy(currentPageIndex = clamped) }
    }

    fun nextPage() {
        updateState { state ->
            if (!state.isLastPage) state.copy(currentPageIndex = state.currentPageIndex + 1)
            else state
        }
    }

    fun previousPage() {
        updateState { state ->
            if (!state.isFirstPage) state.copy(currentPageIndex = state.currentPageIndex - 1)
            else state
        }
    }

    fun selectRegion(region: RegionInfo) {
        updateState { it.copy(selectedRegion = region, showRegionPicker = false) }
    }

    fun toggleRegionPicker() {
        updateState { it.copy(showRegionPicker = !it.showRegionPicker) }
    }

    fun setStartChoice(choice: StartChoice) {
        updateState { it.copy(startChoice = choice) }
    }

    fun confirmRegionAndAdvance() {
        val region = _uiState.value.selectedRegion
        viewModelScope.launch {
            settingsRepository.set(
                SettingsRepository.KEY_CURRENCY_SYMBOL,
                region.currencySymbol,
                "string",
                "Currency symbol"
            )
        }
        nextPage()
    }

    fun completeOnboarding(onComplete: (StartChoice?) -> Unit) {
        if (_uiState.value.isCompleting) return
        _uiState.update { it.copy(isCompleting = true) }
        val choice = _uiState.value.startChoice
        viewModelScope.launch {
            settingsRepository.setBoolean(KEY_ONBOARDING_COMPLETED, true)
            onComplete(choice)
        }
    }

    fun skipOnboarding(onComplete: () -> Unit) {
        if (_uiState.value.isCompleting) return
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            settingsRepository.setBoolean(KEY_ONBOARDING_COMPLETED, true)
            onComplete()
        }
    }
}
