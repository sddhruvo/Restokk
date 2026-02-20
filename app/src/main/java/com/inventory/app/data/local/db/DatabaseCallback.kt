package com.inventory.app.data.local.db

import android.content.Context
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

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Run synchronously — the SupportSQLiteDatabase from onCreate
        // is not safe to use from a different coroutine/thread
        seedCategories(db)
        seedLocations(db)
        seedUnits(db)
        seedSettings(db)
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
            "bi-box2" to "kitchen"
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
            arrayOf("expiry_warning_days", "7", "int", "Days before expiry to show warning", now),
            arrayOf("low_stock_threshold", "1", "float", "Default low stock threshold", now),
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
}

// JSON data classes for Gson parsing
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
