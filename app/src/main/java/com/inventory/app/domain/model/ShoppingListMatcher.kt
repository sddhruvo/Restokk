package com.inventory.app.domain.model

import me.xdrop.fuzzywuzzy.FuzzySearch

data class ShoppingMatch(
    val shoppingItemId: Long,
    val shoppingItemName: String,
    val score: Double,
    val method: String
)

data class ShoppingNameInfo(
    val id: Long,
    val name: String
)

object ShoppingListMatcher {

    private val MODIFIERS = setOf(
        "organic", "whole", "fresh", "large", "small",
        "low", "fat", "free", "extra", "premium",
        "natural", "grass", "fed", "range", "unsweetened",
        "sweetened", "lite", "light", "diet", "mini",
        "jumbo", "raw", "frozen", "canned", "dried",
        "sliced", "chopped", "ground", "boneless", "skinless"
    )

    private val NUMBER_PATTERN = Regex("\\d+(\\.\\d+)?\\s*(%|oz|ml|g|kg|lb|ct|pk|pack)?")

    fun normalizeGroceryName(name: String): String {
        var normalized = name.lowercase().trim()
        // Remove numbers and units
        normalized = NUMBER_PATTERN.replace(normalized, " ")
        // Remove modifiers
        val tokens = normalized.split(Regex("\\s+")).filter { it !in MODIFIERS && it.isNotBlank() }
        return tokens.joinToString(" ").trim()
    }

    fun findMatches(
        newItemName: String,
        activeItems: List<ShoppingNameInfo>
    ): List<ShoppingMatch> {
        if (activeItems.isEmpty()) return emptyList()

        val normalizedNew = normalizeGroceryName(newItemName)
        if (normalizedNew.isBlank()) return emptyList()

        val results = mutableListOf<ShoppingMatch>()

        for (item in activeItems) {
            val normalizedShopping = normalizeGroceryName(item.name)
            if (normalizedShopping.isBlank()) continue

            // Stage 1: Exact match on normalized names
            if (normalizedNew == normalizedShopping) {
                results.add(ShoppingMatch(item.id, item.name, 1.0, "exact"))
                continue
            }

            // Stage 1b: Normalized contains (one contains the other)
            if (normalizedNew.contains(normalizedShopping) || normalizedShopping.contains(normalizedNew)) {
                results.add(ShoppingMatch(item.id, item.name, 0.85, "contains"))
                continue
            }

            // Stage 2: Jaccard token similarity
            val newTokens = normalizedNew.split(Regex("\\s+")).toSet()
            val shopTokens = normalizedShopping.split(Regex("\\s+")).toSet()
            val intersection = newTokens.intersect(shopTokens).size.toDouble()
            val union = newTokens.union(shopTokens).size.toDouble()
            val jaccard = if (union > 0) intersection / union else 0.0

            if (jaccard >= 0.4) {
                results.add(ShoppingMatch(item.id, item.name, jaccard.coerceAtMost(0.79), "jaccard"))
                continue
            }

            // Stage 3: FuzzyWuzzy partial ratio
            val fuzzyScore = FuzzySearch.partialRatio(normalizedShopping, normalizedNew)
            if (fuzzyScore >= 80) {
                val normalizedScore = fuzzyScore / 100.0
                results.add(ShoppingMatch(item.id, item.name, normalizedScore, "fuzzy"))
            }
        }

        return results.sortedByDescending { it.score }
    }
}
