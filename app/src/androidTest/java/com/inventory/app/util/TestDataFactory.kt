package com.inventory.app.util

import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Factory methods for creating test entities with sensible defaults.
 */
object TestDataFactory {

    fun item(
        id: Long = 0,
        name: String = "Test Item",
        quantity: Double = 5.0,
        categoryId: Long? = 1L,
        storageLocationId: Long? = 1L,
        unitId: Long? = 1L,
        expiryDate: LocalDate? = LocalDate.now().plusDays(14),
        purchasePrice: Double? = 2.99,
        minQuantity: Double = 0.0,
        isFavorite: Boolean = false,
        isPaused: Boolean = false,
        isActive: Boolean = true
    ) = ItemEntity(
        id = id,
        name = name,
        quantity = quantity,
        categoryId = categoryId,
        storageLocationId = storageLocationId,
        unitId = unitId,
        expiryDate = expiryDate,
        purchasePrice = purchasePrice,
        minQuantity = minQuantity,
        isFavorite = isFavorite,
        isPaused = isPaused,
        isActive = isActive
    )

    fun expiringSoonItem(
        name: String = "Expiring Milk",
        daysUntilExpiry: Int = 2
    ) = item(
        name = name,
        expiryDate = LocalDate.now().plusDays(daysUntilExpiry.toLong()),
        categoryId = 1L
    )

    fun expiredItem(
        name: String = "Expired Yogurt",
        daysAgo: Int = 3
    ) = item(
        name = name,
        expiryDate = LocalDate.now().minusDays(daysAgo.toLong()),
        categoryId = 1L
    )

    fun lowStockItem(
        name: String = "Low Stock Salt",
        quantity: Double = 0.5,
        minQuantity: Double = 2.0
    ) = item(
        name = name,
        quantity = quantity,
        minQuantity = minQuantity
    )

    fun outOfStockItem(
        name: String = "Out of Stock Sugar"
    ) = item(
        name = name,
        quantity = 0.0,
        minQuantity = 1.0
    )

    fun shoppingItem(
        id: Long = 0,
        customName: String = "Eggs",
        quantity: Double = 1.0,
        isPurchased: Boolean = false,
        priority: Int = 0,
        notes: String? = null
    ) = ShoppingListItemEntity(
        id = id,
        customName = customName,
        quantity = quantity,
        isPurchased = isPurchased,
        priority = priority,
        notes = notes,
        purchasedAt = if (isPurchased) LocalDateTime.now() else null
    )
}
