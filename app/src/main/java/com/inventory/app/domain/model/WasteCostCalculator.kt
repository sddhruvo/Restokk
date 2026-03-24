package com.inventory.app.domain.model

import com.inventory.app.data.local.dao.WasteLogWithCost

data class WasteSummary(
    val totalCost: Double,
    val totalItemCount: Int,
    val totalQuantity: Double,
    val itemsWithPrice: Int,
    val itemsWithoutPrice: Int,
    val topWastedItems: List<WasteItemCost>,
    val hasPartialData: Boolean
)

data class WasteItemCost(
    val itemName: String,
    val quantity: Double,
    val cost: Double?
)

object WasteCostCalculator {

    fun calculate(wasteLogs: List<WasteLogWithCost>): WasteSummary {
        if (wasteLogs.isEmpty()) {
            return WasteSummary(
                totalCost = 0.0,
                totalItemCount = 0,
                totalQuantity = 0.0,
                itemsWithPrice = 0,
                itemsWithoutPrice = 0,
                topWastedItems = emptyList(),
                hasPartialData = false
            )
        }

        var totalCost = 0.0
        var itemsWithPrice = 0
        var itemsWithoutPrice = 0
        val itemCosts = mutableListOf<WasteItemCost>()

        for (log in wasteLogs) {
            // Fallback chain: purchase_history unit_price → item.purchase_price → null
            val unitPrice = log.latestUnitPrice
            val cost: Double? = when {
                unitPrice != null -> {
                    // Best case: per-unit price × wasted quantity
                    log.quantity * unitPrice
                }
                log.itemPurchasePrice != null && log.itemPurchasePrice > 0 -> {
                    // Fallback: item's total purchase_price as rough estimate
                    log.itemPurchasePrice
                }
                else -> null
            }

            if (cost != null) {
                totalCost += cost
                itemsWithPrice++
            } else {
                itemsWithoutPrice++
            }
            itemCosts.add(WasteItemCost(log.itemName, log.quantity, cost))
        }

        // Aggregate by item name for top-wasted list
        val aggregated = itemCosts
            .groupBy { it.itemName }
            .map { (name, entries) ->
                WasteItemCost(
                    itemName = name,
                    quantity = entries.sumOf { it.quantity },
                    cost = entries.mapNotNull { it.cost }.takeIf { it.isNotEmpty() }?.sum()
                )
            }
            .sortedByDescending { it.cost ?: 0.0 }
            .take(5)

        return WasteSummary(
            totalCost = totalCost,
            totalItemCount = wasteLogs.map { it.itemId }.distinct().size,
            totalQuantity = wasteLogs.sumOf { it.quantity },
            itemsWithPrice = itemsWithPrice,
            itemsWithoutPrice = itemsWithoutPrice,
            topWastedItems = aggregated,
            hasPartialData = itemsWithoutPrice > 0
        )
    }
}
