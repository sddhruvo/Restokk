package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    @ColumnInfo(name = "cuisine_origin") val cuisineOrigin: String = "",
    @ColumnInfo(name = "time_minutes") val timeMinutes: Int = 0,
    val difficulty: String = "easy",
    val servings: Int = 2,
    @ColumnInfo(name = "ingredients_json") val ingredientsJson: String = "[]",
    @ColumnInfo(name = "steps_json") val stepsJson: String = "[]",
    val tips: String? = null,
    @ColumnInfo(name = "personal_notes") val personalNotes: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    val rating: Int = 0,
    @ColumnInfo(name = "source_settings_json") val sourceSettingsJson: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
