package com.inventory.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.repository.AuthRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val expiryWarningDays: String = "7",
    val currencySymbol: String = "",
    val defaultQuantity: String = "1",
    val appTheme: AppTheme = AppTheme.CLASSIC_GREEN,
    val shoppingBudget: String = "",
    val autoClearDays: String = "",
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val expiryWarningDaysError: String? = null,
    val currencyError: String? = null,
    val defaultQuantityError: String? = null,
    val shoppingBudgetError: String? = null,
    val autoClearDaysError: String? = null,
    // Auth state
    val userEmail: String? = null,
    val userName: String? = null,
    val userPhotoUrl: String? = null,
    val isSignedIn: Boolean = false,
    val isAnonymous: Boolean = true,
    val authLoading: Boolean = false,
    val authError: String? = null,
    // Notification settings (instant-save, not deferred to Save button)
    val notificationsEnabled: Boolean = true,
    val notifExpiryEnabled: Boolean = true,
    val notifRestockEnabled: Boolean = true,
    val notifShoppingEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                _uiState.update {
                    it.copy(
                        isSignedIn = user != null,
                        isAnonymous = user?.isAnonymous ?: true,
                        userEmail = user?.email,
                        userName = user?.displayName,
                        userPhotoUrl = user?.photoUrl?.toString()
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null) }
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        authLoading = false,
                        isSignedIn = true,
                        isAnonymous = false,
                        userEmail = user.email,
                        userName = user.displayName,
                        userPhotoUrl = user.photoUrl?.toString(),
                        authError = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(authLoading = false, authError = error.message)
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val expiryDays = settingsRepository.getInt(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 7)
            val currency = settingsRepository.getString(SettingsRepository.KEY_CURRENCY_SYMBOL, FormatUtils.getDefaultCurrencySymbol())
            val defaultQty = settingsRepository.getString(SettingsRepository.KEY_DEFAULT_QUANTITY, "1")
            val budget = settingsRepository.getString(SettingsRepository.KEY_SHOPPING_BUDGET, "")
            val autoClear = settingsRepository.getString(SettingsRepository.KEY_AUTO_CLEAR_DAYS, "")

            // Theme: read new key, migrate from old dark_mode if needed
            var themeKey = settingsRepository.getString(SettingsRepository.KEY_APP_THEME, "")
            if (themeKey.isEmpty()) {
                val wasDark = settingsRepository.getBoolean(SettingsRepository.KEY_DARK_MODE, false)
                val migrated = if (wasDark) AppTheme.AMOLED_DARK else AppTheme.CLASSIC_GREEN
                settingsRepository.set(SettingsRepository.KEY_APP_THEME, migrated.key)
                themeKey = migrated.key
            }

            // Notification settings (default: all enabled)
            val notifEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIFICATIONS_ENABLED, true)
            val notifExpiry = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_EXPIRY_ENABLED, true)
            val notifRestock = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_RESTOCK_ENABLED, true)
            val notifShopping = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_SHOPPING_ENABLED, true)

            _uiState.update {
                it.copy(
                    expiryWarningDays = expiryDays.toString(),
                    currencySymbol = currency,
                    defaultQuantity = defaultQty,
                    appTheme = AppTheme.fromKey(themeKey),
                    shoppingBudget = budget,
                    autoClearDays = autoClear,
                    isLoading = false,
                    notificationsEnabled = notifEnabled,
                    notifExpiryEnabled = notifExpiry,
                    notifRestockEnabled = notifRestock,
                    notifShoppingEnabled = notifShopping
                )
            }
        }
    }

    fun updateExpiryWarningDays(v: String) { _uiState.update { it.copy(expiryWarningDays = v, isSaved = false, expiryWarningDaysError = null) } }
    fun updateCurrencySymbol(v: String) { _uiState.update { it.copy(currencySymbol = v, isSaved = false, currencyError = null) } }
    fun updateDefaultQuantity(v: String) { _uiState.update { it.copy(defaultQuantity = v, isSaved = false, defaultQuantityError = null) } }
    fun updateShoppingBudget(v: String) { _uiState.update { it.copy(shoppingBudget = v, isSaved = false, shoppingBudgetError = null) } }
    fun updateAutoClearDays(v: String) { _uiState.update { it.copy(autoClearDays = v, isSaved = false, autoClearDaysError = null) } }

    fun toggleNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setBoolean(SettingsRepository.KEY_NOTIFICATIONS_ENABLED, enabled) }
    }
    fun toggleNotifExpiry(enabled: Boolean) {
        _uiState.update { it.copy(notifExpiryEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setBoolean(SettingsRepository.KEY_NOTIF_EXPIRY_ENABLED, enabled) }
    }
    fun toggleNotifRestock(enabled: Boolean) {
        _uiState.update { it.copy(notifRestockEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setBoolean(SettingsRepository.KEY_NOTIF_RESTOCK_ENABLED, enabled) }
    }
    fun toggleNotifShopping(enabled: Boolean) {
        _uiState.update { it.copy(notifShoppingEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setBoolean(SettingsRepository.KEY_NOTIF_SHOPPING_ENABLED, enabled) }
    }

    fun updateAppTheme(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme) }
        viewModelScope.launch {
            settingsRepository.set(SettingsRepository.KEY_APP_THEME, theme.key)
        }
    }

    fun resetOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setBoolean(OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false)
            onDone()
        }
    }

    fun save() {
        val state = _uiState.value
        var hasError = false

        val warningDays = state.expiryWarningDays.toIntOrNull()
        if (warningDays == null || warningDays < 1 || warningDays > 365) {
            _uiState.update { it.copy(expiryWarningDaysError = "Enter a number between 1 and 365") }
            hasError = true
        }
        if (state.currencySymbol.isBlank()) {
            _uiState.update { it.copy(currencyError = "Currency symbol is required") }
            hasError = true
        }
        val defaultQty = state.defaultQuantity.toDoubleOrNull()
        if (defaultQty == null || defaultQty <= 0) {
            _uiState.update { it.copy(defaultQuantityError = "Enter a positive number") }
            hasError = true
        }

        if (state.shoppingBudget.isNotBlank()) {
            val budget = state.shoppingBudget.toDoubleOrNull()
            if (budget == null || budget < 0) {
                _uiState.update { it.copy(shoppingBudgetError = "Enter a positive number") }
                hasError = true
            }
        }
        if (state.autoClearDays.isNotBlank()) {
            val days = state.autoClearDays.toIntOrNull()
            if (days == null || days < 1) {
                _uiState.update { it.copy(autoClearDaysError = "Enter a number of 1 or more") }
                hasError = true
            }
        }

        if (hasError) return

        val validWarningDays = warningDays ?: return
        viewModelScope.launch {
            settingsRepository.setInt(
                SettingsRepository.KEY_EXPIRY_WARNING_DAYS,
                validWarningDays
            )
            settingsRepository.set(SettingsRepository.KEY_CURRENCY_SYMBOL, state.currencySymbol)
            settingsRepository.set(SettingsRepository.KEY_DEFAULT_QUANTITY, state.defaultQuantity)
            settingsRepository.set(SettingsRepository.KEY_SHOPPING_BUDGET, state.shoppingBudget)
            settingsRepository.set(SettingsRepository.KEY_AUTO_CLEAR_DAYS, state.autoClearDays)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
