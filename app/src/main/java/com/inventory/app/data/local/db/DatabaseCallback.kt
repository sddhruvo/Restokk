package com.inventory.app.data.local.db

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inventory.app.R
import com.inventory.app.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class DatabaseCallback(
    private val context: Context,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    companion object {
        private const val TAG = "DatabaseCallback"
        private const val PREFS_NAME = "db_seed_prefs"
        private const val KEY_DEFAULTS_VERSION = "defaults_seeded_version"
        // Increment this when adding new default categories/locations
        private const val CURRENT_DEFAULTS_VERSION = 2
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Run synchronously — the SupportSQLiteDatabase from onCreate
        // is not safe to use from a different coroutine/thread
        seedCategories(db)
        seedLocations(db)
        seedUnits(db)
        seedSettings(db)
        seedSmartDefaultsCache(db)
        // Mark current defaults version so onOpen doesn't re-run
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DEFAULTS_VERSION, CURRENT_DEFAULTS_VERSION).apply()
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seededVersion = prefs.getInt(KEY_DEFAULTS_VERSION, 1)
        if (seededVersion < CURRENT_DEFAULTS_VERSION) {
            scope.launch(Dispatchers.IO) {
                try {
                    ensureMissingDefaults(db)
                    prefs.edit().putInt(KEY_DEFAULTS_VERSION, CURRENT_DEFAULTS_VERSION).apply()
                    Log.d(TAG, "Seeded missing default categories/locations (v$CURRENT_DEFAULTS_VERSION)")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to seed missing defaults: ${e.message}")
                }
            }
        }
    }

    private fun ensureMissingDefaults(db: SupportSQLiteDatabase) {
        // Seed missing categories
        val catJson = context.resources.openRawResource(R.raw.categories)
            .bufferedReader().use { it.readText() }
        val catData = Gson().fromJson<CategoriesData>(
            catJson, object : TypeToken<CategoriesData>() {}.type
        )
        val iconMap = mapOf(
            "bi-egg" to "egg", "bi-shop" to "store", "bi-water" to "water",
            "bi-apple" to "nutrition", "bi-flower1" to "grass", "bi-basket" to "shopping_basket",
            "bi-archive" to "archive", "bi-box-seam" to "inventory", "bi-droplet" to "water_drop",
            "bi-asterisk" to "star", "bi-bag" to "shopping_bag", "bi-cup-straw" to "local_cafe",
            "bi-snow" to "ac_unit", "bi-cake" to "cake", "bi-droplet-fill" to "water_drop",
            "bi-globe" to "language", "bi-person-heart" to "child_care", "bi-heart" to "favorite",
            "bi-house" to "cleaning_services", "bi-person" to "face",
            "bi-plus-circle" to "medical_services", "bi-receipt" to "receipt",
            "bi-three-dots" to "more_horiz"
        )
        val maxSortOrder = db.query("SELECT COALESCE(MAX(sort_order), -1) FROM categories")
            .use { if (it.moveToFirst()) it.getInt(0) else -1 }

        var sortOffset = maxSortOrder + 1
        catData.categories.forEach { cat ->
            val exists = db.query("SELECT COUNT(*) FROM categories WHERE name = ?", arrayOf(cat.name))
                .use { it.moveToFirst(); it.getInt(0) > 0 }
            if (!exists) {
                val mappedIcon = iconMap[cat.icon] ?: "category"
                val now = now()
                db.execSQL(
                    """INSERT INTO categories (name, description, icon, color, sort_order, is_active, created_at, updated_at)
                       VALUES (?, ?, ?, ?, ?, 1, ?, ?)""",
                    arrayOf(cat.name, cat.description, mappedIcon, cat.color, sortOffset++, now, now)
                )
                val cursor = db.query("SELECT last_insert_rowid()")
                cursor.moveToFirst()
                val categoryId = cursor.getLong(0)
                cursor.close()
                cat.subcategories.forEachIndexed { subIndex, subName ->
                    db.execSQL(
                        """INSERT INTO subcategories (name, category_id, sort_order, is_active, created_at, updated_at)
                           VALUES (?, ?, ?, 1, ?, ?)""",
                        arrayOf(subName, categoryId, subIndex, now, now)
                    )
                }
                Log.d(TAG, "Seeded missing category: ${cat.name}")
            }
        }

        // Seed missing locations
        val locJson = context.resources.openRawResource(R.raw.locations)
            .bufferedReader().use { it.readText() }
        val locData = Gson().fromJson<LocationsData>(
            locJson, object : TypeToken<LocationsData>() {}.type
        )
        val locIconMap = mapOf(
            "bi-thermometer-snow" to "kitchen", "bi-snow2" to "ac_unit",
            "bi-door-open" to "door_front", "bi-box" to "inbox",
            "bi-grid-3x3-gap" to "grid_view", "bi-columns-gap" to "countertops",
            "bi-house-down" to "warehouse", "bi-house-door" to "garage",
            "bi-cup" to "wine_bar", "bi-box2" to "kitchen",
            "bi-droplet-half" to "water_drop", "bi-arrow-repeat" to "autorenew"
        )
        val maxLocSort = db.query("SELECT COALESCE(MAX(sort_order), -1) FROM storage_locations")
            .use { if (it.moveToFirst()) it.getInt(0) else -1 }

        var locSortOffset = maxLocSort + 1
        locData.locations.forEach { loc ->
            val exists = db.query("SELECT COUNT(*) FROM storage_locations WHERE name = ?", arrayOf(loc.name))
                .use { it.moveToFirst(); it.getInt(0) > 0 }
            if (!exists) {
                val mappedIcon = locIconMap[loc.icon] ?: "place"
                val now = now()
                db.execSQL(
                    """INSERT INTO storage_locations (name, description, icon, color, temperature_zone, sort_order, is_active, created_at, updated_at)
                       VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)""",
                    arrayOf(loc.name, loc.description, mappedIcon, loc.color, loc.temperature_zone, locSortOffset++, now, now)
                )
                Log.d(TAG, "Seeded missing location: ${loc.name}")
            }
        }
    }

    private fun now(): Long {
        return LocalDateTime.now()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun seedCategories(db: SupportSQLiteDatabase) {
        val json = context.resources.openRawResource(R.raw.categories)
            .bufferedReader().use { it.readText() }
        val data = Gson().fromJson<CategoriesData>(
            json, object : TypeToken<CategoriesData>() {}.type
        )

        val iconMap = mapOf(
            "bi-egg" to "egg",
            "bi-shop" to "store",
            "bi-water" to "water",
            "bi-apple" to "nutrition",
            "bi-flower1" to "grass",
            "bi-basket" to "shopping_basket",
            "bi-archive" to "archive",
            "bi-box-seam" to "inventory",
            "bi-droplet" to "water_drop",
            "bi-asterisk" to "star",
            "bi-bag" to "shopping_bag",
            "bi-cup-straw" to "local_cafe",
            "bi-snow" to "ac_unit",
            "bi-cake" to "cake",
            "bi-droplet-fill" to "water_drop",
            "bi-globe" to "language",
            "bi-person-heart" to "child_care",
            "bi-heart" to "favorite",
            "bi-house" to "cleaning_services",
            "bi-person" to "face",
            "bi-plus-circle" to "medical_services",
            "bi-receipt" to "receipt",
            "bi-three-dots" to "more_horiz"
        )

        data.categories.forEachIndexed { index, cat ->
            val mappedIcon = iconMap[cat.icon] ?: "category"
            val now = now()
            db.execSQL(
                """INSERT INTO categories (name, description, icon, color, sort_order, is_active, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, 1, ?, ?)""",
                arrayOf(cat.name, cat.description, mappedIcon, cat.color, index, now, now)
            )

            // Get the category id we just inserted
            val cursor = db.query("SELECT last_insert_rowid()")
            cursor.moveToFirst()
            val categoryId = cursor.getLong(0)
            cursor.close()

            cat.subcategories.forEachIndexed { subIndex, subName ->
                db.execSQL(
                    """INSERT INTO subcategories (name, category_id, sort_order, is_active, created_at, updated_at)
                       VALUES (?, ?, ?, 1, ?, ?)""",
                    arrayOf(subName, categoryId, subIndex, now, now)
                )
            }
        }
    }

    private fun seedLocations(db: SupportSQLiteDatabase) {
        val json = context.resources.openRawResource(R.raw.locations)
            .bufferedReader().use { it.readText() }
        val data = Gson().fromJson<LocationsData>(
            json, object : TypeToken<LocationsData>() {}.type
        )

        val iconMap = mapOf(
            "bi-thermometer-snow" to "kitchen",
            "bi-snow2" to "ac_unit",
            "bi-door-open" to "door_front",
            "bi-box" to "inbox",
            "bi-grid-3x3-gap" to "grid_view",
            "bi-columns-gap" to "countertops",
            "bi-house-down" to "warehouse",
            "bi-house-door" to "garage",
            "bi-cup" to "wine_bar",
            "bi-box2" to "kitchen",
            "bi-droplet-half" to "water_drop",
            "bi-arrow-repeat" to "autorenew"
        )

        data.locations.forEachIndexed { index, loc ->
            val mappedIcon = iconMap[loc.icon] ?: "place"
            val now = now()
            db.execSQL(
                """INSERT INTO storage_locations (name, description, icon, color, temperature_zone, sort_order, is_active, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)""",
                arrayOf(loc.name, loc.description, mappedIcon, loc.color, loc.temperature_zone, index, now, now)
            )
        }
    }

    private fun seedUnits(db: SupportSQLiteDatabase) {
        val json = context.resources.openRawResource(R.raw.units)
            .bufferedReader().use { it.readText() }
        val data = Gson().fromJson<UnitsData>(
            json, object : TypeToken<UnitsData>() {}.type
        )

        val now = now()
        data.units.forEach { unit ->
            db.execSQL(
                """INSERT INTO units (name, abbreviation, unit_type, is_active, created_at)
                   VALUES (?, ?, ?, 1, ?)""",
                arrayOf(unit.name, unit.abbreviation, unit.unit_type, now)
            )
        }
    }

    private fun seedSettings(db: SupportSQLiteDatabase) {
        val now = now()
        val defaults = listOf(
            arrayOf("expiry_warning_days", "3", "int", "Days before expiry to show warning", now),
            arrayOf("low_stock_threshold", "25", "float", "Default low stock threshold", now),
            arrayOf("items_per_page", "20", "int", "Number of items per page", now),
            arrayOf("default_view", "grid", "string", "Default view mode (grid/list)", now),
            arrayOf("currency_symbol", FormatUtils.getDefaultCurrencySymbol(), "string", "Currency symbol for prices", now),
            arrayOf("date_format", "yyyy-MM-dd", "string", "Date display format", now)
        )

        defaults.forEach { setting ->
            db.execSQL(
                """INSERT INTO settings (key, value, value_type, description, updated_at)
                   VALUES (?, ?, ?, ?, ?)""",
                setting
            )
        }
    }
    private fun seedSmartDefaultsCache(db: SupportSQLiteDatabase) {
        val json = context.resources.openRawResource(R.raw.smart_defaults_seed)
            .bufferedReader().use { it.readText() }
        val items = Gson().fromJson<List<SmartDefaultSeed>>(
            json, object : TypeToken<List<SmartDefaultSeed>>() {}.type
        )

        val now = System.currentTimeMillis()
        items.forEach { item ->
            db.execSQL(
                """INSERT OR IGNORE INTO smart_defaults_cache
                   (normalized_name, category, subcategory, unit, location, shelf_life_days, version, fetched_at, source)
                   VALUES (?, ?, ?, ?, ?, ?, 1, ?, 'seed')""",
                arrayOf(item.name, item.category, item.subcategory, item.unit, item.location, item.shelfLifeDays?.toLong(), now)
            )
        }
    }
}

// JSON data classes for Gson parsing
data class SmartDefaultSeed(
    val name: String,
    val category: String,
    val subcategory: String? = null,
    val unit: String? = null,
    val location: String? = null,
    val shelfLifeDays: Number? = null
)

data class CategoriesData(val categories: List<CategorySeed>)
data class CategorySeed(
    val name: String,
    val description: String,
    val icon: String,
    val color: String,
    val subcategories: List<String>
)

data class LocationsData(val locations: List<LocationSeed>)
data class LocationSeed(
    val name: String,
    val description: String,
    val icon: String,
    val color: String,
    val temperature_zone: String
)

data class UnitsData(val units: List<UnitSeed>)
data class UnitSeed(
    val name: String,
    val abbreviation: String,
    val unit_type: String
)
