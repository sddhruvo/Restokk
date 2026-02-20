package com.inventory.app.domain.model

import kotlin.math.exp
import kotlin.math.pow

data class HomeScoreBreakdown(
    val finalScore: Int,
    val engagementScore: Int,
    val conditionScore: Int,
    val label: String,
    val engagementFactors: List<ScoreFactor>,
    val conditionFactors: List<ScoreFactor>
)

data class ScoreFactor(
    val title: String,
    val icon: String,
    val description: String,
    val points: Int,
    val isPositive: Boolean,
    val route: String
)

object HomeScoreCalculator {

    fun compute(
        totalItems: Int,
        itemsWithCategory: Int,
        itemsWithLocation: Int,
        itemsWithExpiry: Int,
        expiredCount: Int,
        expiringSoonCount: Int,
        lowStockCount: Int,
        outOfStockCount: Int,
        shoppingActive: Int,
        shoppingPurchased: Int
    ): HomeScoreBreakdown {
        val engagement = computeEngagement(totalItems, itemsWithCategory, itemsWithLocation, itemsWithExpiry)
        val condition = computeCondition(totalItems, expiredCount, expiringSoonCount, lowStockCount, outOfStockCount, shoppingActive, shoppingPurchased)

        val finalScore = if (engagement == 0 && condition == 0) 0
        else (engagement.coerceAtLeast(0).toDouble().pow(0.4) * condition.coerceAtLeast(0).toDouble().pow(0.6)).toInt().coerceIn(0, 100)

        val label = when {
            totalItems == 0 -> "No Items"
            finalScore >= 85 -> "Excellent"
            finalScore >= 70 -> "Great"
            finalScore >= 50 -> "Good"
            finalScore >= 30 -> "Getting There"
            else -> "Just Starting"
        }

        val engagementFactors = buildEngagementFactors(totalItems, itemsWithCategory, itemsWithLocation, itemsWithExpiry)
        val conditionFactors = buildConditionFactors(totalItems, expiredCount, expiringSoonCount, lowStockCount, outOfStockCount, shoppingActive, shoppingPurchased)

        return HomeScoreBreakdown(
            finalScore = finalScore,
            engagementScore = engagement,
            conditionScore = condition,
            label = label,
            engagementFactors = engagementFactors,
            conditionFactors = conditionFactors
        )
    }

    private fun computeEngagement(
        totalItems: Int,
        itemsWithCategory: Int,
        itemsWithLocation: Int,
        itemsWithExpiry: Int
    ): Int {
        if (totalItems == 0) return 0

        val engagementRaw = (1.0 - exp(-totalItems / 15.0)) * 100.0

        val categoryPct = itemsWithCategory.toDouble() / totalItems
        val locationPct = itemsWithLocation.toDouble() / totalItems
        val expiryPct = itemsWithExpiry.toDouble() / totalItems
        val completenessBonus = ((categoryPct + locationPct + expiryPct) / 3.0) * 15.0

        return (engagementRaw + completenessBonus).toInt().coerceIn(0, 100)
    }

    private fun computeCondition(
        totalItems: Int,
        expiredCount: Int,
        expiringSoonCount: Int,
        lowStockCount: Int,
        outOfStockCount: Int,
        shoppingActive: Int,
        shoppingPurchased: Int
    ): Int {
        if (totalItems == 0) return 0

        var condition = 100.0
        condition -= ((expiredCount.toDouble() / totalItems) * 100.0).coerceAtMost(30.0)
        condition -= ((outOfStockCount.toDouble() / totalItems) * 60.0).coerceAtMost(20.0)
        condition -= ((expiringSoonCount.toDouble() / totalItems) * 50.0).coerceAtMost(15.0)
        condition -= ((lowStockCount.toDouble() / totalItems) * 30.0).coerceAtMost(10.0)

        val totalShopping = shoppingActive + shoppingPurchased
        if (totalShopping > 0) {
            val completionPct = shoppingPurchased.toFloat() / totalShopping
            if (completionPct >= 0.8f) condition += 5
            else if (completionPct < 0.3f) condition -= 5
        }

        return condition.toInt().coerceIn(0, 100)
    }

    private fun buildEngagementFactors(
        totalItems: Int,
        itemsWithCategory: Int,
        itemsWithLocation: Int,
        itemsWithExpiry: Int
    ): List<ScoreFactor> {
        val factors = mutableListOf<ScoreFactor>()

        // Items tracked
        val itemsDesc = when {
            totalItems == 0 -> "Start adding items to build your score"
            totalItems < 10 -> "Add more items to reach Good ($totalItems tracked)"
            totalItems < 20 -> "Growing nicely! ($totalItems tracked)"
            totalItems < 30 -> "Almost at Excellent ($totalItems tracked)"
            else -> "$totalItems items tracked"
        }
        val itemScore = ((1.0 - exp(-totalItems / 15.0)) * 100.0).toInt().coerceAtMost(100)
        factors.add(ScoreFactor(
            title = "Items Tracked",
            icon = "items",
            description = itemsDesc,
            points = itemScore,
            isPositive = true,
            route = "items"
        ))

        // Data quality
        if (totalItems > 0) {
            val categoryPct = (itemsWithCategory * 100.0 / totalItems).toInt()
            val locationPct = (itemsWithLocation * 100.0 / totalItems).toInt()
            val expiryPct = (itemsWithExpiry * 100.0 / totalItems).toInt()
            val avgPct = (categoryPct + locationPct + expiryPct) / 3
            val qualityDesc = when {
                avgPct >= 80 -> "Excellent data quality ($avgPct% complete)"
                avgPct >= 50 -> "Good data quality ($avgPct% complete)"
                else -> "Add categories, locations & expiry dates ($avgPct% complete)"
            }
            val qualityPoints = (avgPct * 15) / 100
            factors.add(ScoreFactor(
                title = "Data Quality",
                icon = "quality",
                description = qualityDesc,
                points = qualityPoints,
                isPositive = true,
                route = "items"
            ))
        }

        return factors
    }

    private fun buildConditionFactors(
        totalItems: Int,
        expiredCount: Int,
        expiringSoonCount: Int,
        lowStockCount: Int,
        outOfStockCount: Int,
        shoppingActive: Int,
        shoppingPurchased: Int
    ): List<ScoreFactor> {
        if (totalItems == 0) return emptyList()
        val factors = mutableListOf<ScoreFactor>()

        val expiredPenalty = ((expiredCount.toDouble() / totalItems) * 100.0).coerceAtMost(30.0).toInt()
        if (expiredPenalty > 0) {
            factors.add(ScoreFactor(
                title = "Expired Items",
                icon = "expired",
                description = "$expiredCount expired — remove or consume",
                points = expiredPenalty,
                isPositive = false,
                route = "reports/expiring"
            ))
        }

        val oosPenalty = ((outOfStockCount.toDouble() / totalItems) * 60.0).coerceAtMost(20.0).toInt()
        if (oosPenalty > 0) {
            factors.add(ScoreFactor(
                title = "Out of Stock",
                icon = "out_of_stock",
                description = "$outOfStockCount out of stock — restock or remove",
                points = oosPenalty,
                isPositive = false,
                route = "reports/low-stock"
            ))
        }

        val expiringSoonPenalty = ((expiringSoonCount.toDouble() / totalItems) * 50.0).coerceAtMost(15.0).toInt()
        if (expiringSoonPenalty > 0) {
            factors.add(ScoreFactor(
                title = "Expiring Soon",
                icon = "expiring",
                description = "$expiringSoonCount expiring soon — use them up",
                points = expiringSoonPenalty,
                isPositive = false,
                route = "reports/expiring"
            ))
        }

        val lowStockPenalty = ((lowStockCount.toDouble() / totalItems) * 30.0).coerceAtMost(10.0).toInt()
        if (lowStockPenalty > 0) {
            factors.add(ScoreFactor(
                title = "Low Stock",
                icon = "low_stock",
                description = "$lowStockCount running low — add to shopping list",
                points = lowStockPenalty,
                isPositive = false,
                route = "reports/low-stock"
            ))
        }

        val totalShopping = shoppingActive + shoppingPurchased
        if (totalShopping > 0) {
            val completionPct = shoppingPurchased.toFloat() / totalShopping
            if (completionPct < 0.3f) {
                factors.add(ScoreFactor(
                    title = "Shopping List",
                    icon = "shopping",
                    description = "$shoppingActive items left to buy",
                    points = 5,
                    isPositive = false,
                    route = "shopping"
                ))
            }
        }

        return factors.sortedByDescending { it.points }
    }
}
