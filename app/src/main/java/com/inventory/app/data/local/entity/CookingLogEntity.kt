package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cooking_log",
    foreignKeys = [
        ForeignKey(
            entity = SavedRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("recipe_id"),
        Index("cooked_date")
    ]
)
data class CookingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "cooked_date") val cookedDate: Long = System.currentTimeMillis(),
    val servings: Int = 2,
    @ColumnInfo(name = "deducted_items_json") val deductedItemsJson: String? = null,
    val notes: String? = null
)
