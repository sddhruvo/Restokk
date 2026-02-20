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
import com.inventory.app.util.FormatUtils
import java.time.LocalDateTime
import java.time.ZoneId
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
        const val KEY_DEFAULT_QUANTITY = "default_quantity"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_APP_THEME = "app_theme"
        const val KEY_GROK_API_KEY = "grok_api_key"
        const val KEY_OPENAI_API_KEY = "openai_api_key"
        const val KEY_SHOPPING_BUDGET = "shopping_budget"
        const val KEY_AUTO_CLEAR_DAYS = "auto_clear_purchased_days"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_NOTIF_EXPIRY_ENABLED = "notif_expiry_enabled"
        const val KEY_NOTIF_RESTOCK_ENABLED = "notif_restock_enabled"
        const val KEY_NOTIF_SHOPPING_ENABLED = "notif_shopping_enabled"
        const val KEY_NOTIF_SENT_TIMESTAMPS = "notif_sent_timestamps"

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
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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

    fun getAllSettings(): Flow<List<SettingsEntity>> = settingsDao.getAll()

    suspend fun set(key: String, value: String, valueType: String = "string", description: String? = null) {
        val existing = settingsDao.get(key)
        if (existing != null) {
            settingsDao.updateValue(key, value, now())
        } else {
            settingsDao.insert(
                SettingsEntity(
                    key = key,
                    value = value,
                    valueType = valueType,
                    description = description
                )
            )
        }
    }

    suspend fun setInt(key: String, value: Int) = set(key, value.toString(), "integer")
    suspend fun setBoolean(key: String, value: Boolean) = set(key, value.toString(), "boolean")

    suspend fun getExpiryWarningDays(): Int = getInt(KEY_EXPIRY_WARNING_DAYS, 7)
    suspend fun getCurrencySymbol(): String = getString(KEY_CURRENCY_SYMBOL, FormatUtils.getDefaultCurrencySymbol())

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
}
