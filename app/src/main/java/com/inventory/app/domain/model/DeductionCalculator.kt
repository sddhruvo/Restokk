package com.inventory.app.domain.model

import com.inventory.app.data.local.entity.relations.ItemWithDetails

data class DeductionItem(
    val ingredientName: String,
    val amount: String,
    val unit: String,
    val parsedAmount: Double?,
    val scaledAmount: Double?,
    val matchedItemId: Long? = null,
    val matchedItemName: String? = null,
    val matchedItemUnit: String? = null,
    val isChecked: Boolean = true,
    val canDeduct: Boolean = false,
    val convertedAmount: Double? = null,
    val conversionNote: String? = null,
    val cannotDeductReason: String? = null
)

/** Pure stateless deduction calculation.
 *  No Android dependencies — fully unit-testable with mock lists. */
object DeductionCalculator {

    /**
     * For each recipe ingredient:
     * 1. Parse amount → parsedAmount
     * 2. Scale by scalingFactor → scaledAmount
     * 3. Fuzzy-match against inventory items via IngredientMatcher
     * 4. Check unit category compatibility via UnitSystem
     * 5. Convert if needed (e.g. "500 gm chicken" in inventory as "kg" → 0.5 kg)
     * 6. canDeduct = parsedAmount != null && matchedItem != null && units compatible
     */
    fun calculateDeductions(
        ingredients: List<RecipeIngredient>,
        scalingFactor: Float,
        inventoryItems: List<ItemWithDetails>
    ): List<DeductionItem> {
        return ingredients.map { ingredient ->
            val parsedAmount = parseAmountToDouble(ingredient.amount)
            val scaledAmount = parsedAmount?.let { it * scalingFactor }

            // Find best matching inventory item by name
            val matched = inventoryItems.firstOrNull { item ->
                IngredientMatcher.matches(item.item.name, ingredient.name)
            }

            if (matched == null) {
                return@map DeductionItem(
                    ingredientName = ingredient.name,
                    amount = ingredient.amount,
                    unit = ingredient.unit,
                    parsedAmount = parsedAmount,
                    scaledAmount = scaledAmount,
                    canDeduct = false,
                    cannotDeductReason = if (ingredient.amount.isBlank() || parsedAmount == null)
                        "non-numeric amount" else "not in inventory"
                )
            }

            // Get inventory unit string (abbreviation preferred, fallback to name)
            val inventoryUnit = matched.unit?.abbreviation?.takeIf { it.isNotBlank() }
                ?: matched.unit?.name?.takeIf { it.isNotBlank() }
                ?: ""

            if (parsedAmount == null) {
                return@map DeductionItem(
                    ingredientName = ingredient.name,
                    amount = ingredient.amount,
                    unit = ingredient.unit,
                    parsedAmount = null,
                    scaledAmount = null,
                    matchedItemId = matched.item.id,
                    matchedItemName = matched.item.name,
                    matchedItemUnit = inventoryUnit,
                    canDeduct = false,
                    cannotDeductReason = "non-numeric amount"
                )
            }

            val recipeCategory = UnitSystem.category(ingredient.unit)
            val inventoryCategory = UnitSystem.category(inventoryUnit)
            val recipeCanonical = UnitSystem.canonicalize(ingredient.unit)
            val inventoryCanonical = UnitSystem.canonicalize(inventoryUnit)

            // Determine conversion
            val (convertedAmount, conversionNote, canDeduct, reason) = when {
                recipeCategory != inventoryCategory -> {
                    val recipeLabel = recipeCategory.name.lowercase()
                    val invLabel = inventoryCategory.name.lowercase()
                    Quad(null, null, false, "unit type mismatch ($recipeLabel vs $invLabel)")
                }

                recipeCanonical == inventoryCanonical -> {
                    // Same canonical unit — use scaled amount directly, no conversion note needed
                    Quad(scaledAmount, null, true, null)
                }

                recipeCategory == UnitCategory.COUNT -> {
                    // COUNT with different canonical units (e.g. "sprig" vs "bunch") — no factor
                    Quad(null, null, false, "unit type mismatch (different count units)")
                }

                else -> {
                    // Same category (WEIGHT or VOLUME), different unit — convert
                    val converted = scaledAmount?.let {
                        UnitSystem.convert(it, ingredient.unit, inventoryUnit)
                    }
                    if (converted != null) {
                        val note = "${formatAmount(scaledAmount ?: 0.0)} ${recipeCanonical} → " +
                            "${formatAmount(converted)} ${inventoryCanonical}"
                        Quad(converted, note, true, null)
                    } else {
                        Quad(null, null, false, "unit type mismatch")
                    }
                }
            }

            DeductionItem(
                ingredientName = ingredient.name,
                amount = ingredient.amount,
                unit = ingredient.unit,
                parsedAmount = parsedAmount,
                scaledAmount = scaledAmount,
                matchedItemId = matched.item.id,
                matchedItemName = matched.item.name,
                matchedItemUnit = inventoryUnit,
                isChecked = canDeduct,
                canDeduct = canDeduct,
                convertedAmount = convertedAmount,
                conversionNote = conversionNote,
                cannotDeductReason = reason
            )
        }
    }

    /** Simple 4-value tuple to keep the when-block readable */
    private data class Quad(
        val convertedAmount: Double?,
        val conversionNote: String?,
        val canDeduct: Boolean,
        val reason: String?
    )
}
