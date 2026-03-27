package com.inventory.app.data.sync.mapper

import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.local.entity.SettingsEntity
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.local.entity.StorageLocationEntity
import com.inventory.app.data.local.entity.SubcategoryEntity
import com.inventory.app.data.local.entity.UnitEntity
import com.inventory.app.data.sync.model.SyncConstants
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Firestore ↔ Room entity mappers.
 *
 * Date conventions (matching Room's Converters.kt):
 * - LocalDate → Long (epoch day)
 * - LocalDateTime → Long (epoch millis UTC)
 * - SavedRecipeEntity already uses Long millis for timestamps
 *
 * The original Room ID is stored as [SyncConstants.FIELD_ORIGINAL_ID]
 * for FK remapping during restore.
 */

// ─── ItemEntity ──────────────────────────────────────────────────

fun ItemEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    SyncConstants.FIELD_ORIGINAL_ID to id,
    "name" to name,
    "description" to description,
    "barcode" to barcode,
    "brand" to brand,
    "categoryId" to categoryId,
    "subcategoryId" to subcategoryId,
    "storageLocationId" to storageLocationId,
    "quantity" to quantity,
    "minQuantity" to minQuantity,
    "smartMinQuantity" to smartMinQuantity,
    "maxQuantity" to maxQuantity,
    "unitId" to unitId,
    "expiryDate" to expiryDate?.toEpochDay(),
    "expiryWarningDays" to expiryWarningDays,
    "openedDate" to openedDate?.toEpochDay(),
    "daysAfterOpening" to daysAfterOpening,
    "purchaseDate" to purchaseDate?.toEpochDay(),
    "purchasePrice" to purchasePrice,
    "isFavorite" to isFavorite,
    "isPaused" to isPaused,
    "isActive" to isActive,
    "notes" to notes,
    "createdAt" to createdAt.toEpochMillis(),
    "updatedAt" to updatedAt.toEpochMillis()
)

fun Map<String, Any?>.toItemEntity(
    idOverride: Long = 0,
    categoryIdRemap: Map<Long, Long> = emptyMap(),
    subcategoryIdRemap: Map<Long, Long> = emptyMap(),
    storageLocationIdRemap: Map<Long, Long> = emptyMap(),
    unitIdRemap: Map<Long, Long> = emptyMap()
): ItemEntity {
    val origCategoryId = getLongOrNull("categoryId")
    val origSubcategoryId = getLongOrNull("subcategoryId")
    val origStorageLocationId = getLongOrNull("storageLocationId")
    val origUnitId = getLongOrNull("unitId")

    return ItemEntity(
        id = idOverride,
        name = getString("name"),
        description = getStringOrNull("description"),
        barcode = getStringOrNull("barcode"),
        brand = getStringOrNull("brand"),
        categoryId = origCategoryId?.let { categoryIdRemap[it] ?: it },
        subcategoryId = origSubcategoryId?.let { subcategoryIdRemap[it] ?: it },
        storageLocationId = origStorageLocationId?.let { storageLocationIdRemap[it] ?: it },
        quantity = getDouble("quantity"),
        minQuantity = getDouble("minQuantity"),
        smartMinQuantity = getDouble("smartMinQuantity"),
        maxQuantity = getDoubleOrNull("maxQuantity"),
        unitId = origUnitId?.let { unitIdRemap[it] ?: it },
        expiryDate = getLongOrNull("expiryDate")?.let { LocalDate.ofEpochDay(it) },
        expiryWarningDays = getInt("expiryWarningDays", 7),
        openedDate = getLongOrNull("openedDate")?.let { LocalDate.ofEpochDay(it) },
        daysAfterOpening = getIntOrNull("daysAfterOpening"),
        purchaseDate = getLongOrNull("purchaseDate")?.let { LocalDate.ofEpochDay(it) },
        purchasePrice = getDoubleOrNull("purchasePrice"),
        isFavorite = getBoolean("isFavorite"),
        isPaused = getBoolean("isPaused"),
        isActive = getBoolean("isActive", true),
        notes = getStringOrNull("notes"),
        createdAt = getLongOrNull("createdAt")?.toLocalDateTime() ?: LocalDateTime.now(),
        updatedAt = getLongOrNull("updatedAt")?.toLocalDateTime() ?: LocalDateTime.now()
    )
}

// ─── ShoppingListItemEntity ──────────────────────────────────────

fun ShoppingListItemEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    SyncConstants.FIELD_ORIGINAL_ID to id,
    "itemId" to itemId,
    "customName" to customName,
    "quantity" to quantity,
    "unitId" to unitId,
    "isPurchased" to isPurchased,
    "purchasedAt" to purchasedAt?.toEpochMillis(),
    "priority" to priority,
    "notes" to notes,
    "previousPurchaseDate" to previousPurchaseDate?.toEpochDay(),
    "createdAt" to createdAt.toEpochMillis(),
    "updatedAt" to updatedAt.toEpochMillis()
)

fun Map<String, Any?>.toShoppingListItemEntity(
    idOverride: Long = 0,
    itemIdRemap: Map<Long, Long> = emptyMap(),
    unitIdRemap: Map<Long, Long> = emptyMap()
): ShoppingListItemEntity {
    val origItemId = getLongOrNull("itemId")
    val origUnitId = getLongOrNull("unitId")

    return ShoppingListItemEntity(
        id = idOverride,
        itemId = origItemId?.let { itemIdRemap[it] ?: it },
        customName = getStringOrNull("customName"),
        quantity = getDouble("quantity", 1.0),
        unitId = origUnitId?.let { unitIdRemap[it] ?: it },
        isPurchased = getBoolean("isPurchased"),
        purchasedAt = getLongOrNull("purchasedAt")?.toLocalDateTime(),
        priority = getInt("priority"),
        notes = getStringOrNull("notes"),
        previousPurchaseDate = getLongOrNull("previousPurchaseDate")?.let { LocalDate.ofEpochDay(it) },
        createdAt = getLongOrNull("createdAt")?.toLocalDateTime() ?: LocalDateTime.now(),
        updatedAt = getLongOrNull("updatedAt")?.toLocalDateTime() ?: LocalDateTime.now()
    )
}

// ─── SavedRecipeEntity ───────────────────────────────────────────
// Note: SavedRecipeEntity uses Long (epoch millis) for createdAt/updatedAt, not LocalDateTime

fun SavedRecipeEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    SyncConstants.FIELD_ORIGINAL_ID to id,
    "name" to name,
    "description" to description,
    "cuisineOrigin" to cuisineOrigin,
    "timeMinutes" to timeMinutes,
    "difficulty" to difficulty,
    "servings" to servings,
    "ingredientsJson" to ingredientsJson,
    "stepsJson" to stepsJson,
    "tips" to tips,
    "personalNotes" to personalNotes,
    "isFavorite" to isFavorite,
    "rating" to rating,
    "sourceSettingsJson" to sourceSettingsJson,
    "isActive" to isActive,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "source" to source,
    "mealType" to mealType,
    "tags" to tags,
    "isDraft" to isDraft,
    "coverPhotoUri" to coverPhotoUri
)

fun Map<String, Any?>.toSavedRecipeEntity(idOverride: Long = 0): SavedRecipeEntity = SavedRecipeEntity(
    id = idOverride,
    name = getString("name"),
    description = getStringOrNull("description") ?: "",
    cuisineOrigin = getStringOrNull("cuisineOrigin") ?: "",
    timeMinutes = getInt("timeMinutes"),
    difficulty = getStringOrNull("difficulty") ?: "easy",
    servings = getInt("servings", 2),
    ingredientsJson = getStringOrNull("ingredientsJson") ?: "[]",
    stepsJson = getStringOrNull("stepsJson") ?: "[]",
    tips = getStringOrNull("tips"),
    personalNotes = getStringOrNull("personalNotes"),
    isFavorite = getBoolean("isFavorite"),
    rating = getInt("rating"),
    sourceSettingsJson = getStringOrNull("sourceSettingsJson"),
    isActive = getBoolean("isActive", true),
    createdAt = getLongOrNull("createdAt") ?: System.currentTimeMillis(),
    updatedAt = getLongOrNull("updatedAt") ?: System.currentTimeMillis(),
    source = getStringOrNull("source") ?: "ai",
    mealType = getStringOrNull("mealType"),
    tags = getStringOrNull("tags"),
    isDraft = getBoolean("isDraft"),
    coverPhotoUri = getStringOrNull("coverPhotoUri")
)

// ─── CategoryEntity ──────────────────────────────────────────────

fun CategoryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    SyncConstants.FIELD_ORIGINAL_ID to id,
    "name" to name,
    "description" to description,
    "icon" to icon,
    "color" to color,
    "sortOrder" to sortOrder,
    "isActive" to isActive,
    "createdAt" to createdAt.toEpochMillis(),
    "updatedAt" to updatedAt.toEpochMillis()
)

fun Map<String, Any?>.toCategoryEntity(idOverride: Long = 0): CategoryEntity = CategoryEntity(
    id = idOverride,
    name = getString("name"),
    description = getStringOrNull("description"),
    icon = getStringOrNull("icon"),
    color = getStringOrNull("color"),
    sortOrder = getInt("sortOrder"),
    isActive = getBoolean("isActive", true),
    createdAt = getLongOrNull("createdAt")?.toLocalDateTime() ?: LocalDateTime.now(),
    updatedAt = getLongOrNull("updatedAt")?.toLocalDateTime() ?: LocalDateTime.now()
)

// ─── SubcategoryEntity ───────────────────────────────────────────

fun SubcategoryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    SyncConstants.FIELD_ORIGINAL_ID to id,
    "name" to name,
    "description" to description,
    "categoryId" to categoryId,
    "sortOrder" to sortOrder,
    "isActive" to isActive,
    "createdAt" to createdAt.toEpochMillis(),
    "updatedAt" to updatedAt.toEpochMillis()
)

fun Map<String, Any?>.toSubcategoryEntity(
    idOverride: Long = 0,
    categoryIdRemap: Map<Long, Long> = emptyMap()
): SubcategoryEntity {
    val origCategoryId = getLong("categoryId")
    return SubcategoryEntity(
        id = idOverride,
        name = getString("name"),
        description = getStringOrNull("description"),
        categoryId = categoryIdRemap[origCategoryId] ?: origCategoryId,
        sortOrder = getInt("sortOrder"),
        isActive = getBoolean("isActive", true),
        createdAt = getLongOrNull("createdAt")?.toLocalDateTime() ?: LocalDateTime.now(),
        updatedAt = getLongOrNull("updatedAt")?.toLocalDateTime() ?: LocalDateTime.now()
    )
}

// ─── StorageLocationEntity ───────────────────────────────────────

fun StorageLocationEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    SyncConstants.FIELD_ORIGINAL_ID to id,
    "name" to name,
    "description" to description,
    "icon" to icon,
    "color" to color,
    "temperatureZone" to temperatureZone,
    "sortOrder" to sortOrder,
    "isActive" to isActive,
    "createdAt" to createdAt.toEpochMillis(),
    "updatedAt" to updatedAt.toEpochMillis()
)

fun Map<String, Any?>.toStorageLocationEntity(idOverride: Long = 0): StorageLocationEntity = StorageLocationEntity(
    id = idOverride,
    name = getString("name"),
    description = getStringOrNull("description"),
    icon = getStringOrNull("icon"),
    color = getStringOrNull("color"),
    temperatureZone = getStringOrNull("temperatureZone"),
    sortOrder = getInt("sortOrder"),
    isActive = getBoolean("isActive", true),
    createdAt = getLongOrNull("createdAt")?.toLocalDateTime() ?: LocalDateTime.now(),
    updatedAt = getLongOrNull("updatedAt")?.toLocalDateTime() ?: LocalDateTime.now()
)

// ─── UnitEntity ──────────────────────────────────────────────────

fun UnitEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    SyncConstants.FIELD_ORIGINAL_ID to id,
    "name" to name,
    "abbreviation" to abbreviation,
    "unitType" to unitType,
    "isActive" to isActive,
    "createdAt" to createdAt.toEpochMillis()
)

fun Map<String, Any?>.toUnitEntity(idOverride: Long = 0): UnitEntity = UnitEntity(
    id = idOverride,
    name = getString("name"),
    abbreviation = getStringOrNull("abbreviation") ?: "",
    unitType = getStringOrNull("unitType"),
    isActive = getBoolean("isActive", true),
    createdAt = getLongOrNull("createdAt")?.toLocalDateTime() ?: LocalDateTime.now()
)

// ─── SettingsEntity ──────────────────────────────────────────────

fun SettingsEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "key" to key,
    "value" to value,
    "valueType" to valueType,
    "description" to description
)

fun Map<String, Any?>.toSettingsEntity(idOverride: Long = 0): SettingsEntity = SettingsEntity(
    id = idOverride,
    key = getString("key"),
    value = getStringOrNull("value"),
    valueType = getStringOrNull("valueType") ?: "string",
    description = getStringOrNull("description")
)

// ─── Helpers ─────────────────────────────────────────────────────

private fun LocalDateTime.toEpochMillis(): Long =
    this.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDateTime()

private fun Map<String, Any?>.getString(key: String): String =
    (this[key] as? String) ?: ""

private fun Map<String, Any?>.getStringOrNull(key: String): String? =
    this[key] as? String

private fun Map<String, Any?>.getLong(key: String, default: Long = 0L): Long =
    (this[key] as? Long) ?: (this[key] as? Number)?.toLong() ?: default

private fun Map<String, Any?>.getLongOrNull(key: String): Long? =
    (this[key] as? Long) ?: (this[key] as? Number)?.toLong()

private fun Map<String, Any?>.getInt(key: String, default: Int = 0): Int =
    (this[key] as? Long)?.toInt() ?: (this[key] as? Number)?.toInt() ?: default

private fun Map<String, Any?>.getIntOrNull(key: String): Int? =
    (this[key] as? Long)?.toInt() ?: (this[key] as? Number)?.toInt()

private fun Map<String, Any?>.getDouble(key: String, default: Double = 0.0): Double =
    (this[key] as? Double) ?: (this[key] as? Number)?.toDouble() ?: default

private fun Map<String, Any?>.getDoubleOrNull(key: String): Double? =
    (this[key] as? Double) ?: (this[key] as? Number)?.toDouble()

private fun Map<String, Any?>.getBoolean(key: String, default: Boolean = false): Boolean =
    (this[key] as? Boolean) ?: default
