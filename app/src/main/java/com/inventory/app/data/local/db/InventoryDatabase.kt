package com.inventory.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inventory.app.data.local.dao.*
import com.inventory.app.data.local.entity.*

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `app_notifications` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `deep_link_route` TEXT DEFAULT NULL,
                `cta_text` TEXT DEFAULT NULL,
                `cta_route` TEXT DEFAULT NULL,
                `priority` INTEGER NOT NULL DEFAULT 1,
                `created_at` INTEGER NOT NULL DEFAULT 0,
                `read_at` INTEGER DEFAULT NULL,
                `dismissed_at` INTEGER DEFAULT NULL,
                `expires_at` INTEGER DEFAULT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_notifications_type` ON `app_notifications` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_notifications_read_at` ON `app_notifications` (`read_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_notifications_dismissed_at` ON `app_notifications` (`dismissed_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_notifications_expires_at` ON `app_notifications` (`expires_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_notifications_priority_created_at` ON `app_notifications` (`priority`, `created_at`)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add 5 new columns to saved_recipes (all additive with safe defaults)
        db.execSQL("ALTER TABLE saved_recipes ADD COLUMN source TEXT NOT NULL DEFAULT 'ai'")
        db.execSQL("ALTER TABLE saved_recipes ADD COLUMN meal_type TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE saved_recipes ADD COLUMN tags TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE saved_recipes ADD COLUMN is_draft INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE saved_recipes ADD COLUMN cover_photo_uri TEXT DEFAULT NULL")

        // Create cooking_log table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cooking_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `recipe_id` INTEGER NOT NULL,
                `cooked_date` INTEGER NOT NULL DEFAULT 0,
                `servings` INTEGER NOT NULL DEFAULT 2,
                `deducted_items_json` TEXT,
                `notes` TEXT,
                FOREIGN KEY(`recipe_id`) REFERENCES `saved_recipes`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cooking_log_recipe_id` ON `cooking_log` (`recipe_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cooking_log_cooked_date` ON `cooking_log` (`cooked_date`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shopping_list ADD COLUMN previous_purchase_date INTEGER DEFAULT NULL")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `smart_defaults_cache` (
                `normalized_name` TEXT NOT NULL PRIMARY KEY,
                `category` TEXT NOT NULL,
                `subcategory` TEXT,
                `unit` TEXT,
                `location` TEXT,
                `shelf_life_days` INTEGER,
                `version` INTEGER NOT NULL DEFAULT 1,
                `fetched_at` INTEGER NOT NULL DEFAULT 0,
                `source` TEXT NOT NULL DEFAULT 'seed'
            )
        """.trimIndent())
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `items` ADD COLUMN `is_paused` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_is_paused` ON `items` (`is_paused`)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `saved_recipes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `cuisine_origin` TEXT NOT NULL DEFAULT '',
                `time_minutes` INTEGER NOT NULL DEFAULT 0,
                `difficulty` TEXT NOT NULL DEFAULT 'easy',
                `servings` INTEGER NOT NULL DEFAULT 2,
                `ingredients_json` TEXT NOT NULL DEFAULT '[]',
                `steps_json` TEXT NOT NULL DEFAULT '[]',
                `tips` TEXT,
                `personal_notes` TEXT,
                `is_favorite` INTEGER NOT NULL DEFAULT 0,
                `rating` INTEGER NOT NULL DEFAULT 0,
                `source_settings_json` TEXT,
                `is_active` INTEGER NOT NULL DEFAULT 1,
                `created_at` INTEGER NOT NULL DEFAULT 0,
                `updated_at` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `pantry_health_log` ADD COLUMN `total_items` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `pantry_health_log` ADD COLUMN `engagement_score` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `pantry_health_log` ADD COLUMN `condition_score` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `pantry_health_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `score` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `expired_count` INTEGER NOT NULL DEFAULT 0,
                `expiring_soon_count` INTEGER NOT NULL DEFAULT 0,
                `low_stock_count` INTEGER NOT NULL DEFAULT 0,
                `out_of_stock_count` INTEGER NOT NULL DEFAULT 0,
                `shopping_completion_pct` REAL NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pantry_health_log_date` ON `pantry_health_log` (`date`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add smart_min_quantity column — tracks peak quantity for automatic stock level indicator
        db.execSQL("ALTER TABLE `items` ADD COLUMN `smart_min_quantity` REAL NOT NULL DEFAULT 0.0")
        // Backfill: set smart min to current quantity for all existing items
        db.execSQL("UPDATE `items` SET `smart_min_quantity` = `quantity`")
        // Reset old auto-default minQuantity (1.0) to 0 so smart min kicks in
        db.execSQL("UPDATE `items` SET `min_quantity` = 0.0 WHERE `min_quantity` = 1.0")

        // Merge plural duplicates into singular forms (e.g., "Onions" → "Onion")
        // Step 1: Add plural's quantity + purchase history to the singular item
        // Handles "s" suffix (onions→onion) and "es" suffix (tomatoes→tomato)
        db.execSQL("""
            UPDATE items SET
                quantity = quantity + COALESCE((
                    SELECT SUM(p.quantity) FROM items p
                    WHERE p.is_active = 1 AND p.id != items.id
                    AND (LOWER(p.name) = LOWER(items.name) || 's'
                      OR LOWER(p.name) = LOWER(items.name) || 'es')
                ), 0),
                smart_min_quantity = MAX(smart_min_quantity, quantity + COALESCE((
                    SELECT SUM(p.quantity) FROM items p
                    WHERE p.is_active = 1 AND p.id != items.id
                    AND (LOWER(p.name) = LOWER(items.name) || 's'
                      OR LOWER(p.name) = LOWER(items.name) || 'es')
                ), 0)),
                updated_at = ${System.currentTimeMillis()}
            WHERE is_active = 1
            AND EXISTS (
                SELECT 1 FROM items p
                WHERE p.is_active = 1 AND p.id != items.id
                AND (LOWER(p.name) = LOWER(items.name) || 's'
                  OR LOWER(p.name) = LOWER(items.name) || 'es')
            )
        """.trimIndent())

        // Step 2: Reassign purchase_history from plural items to their singular counterpart
        db.execSQL("""
            UPDATE purchase_history SET item_id = (
                SELECT s.id FROM items s
                WHERE s.is_active = 1
                AND (LOWER((SELECT name FROM items WHERE id = purchase_history.item_id)) = LOWER(s.name) || 's'
                  OR LOWER((SELECT name FROM items WHERE id = purchase_history.item_id)) = LOWER(s.name) || 'es')
                LIMIT 1
            )
            WHERE EXISTS (
                SELECT 1 FROM items s
                WHERE s.is_active = 1
                AND (LOWER((SELECT name FROM items WHERE id = purchase_history.item_id)) = LOWER(s.name) || 's'
                  OR LOWER((SELECT name FROM items WHERE id = purchase_history.item_id)) = LOWER(s.name) || 'es')
            )
        """.trimIndent())

        // Step 3: Delete the plural items
        db.execSQL("""
            DELETE FROM items
            WHERE is_active = 1
            AND EXISTS (
                SELECT 1 FROM items s
                WHERE s.is_active = 1 AND s.id != items.id
                AND (LOWER(items.name) = LOWER(s.name) || 's'
                  OR LOWER(items.name) = LOWER(s.name) || 'es')
            )
        """.trimIndent())
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Performance indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_expiry_date` ON `items` (`expiry_date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_is_active` ON `items` (`is_active`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_history_purchase_date` ON `purchase_history` (`purchase_date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_log_usage_date` ON `usage_log` (`usage_date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_list_is_purchased` ON `shopping_list` (`is_purchased`)")

        // Unique name constraints — deduplicate first, then add index
        for (table in listOf("categories", "storage_locations", "units", "stores")) {
            db.execSQL("DELETE FROM `$table` WHERE rowid NOT IN (SELECT MIN(rowid) FROM `$table` GROUP BY `name`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_name` ON `$table` (`name`)")
        }
    }
}

@Database(
    entities = [
        ItemEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        StorageLocationEntity::class,
        UnitEntity::class,
        ItemImageEntity::class,
        PurchaseHistoryEntity::class,
        StoreEntity::class,
        UsageLogEntity::class,
        BarcodeCacheEntity::class,
        ShoppingListItemEntity::class,
        SettingsEntity::class,
        PantryHealthLogEntity::class,
        SavedRecipeEntity::class,
        SmartDefaultCacheEntity::class,
        CookingLogEntity::class,
        AppNotificationEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun storageLocationDao(): StorageLocationDao
    abstract fun unitDao(): UnitDao
    abstract fun itemImageDao(): ItemImageDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao
    abstract fun storeDao(): StoreDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun barcodeCacheDao(): BarcodeCacheDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun settingsDao(): SettingsDao
    abstract fun pantryHealthLogDao(): PantryHealthLogDao
    abstract fun savedRecipeDao(): SavedRecipeDao
    abstract fun smartDefaultCacheDao(): SmartDefaultCacheDao
    abstract fun cookingLogDao(): CookingLogDao
    abstract fun appNotificationDao(): AppNotificationDao
}
