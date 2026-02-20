package com.inventory.app.domain.tips.providers

import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.domain.model.HomeScoreCalculator
import com.inventory.app.domain.tips.Tip
import com.inventory.app.domain.tips.TipCategory
import com.inventory.app.domain.tips.TipProvider
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class PantryHealthTipProvider @Inject constructor(
    private val itemRepository: ItemRepository,
    private val shoppingListRepository: ShoppingListRepository
) : TipProvider {

    override val category = TipCategory.PANTRY_HEALTH

    override suspend fun generateTips(): List<Tip> {
        val tips = mutableListOf<Tip>()

        val totalItems = itemRepository.getTotalItemCount().firstOrNull() ?: 0
        val withCategory = itemRepository.getItemsWithCategoryCount().firstOrNull() ?: 0
        val withLocation = itemRepository.getItemsWithLocationCount().firstOrNull() ?: 0
        val withExpiry = itemRepository.getItemsWithExpiryCount().firstOrNull() ?: 0
        val expiredCount = itemRepository.getExpiredCount().firstOrNull() ?: 0
        val expiringSoonCount = itemRepository.getExpiringSoonCount(7).firstOrNull() ?: 0
        val lowStockCount = itemRepository.getLowStockCount().firstOrNull() ?: 0
        val outOfStockCount = itemRepository.getOutOfStockCount().firstOrNull() ?: 0
        val shoppingActive = shoppingListRepository.getActiveCount().firstOrNull() ?: 0
        val shoppingPurchased = shoppingListRepository.getPurchasedCount().firstOrNull() ?: 0

        val breakdown = HomeScoreCalculator.compute(
            totalItems = totalItems,
            itemsWithCategory = withCategory,
            itemsWithLocation = withLocation,
            itemsWithExpiry = withExpiry,
            expiredCount = expiredCount,
            expiringSoonCount = expiringSoonCount,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            shoppingActive = shoppingActive,
            shoppingPurchased = shoppingPurchased
        )

        // Condition tips (highest priority — fix issues)
        if (expiredCount > 0) {
            tips.add(Tip(
                id = "health_expired",
                message = "Remove $expiredCount expired item${if (expiredCount > 1) "s" else ""} to improve your condition score",
                category = TipCategory.PANTRY_HEALTH,
                priority = 100,
                actionLabel = "View Expired",
                actionRoute = "reports/expiring"
            ))
        }

        if (outOfStockCount > 0) {
            tips.add(Tip(
                id = "health_out_of_stock",
                message = "Restock $outOfStockCount out-of-stock item${if (outOfStockCount > 1) "s" else ""} — add to shopping list",
                category = TipCategory.PANTRY_HEALTH,
                priority = 90,
                actionLabel = "View Low Stock",
                actionRoute = "reports/low-stock"
            ))
        }

        if (expiringSoonCount > 0) {
            tips.add(Tip(
                id = "health_expiring_soon",
                message = "Plan meals around $expiringSoonCount item${if (expiringSoonCount > 1) "s" else ""} expiring soon",
                category = TipCategory.PANTRY_HEALTH,
                priority = 70,
                actionLabel = "View Expiring",
                actionRoute = "reports/expiring"
            ))
        }

        if (lowStockCount > 0) {
            tips.add(Tip(
                id = "health_low_stock",
                message = "Add $lowStockCount low stock item${if (lowStockCount > 1) "s" else ""} to your shopping list",
                category = TipCategory.PANTRY_HEALTH,
                priority = 60,
                actionLabel = "Shopping List",
                actionRoute = "shopping"
            ))
        }

        // Engagement tips (encourage growth)
        if (totalItems < 10) {
            val needed = 10 - totalItems
            tips.add(Tip(
                id = "health_add_items",
                message = "Add $needed more item${if (needed > 1) "s" else ""} to reach a Good score",
                category = TipCategory.PANTRY_HEALTH,
                priority = 55,
                actionLabel = "Add Item",
                actionRoute = "items/form"
            ))
        } else if (totalItems < 20) {
            val needed = 20 - totalItems
            tips.add(Tip(
                id = "health_add_items",
                message = "Add $needed more item${if (needed > 1) "s" else ""} to reach Great",
                category = TipCategory.PANTRY_HEALTH,
                priority = 45,
                actionLabel = "Add Item",
                actionRoute = "items/form"
            ))
        }

        if (totalItems > 0) {
            val uncategorized = totalItems - withCategory
            if (uncategorized > 0) {
                tips.add(Tip(
                    id = "health_set_categories",
                    message = "Set categories on $uncategorized item${if (uncategorized > 1) "s" else ""} to boost data quality",
                    category = TipCategory.PANTRY_HEALTH,
                    priority = 40,
                    actionLabel = "View Items",
                    actionRoute = "items"
                ))
            }

            val noExpiry = totalItems - withExpiry
            if (noExpiry > 0 && noExpiry > totalItems / 3) {
                tips.add(Tip(
                    id = "health_set_expiry",
                    message = "Add expiry dates to $noExpiry item${if (noExpiry > 1) "s" else ""} for better tracking",
                    category = TipCategory.PANTRY_HEALTH,
                    priority = 35,
                    actionLabel = "View Items",
                    actionRoute = "items"
                ))
            }
        }

        val totalShopping = shoppingActive + shoppingPurchased
        if (totalShopping > 0) {
            val completionPct = shoppingPurchased.toFloat() / totalShopping
            if (completionPct < 0.3f && shoppingActive > 0) {
                tips.add(Tip(
                    id = "health_shopping_incomplete",
                    message = "Complete your shopping list to earn a condition bonus",
                    category = TipCategory.PANTRY_HEALTH,
                    priority = 50,
                    actionLabel = "Shopping List",
                    actionRoute = "shopping"
                ))
            }
        }

        if (tips.isEmpty()) {
            tips.add(Tip(
                id = "health_all_good",
                message = "Your Home Score is ${breakdown.finalScore} — ${breakdown.label}! Keep it up.",
                category = TipCategory.PANTRY_HEALTH,
                priority = 10
            ))
        }

        return tips
    }
}
