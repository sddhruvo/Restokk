package com.inventory.app.data.sync.model

import com.inventory.app.data.repository.SettingsRepository

/**
 * Constants for Firestore cloud backup & sync.
 */
object SyncConstants {
    // Firestore collection paths (all under users/{uid}/)
    const val COLLECTION_USERS = "users"
    const val COLLECTION_INVENTORY = "inventory"
    const val COLLECTION_SHOPPING = "shopping"
    const val COLLECTION_RECIPES = "recipes"
    const val COLLECTION_CATEGORIES = "categories"
    const val COLLECTION_SUBCATEGORIES = "subcategories"
    const val COLLECTION_STORAGE_LOCATIONS = "storage_locations"
    const val COLLECTION_UNITS = "units"
    const val COLLECTION_SETTINGS = "settings"
    const val COLLECTION_METADATA = "metadata"
    const val DOC_SYNC_STATE = "sync_state"

    // Free tier limits
    const val FREE_TIER_ITEM_LIMIT = 75
    const val FREE_TIER_RECIPE_LIMIT = 10

    // Rate limiting
    const val BACKUP_COOLDOWN_MS = 3_600_000L // 1 hour

    // Firestore field for storing original Room ID (used for FK remapping on restore)
    const val FIELD_ORIGINAL_ID = "originalId"

    /**
     * Allowlist of settings keys safe to sync to cloud.
     * Never includes API keys, notification timestamps, or legacy keys.
     */
    val SETTINGS_KEYS_TO_SYNC = setOf(
        SettingsRepository.KEY_EXPIRY_WARNING_DAYS,
        SettingsRepository.KEY_CURRENCY_SYMBOL,
        SettingsRepository.KEY_REGION_CODE,
        SettingsRepository.KEY_DEFAULT_QUANTITY,
        SettingsRepository.KEY_APP_THEME,
        SettingsRepository.KEY_VISUAL_STYLE,
        SettingsRepository.KEY_SHOPPING_BUDGET,
        SettingsRepository.KEY_AUTO_CLEAR_DAYS,
        SettingsRepository.KEY_LOW_STOCK_THRESHOLD,
        SettingsRepository.KEY_MEASUREMENT_SYSTEM,
        SettingsRepository.KEY_DATE_FORMAT,
        SettingsRepository.KEY_DASHBOARD_HIGHLIGHT_ENABLED,
        SettingsRepository.KEY_NOTIFICATIONS_ENABLED,
        SettingsRepository.KEY_NOTIF_EXPIRY_ENABLED,
        SettingsRepository.KEY_NOTIF_RESTOCK_ENABLED,
        SettingsRepository.KEY_NOTIF_SHOPPING_ENABLED,
        SettingsRepository.KEY_CORRECTION_CONSENT
    )
}
