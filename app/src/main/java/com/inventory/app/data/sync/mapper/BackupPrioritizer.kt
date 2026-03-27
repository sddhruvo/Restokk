package com.inventory.app.data.sync.mapper

import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.SavedRecipeEntity
import java.time.LocalDate

/**
 * Selects which items/recipes to back up when the user exceeds
 * free-tier limits (75 items, 10 recipes).
 *
 * Item priority: expiry soonest → most recently updated → low stock.
 * Recipe priority: favorites first → most recently updated.
 */
object BackupPrioritizer {

    fun prioritizeItems(items: List<ItemEntity>, limit: Int): List<ItemEntity> {
        if (items.size <= limit) return items

        val today = LocalDate.now()

        return items.sortedWith(
            // 1) Items with expiry dates — soonest first (most urgent)
            // 2) Most recently updated (most current)
            // 3) Low stock items (need attention)
            compareBy<ItemEntity> { item ->
                // Items with expiry get priority (0), others get 1
                if (item.expiryDate != null) 0 else 1
            }.thenBy { item ->
                // Among items with expiry, soonest first
                item.expiryDate?.toEpochDay() ?: Long.MAX_VALUE
            }.thenByDescending { item ->
                // Most recently updated
                item.updatedAt
            }.thenBy { item ->
                // Low stock items: lower ratio = more urgent
                if (item.minQuantity > 0) {
                    item.quantity / item.minQuantity
                } else {
                    Double.MAX_VALUE
                }
            }
        ).take(limit)
    }

    fun prioritizeRecipes(recipes: List<SavedRecipeEntity>, limit: Int): List<SavedRecipeEntity> {
        if (recipes.size <= limit) return recipes

        return recipes.sortedWith(
            // 1) Favorites first
            // 2) Most recently updated
            compareByDescending<SavedRecipeEntity> { it.isFavorite }
                .thenByDescending { it.updatedAt }
        ).take(limit)
    }
}
