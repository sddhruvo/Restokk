package com.inventory.app.domain.model

import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.dao.ShoppingListDao
import com.inventory.app.data.repository.UnitRepository
import javax.inject.Inject
import javax.inject.Singleton

enum class QuantitySource { SHOPPING_LIST, PURCHASE_HISTORY, DEFAULT }

data class SmartQuantity(
    val value: Double,
    val source: QuantitySource,
    val shoppingListId: Long?,
    val unitAbbreviation: String?
)

@Singleton
class SmartQuantityResolver @Inject constructor(
    private val shoppingListDao: ShoppingListDao,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val unitRepository: UnitRepository
) {

    suspend fun resolve(
        itemId: Long?,
        itemName: String?,
        unitId: Long?
    ): SmartQuantity {
        val unitAbbr = unitId?.let { unitRepository.getById(it)?.abbreviation }

        if (itemId == null) {
            return SmartQuantity(1.0, QuantitySource.DEFAULT, null, unitAbbr)
        }

        // Layer 1: Shopping list — if this item is on the active shopping list, use that quantity
        val shoppingItem = shoppingListDao.findActiveByItemId(itemId)
        if (shoppingItem != null && shoppingItem.quantity > 0) {
            return SmartQuantity(
                value = shoppingItem.quantity,
                source = QuantitySource.SHOPPING_LIST,
                shoppingListId = shoppingItem.id,
                unitAbbreviation = unitAbbr
            )
        }

        // Layer 2: Purchase history — find mode (most frequent quantity) from last 10 purchases
        val recentQuantities = purchaseHistoryDao.getRecentPurchaseQuantities(itemId)
        if (recentQuantities.isNotEmpty()) {
            val mode = computeMode(recentQuantities)
            return SmartQuantity(
                value = mode,
                source = QuantitySource.PURCHASE_HISTORY,
                shoppingListId = null,
                unitAbbreviation = unitAbbr
            )
        }

        // Layer 3: Default
        return SmartQuantity(1.0, QuantitySource.DEFAULT, null, unitAbbr)
    }

    /**
     * Compute mode (most frequent value) from a list ordered by most recent first.
     * Tie-break: the value that appears most recently (earliest in the list) wins.
     */
    private fun computeMode(quantities: List<Double>): Double {
        if (quantities.isEmpty()) return 1.0
        if (quantities.size == 1) return quantities[0]

        // Count frequency of each value
        val freqMap = mutableMapOf<Double, Int>()
        for (q in quantities) {
            freqMap[q] = (freqMap[q] ?: 0) + 1
        }

        val maxFreq = freqMap.values.maxOrNull() ?: return quantities[0]

        // Among values with max frequency, pick the one that appears first (most recent)
        for (q in quantities) {
            if (freqMap[q] == maxFreq) return q
        }

        return quantities[0]
    }
}
