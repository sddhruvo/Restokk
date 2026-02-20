package com.inventory.app.util

import com.inventory.app.domain.model.SmartDefaults

/**
 * Matches item names to category names using SmartDefaults lookup.
 * Used to auto-assign categories to custom-name shopping list items
 * so they get proper color coding and grouping.
 */
object CategoryMatcher {

    /**
     * Returns the category name for a given item name, or null if no match found.
     * Uses SmartDefaults' fuzzy matching (exact → contains → word-level).
     */
    fun matchCategory(itemName: String): String? {
        return SmartDefaults.lookup(itemName)?.category
    }
}
