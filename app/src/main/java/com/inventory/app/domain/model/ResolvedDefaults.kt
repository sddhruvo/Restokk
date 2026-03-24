package com.inventory.app.domain.model

import kotlinx.coroutines.Deferred

/**
 * Unified result from the Smart Defaults 5-layer cascade.
 * Contains both resolved DB IDs and original string names
 * (strings needed for correction tracking + display).
 */
data class ResolvedDefaults(
    // Resolved IDs (ready for ItemEntity / UI dropdowns)
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val locationId: Long? = null,
    val unitId: Long? = null,

    // String names (for correction tracking + display fallback)
    val categoryName: String? = null,
    val subcategoryName: String? = null,
    val locationName: String? = null,
    val unitAbbreviation: String? = null,

    // Non-ID fields
    val shelfLifeDays: Int? = null,
    val quantity: Double? = null,
    val price: Double? = null,
    val brand: String? = null,

    // Which layer provided this result
    val source: String = "none"  // "personal" | "static" | "cache" | "remote" | "none"
)

/**
 * External hints from AI vision, barcode API, or kitchen area selection.
 * Hints slot between Layer 1 (personal history) and Layer 2 (static dict)
 * in the cascade — stronger than generic lookups, weaker than user's own history.
 */
data class DefaultHints(
    val categoryName: String? = null,
    val subcategoryName: String? = null,
    val unitAbbreviation: String? = null,
    val locationName: String? = null,
    val locationId: Long? = null,       // Kitchen scan area-based (already resolved)
    val shelfLifeDays: Int? = null,
    val quantity: Double? = null,
    val price: Double? = null,
    val brand: String? = null
)

/**
 * Result of [SmartDefaultRepository.resolve] — contains the best instant local result
 * plus an optional deferred for async remote layers (4+5).
 */
data class ResolveResult(
    val local: ResolvedDefaults,
    val remoteDeferred: Deferred<ResolvedDefaults?>? = null
)
