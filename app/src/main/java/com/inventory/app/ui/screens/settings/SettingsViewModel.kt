package com.inventory.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.db.InventoryDatabase
import com.inventory.app.data.repository.AuthRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.sync.BackupRepository
import com.inventory.app.data.sync.RestorePromptManager
import com.inventory.app.data.sync.model.BackupEligibility
import com.inventory.app.data.sync.model.BackupMetadata
import com.inventory.app.data.sync.model.BackupStatus
import com.inventory.app.data.sync.model.RestoreResult
import com.inventory.app.domain.model.RegionRegistry
import com.inventory.app.ui.screens.onboarding.RegionInfo
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.ui.theme.VisualStyle
import com.inventory.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val expiryWarningDays: String = "3",
    val currencySymbol: String = "",
    val defaultQuantity: String = "1",
    val appTheme: AppTheme = AppTheme.CLASSIC_GREEN,
    val visualStyle: VisualStyle = VisualStyle.MODERN,
    val shoppingBudget: String = "",
    val autoClearDays: String = "",
    val lowStockThreshold: String = "25",
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val hasBeenTouched: Boolean = false,
    val expiryWarningDaysError: String? = null,
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
    // Region & measurement
    val regionCode: String = "US",
    val regionName: String = "United States",
    val regionFlag: String = "\uD83C\uDDFA\uD83C\uDDF8",
    val measurementSystem: String = "",  // "" = auto, "METRIC", "IMPERIAL"
    val dateFormat: String = "",  // "" = auto from region, "MONTH_FIRST", "DAY_FIRST"
    val showRegionPicker: Boolean = false,
    // Notification settings (instant-save, not deferred to Save button)
    val notificationsEnabled: Boolean = true,
    val notifExpiryEnabled: Boolean = true,
    val notifRestockEnabled: Boolean = true,
    val notifShoppingEnabled: Boolean = true,
    val userPreference: String = "INVENTORY",
    val dashboardHighlightEnabled: Boolean = true,
    // Backup state
    val backupStatus: BackupStatus = BackupStatus.Idle,
    val lastBackupDate: String? = null,
    val lastBackupItemCount: Int = 0,
    val totalItemCount: Int = 0,
    val backupEligibility: BackupEligibility = BackupEligibility.NotSignedIn,
    // Restore state
    val showRestoreDialog: Boolean = false,
    val restorePromptData: BackupMetadata? = null,
    val restoreHasLocalData: Boolean = false,
    val restoreInProgress: Boolean = false,
    val restoreResult: RestoreResult? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val database: InventoryDatabase,
    private val backupRepository: BackupRepository,
    private val restorePromptManager: RestorePromptManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
        observeAuthState()
        observeTotalItemCount()
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
                refreshBackupStatus()
            }
        }
    }

    private fun observeTotalItemCount() {
        viewModelScope.launch {
            database.itemDao().getTotalItemCount().collect { count ->
                _uiState.update { it.copy(totalItemCount = count) }
            }
        }
    }

    fun refreshBackupStatus() {
        viewModelScope.launch {
            val eligibility = backupRepository.canBackup()
            val lastTimestamp = settingsRepository.getString(
                SettingsRepository.KEY_LAST_BACKUP_TIMESTAMP, "0"
            ).toLongOrNull() ?: 0L
            val lastCount = settingsRepository.getInt(
                SettingsRepository.KEY_LAST_BACKUP_ITEM_COUNT, 0
            )

            val lastDateStr = if (lastTimestamp > 0L) {
                FormatUtils.formatRelativeTime(lastTimestamp)
            } else null

            _uiState.update {
                it.copy(
                    backupEligibility = eligibility,
                    lastBackupDate = lastDateStr,
                    lastBackupItemCount = lastCount
                )
            }
        }
    }

    fun performBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(backupStatus = BackupStatus.InProgress) }
            val result = backupRepository.performBackup()
            result.onSuccess { metadata ->
                _uiState.update {
                    it.copy(
                        backupStatus = BackupStatus.Success(metadata),
                        lastBackupDate = FormatUtils.formatRelativeTime(metadata.lastBackupAt),
                        lastBackupItemCount = metadata.itemCount
                    )
                }
                refreshBackupStatus()
                // Auto-dismiss success after 3 seconds
                delay(3000)
                _uiState.update { state ->
                    if (state.backupStatus is BackupStatus.Success) {
                        state.copy(backupStatus = BackupStatus.Idle)
                    } else state
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(backupStatus = BackupStatus.Failed(error.message ?: "Backup failed"))
                }
                // Auto-dismiss error after 5 seconds
                delay(5000)
                _uiState.update { state ->
                    if (state.backupStatus is BackupStatus.Failed) {
                        state.copy(backupStatus = BackupStatus.Idle)
                    } else state
                }
            }
        }
    }

    fun dismissBackupStatus() {
        _uiState.update { it.copy(backupStatus = BackupStatus.Idle) }
    }


    // ─── Restore Flow ────────────────────────────────────────────

    private fun checkForRestorePrompt() {
        viewModelScope.launch {
            try {
                val metadata = restorePromptManager.checkForExistingBackup() ?: return@launch
                val hasLocal = restorePromptManager.hasLocalData()
                _uiState.update {
                    it.copy(
                        showRestoreDialog = true,
                        restorePromptData = metadata,
                        restoreHasLocalData = hasLocal
                    )
                }
            } catch (_: Exception) {
                // Silently fail — don't block sign-in flow
            }
        }
    }

    fun performRestore() {
        val mergeWithLocal = _uiState.value.restoreHasLocalData
        viewModelScope.launch {
            _uiState.update { it.copy(restoreInProgress = true) }
            val result = backupRepository.performRestore(mergeWithLocal)
            result.onSuccess { restoreResult ->
                _uiState.update {
                    it.copy(
                        showRestoreDialog = false,
                        restoreInProgress = false,
                        restoreResult = restoreResult,
                        restorePromptData = null
                    )
                }
                refreshBackupStatus()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        showRestoreDialog = false,
                        restoreInProgress = false,
                        restorePromptData = null,
                        authError = "Restore failed: ${error.message}"
                    )
                }
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update {
            it.copy(
                showRestoreDialog = false,
                restorePromptData = null
            )
        }
    }

    fun clearRestoreResult() {
        _uiState.update { it.copy(restoreResult = null) }
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
                // Check for existing cloud backup after successful sign-in
                checkForRestorePrompt()
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

    fun signInFailed(message: String) {
        _uiState.update { it.copy(authLoading = false, authError = message) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null) }
            // Delete cloud data before account deletion (Cloud Function safety net handles failures)
            try { backupRepository.deleteAllCloudData() } catch (_: Exception) { }
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                withContext(Dispatchers.IO) { database.clearAllTables() }
                settingsRepository.clearEncryptedPrefs()
                _uiState.update { it.copy(authLoading = false) }
                onComplete()
            } else {
                _uiState.update { it.copy(
                    authLoading = false,
                    authError = result.exceptionOrNull()?.message ?: "Failed to delete account"
                ) }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val expiryDays = settingsRepository.getInt(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 3)
            val currency = settingsRepository.getString(SettingsRepository.KEY_CURRENCY_SYMBOL, FormatUtils.getDefaultCurrencySymbol())
            val defaultQty = settingsRepository.getString(SettingsRepository.KEY_DEFAULT_QUANTITY, "1")
            val budget = settingsRepository.getString(SettingsRepository.KEY_SHOPPING_BUDGET, "")
            val autoClear = settingsRepository.getString(SettingsRepository.KEY_AUTO_CLEAR_DAYS, "")
            val lowStockThreshold = settingsRepository.getString(SettingsRepository.KEY_LOW_STOCK_THRESHOLD, "25")

            // Region & measurement
            val regionCode = settingsRepository.getString(SettingsRepository.KEY_REGION_CODE, "US")
            val regionConfig = RegionRegistry.findByCode(regionCode)
            val measurementOverride = settingsRepository.getString(SettingsRepository.KEY_MEASUREMENT_SYSTEM, "")
            val dateFormatOverride = settingsRepository.getString(SettingsRepository.KEY_DATE_FORMAT, "")
            FormatUtils.dateFormatOverride = dateFormatOverride

            // Theme: read new key, migrate from old dark_mode if needed
            var themeKey = settingsRepository.getString(SettingsRepository.KEY_APP_THEME, "")
            if (themeKey.isEmpty()) {
                val wasDark = settingsRepository.getBoolean(SettingsRepository.KEY_DARK_MODE, false)
                val migrated = if (wasDark) AppTheme.AMOLED_DARK else AppTheme.CLASSIC_GREEN
                settingsRepository.set(SettingsRepository.KEY_APP_THEME, migrated.key)
                themeKey = migrated.key
            }

            // Visual style
            val visualStyleKey = settingsRepository.getString(SettingsRepository.KEY_VISUAL_STYLE, VisualStyle.PAPER_INK.key)

            // Notification settings (default: all enabled)
            val notifEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIFICATIONS_ENABLED, true)
            val notifExpiry = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_EXPIRY_ENABLED, true)
            val notifRestock = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_RESTOCK_ENABLED, true)
            val notifShopping = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_SHOPPING_ENABLED, true)
            val preference = settingsRepository.getString(OnboardingViewModel.KEY_USER_PREFERENCE, "INVENTORY")
            val highlightEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_DASHBOARD_HIGHLIGHT_ENABLED, true)

            _uiState.update {
                it.copy(
                    expiryWarningDays = expiryDays.toString(),
                    currencySymbol = currency,
                    defaultQuantity = defaultQty,
                    appTheme = AppTheme.fromKey(themeKey),
                    visualStyle = VisualStyle.fromKey(visualStyleKey),
                    shoppingBudget = budget,
                    autoClearDays = autoClear,
                    lowStockThreshold = lowStockThreshold,
                    regionCode = regionCode,
                    regionName = regionConfig?.countryName ?: regionCode,
                    regionFlag = regionConfig?.flag ?: "\uD83C\uDF10",
                    measurementSystem = measurementOverride,
                    dateFormat = dateFormatOverride,
                    isLoading = false,
                    notificationsEnabled = notifEnabled,
                    notifExpiryEnabled = notifExpiry,
                    notifRestockEnabled = notifRestock,
                    notifShoppingEnabled = notifShopping,
                    userPreference = preference,
                    dashboardHighlightEnabled = highlightEnabled
                )
            }
        }
    }

    fun updateExpiryWarningDays(v: String) { _uiState.update { it.copy(expiryWarningDays = v, isSaved = false, expiryWarningDaysError = null, hasBeenTouched = true) } }
    fun updateDefaultQuantity(v: String) { _uiState.update { it.copy(defaultQuantity = v, isSaved = false, defaultQuantityError = null, hasBeenTouched = true) } }
    fun updateShoppingBudget(v: String) { _uiState.update { it.copy(shoppingBudget = v, isSaved = false, shoppingBudgetError = null, hasBeenTouched = true) } }
    fun updateAutoClearDays(v: String) { _uiState.update { it.copy(autoClearDays = v, isSaved = false, autoClearDaysError = null, hasBeenTouched = true) } }
    fun updateLowStockThreshold(v: String) { _uiState.update { it.copy(lowStockThreshold = v, isSaved = false, hasBeenTouched = true) } }

    fun toggleRegionPicker() {
        _uiState.update { it.copy(showRegionPicker = !it.showRegionPicker) }
    }

    fun updateRegion(region: RegionInfo) {
        _uiState.update {
            it.copy(
                regionCode = region.countryCode,
                regionName = region.countryName,
                regionFlag = region.flag,
                currencySymbol = region.currencySymbol,
                measurementSystem = "",  // reset to auto
                showRegionPicker = false,
                hasBeenTouched = true,
                isSaved = false
            )
        }
    }

    fun updateMeasurementSystem(system: String) {
        _uiState.update { it.copy(measurementSystem = system, hasBeenTouched = true, isSaved = false) }
    }

    fun updateDateFormat(format: String) {
        _uiState.update { it.copy(dateFormat = format, hasBeenTouched = true, isSaved = false) }
    }

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
    fun toggleDashboardHighlight(enabled: Boolean) {
        _uiState.update { it.copy(dashboardHighlightEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setBoolean(SettingsRepository.KEY_DASHBOARD_HIGHLIGHT_ENABLED, enabled) }
    }

    fun updateUserPreference(pref: String) {
        _uiState.update { it.copy(userPreference = pref) }
        viewModelScope.launch {
            settingsRepository.set(OnboardingViewModel.KEY_USER_PREFERENCE, pref)
        }
    }

    fun updateAppTheme(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme) }
        viewModelScope.launch {
            settingsRepository.set(SettingsRepository.KEY_APP_THEME, theme.key)
        }
    }

    fun updateVisualStyle(style: VisualStyle) {
        _uiState.update { it.copy(visualStyle = style) }
        viewModelScope.launch {
            settingsRepository.set(SettingsRepository.KEY_VISUAL_STYLE, style.key)
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
            settingsRepository.set(SettingsRepository.KEY_LOW_STOCK_THRESHOLD, state.lowStockThreshold)
            settingsRepository.set(SettingsRepository.KEY_REGION_CODE, state.regionCode)
            settingsRepository.set(SettingsRepository.KEY_MEASUREMENT_SYSTEM, state.measurementSystem)
            settingsRepository.set(SettingsRepository.KEY_DATE_FORMAT, state.dateFormat)
            FormatUtils.dateFormatOverride = state.dateFormat
            _uiState.update { it.copy(isSaved = true, hasBeenTouched = false) }
            delay(100)
            _uiState.update { it.copy(isSaved = false) }
        }
    }
}
