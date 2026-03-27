package com.inventory.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.inventory.app.data.local.dao.SettingsDao
import com.inventory.app.data.local.entity.SettingsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.inventory.app.domain.model.MeasurementSystem
import com.inventory.app.domain.model.RegionRegistry
import com.inventory.app.util.FormatUtils
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val KEY_EXPIRY_WARNING_DAYS = "expiry_warning_days"

        const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        const val KEY_REGION_CODE = "region_code"
        const val KEY_DEFAULT_QUANTITY = "default_quantity"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_APP_THEME = "app_theme"
        const val KEY_VISUAL_STYLE = "visual_style"
        const val KEY_GROK_API_KEY = "grok_api_key"
        const val KEY_OPENAI_API_KEY = "openai_api_key"
        const val KEY_SHOPPING_BUDGET = "shopping_budget"
        const val KEY_AUTO_CLEAR_DAYS = "auto_clear_purchased_days"
        const val KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_NOTIF_EXPIRY_ENABLED = "notif_expiry_enabled"
        const val KEY_NOTIF_RESTOCK_ENABLED = "notif_restock_enabled"
        const val KEY_NOTIF_SHOPPING_ENABLED = "notif_shopping_enabled"
        const val KEY_NOTIF_SENT_TIMESTAMPS = "notif_sent_timestamps"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check_ms"
        const val KEY_CORRECTION_CONSENT = "correction_consent"
        const val KEY_MEASUREMENT_SYSTEM = "measurement_system"
        const val KEY_DATE_FORMAT = "date_format"
        const val KEY_DASHBOARD_HIGHLIGHT_ENABLED = "dashboard_highlight_enabled"
        const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
        const val KEY_LAST_BACKUP_ITEM_COUNT = "last_backup_item_count"
        const val KEY_REPORTS_EVER_VIEWED = "reports_ever_viewed"
        const val KEY_COOK_FEATURE_USED = "cook_feature_used"

        private val SECURE_KEYS = setOf(KEY_GROK_API_KEY, KEY_OPENAI_API_KEY)
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getSecureString(key: String, default: String = ""): String =
        encryptedPrefs.getString(key, default) ?: default

    fun setSecureString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    private fun now(): Long = LocalDateTime.now()
        .atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

    suspend fun getString(key: String, default: String = ""): String =
        settingsDao.get(key)?.value ?: default

    suspend fun getInt(key: String, default: Int = 0): Int =
        settingsDao.get(key)?.value?.toIntOrNull() ?: default

    suspend fun getDouble(key: String, default: Double = 0.0): Double =
        settingsDao.get(key)?.value?.toDoubleOrNull() ?: default

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean =
        settingsDao.get(key)?.value?.toBooleanStrictOrNull() ?: default

    fun getStringFlow(key: String, default: String = ""): Flow<String> =
        settingsDao.getFlow(key).map { it?.value ?: default }

    fun getIntFlow(key: String, default: Int = 0): Flow<Int> =
        settingsDao.getFlow(key).map { it?.value?.toIntOrNull() ?: default }

    fun getBooleanFlow(key: String, default: Boolean = false): Flow<Boolean> =
        settingsDao.getFlow(key).map { it?.value?.toBooleanStrictOrNull() ?: default }

    fun getAllSettings(): Flow<List<SettingsEntity>> = settingsDao.getAll()

    suspend fun set(key: String, value: String, valueType: String = "string", description: String? = null) {
        // Use REPLACE strategy to handle concurrent inserts safely (last-writer-wins)
        val existing = settingsDao.get(key)
        settingsDao.insert(
            SettingsEntity(
                id = existing?.id ?: 0,
                key = key,
                value = value,
                valueType = valueType,
                description = description ?: existing?.description
            )
        )
    }

    suspend fun setInt(key: String, value: Int) = set(key, value.toString(), "integer")
    suspend fun setBoolean(key: String, value: Boolean) = set(key, value.toString(), "boolean")

    suspend fun getExpiryWarningDays(): Int = getInt(KEY_EXPIRY_WARNING_DAYS, 3)
    suspend fun getCurrencySymbol(): String = getString(KEY_CURRENCY_SYMBOL, FormatUtils.getDefaultCurrencySymbol())
    suspend fun getRegionCode(): String = getString(KEY_REGION_CODE, "US")

    suspend fun getMeasurementSystem(): MeasurementSystem {
        val override = getString(KEY_MEASUREMENT_SYSTEM, "")
        if (override.isNotBlank()) {
            return try { MeasurementSystem.valueOf(override) } catch (_: Exception) { MeasurementSystem.METRIC }
        }
        val regionCode = getRegionCode()
        return RegionRegistry.findByCode(regionCode)?.measurementSystem ?: MeasurementSystem.METRIC
    }

    suspend fun getNotificationCountThisWeek(): Int {
        val raw = getString(KEY_NOTIF_SENT_TIMESTAMPS, "")
        if (raw.isBlank()) return 0
        val now = now()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
        return raw.split(",").count { ts ->
            val epoch = ts.trim().toLongOrNull() ?: return@count false
            epoch >= sevenDaysAgo
        }
    }

    suspend fun recordNotificationSent() {
        val raw = getString(KEY_NOTIF_SENT_TIMESTAMPS, "")
        val now = now()
        val existing = if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
        val recent = (existing + now).takeLast(10)
        set(KEY_NOTIF_SENT_TIMESTAMPS, recent.joinToString(","))
    }

    /** Clear encrypted preferences (API keys). Room tables are cleared separately via clearAllTables(). */
    fun clearEncryptedPrefs() {
        encryptedPrefs.edit().clear().apply()
    }
}
