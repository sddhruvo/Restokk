package com.inventory.app.domain.model

/** Stateless ingredient matching — reused by CookViewModel, SavedRecipesViewModel,
 *  CookingPlaybackViewModel, RecipeBuilderViewModel */
object IngredientMatcher {

    /** Strict word-boundary matching. "rice" matches "basmati rice" but NOT "rice vinegar".
     *  All words of the shorter name must appear in the longer, and nature-changing words
     *  (vinegar, paste, sauce, etc.) in the longer but not shorter block the match. */
    fun matches(inventoryName: String, ingredientName: String): Boolean {
        val inv = inventoryName.lowercase().trim()
        val ing = ingredientName.lowercase().trim()
        if (inv == ing) return true

        val invWords = inv.split(Regex("\\s+")).filter { it.isNotBlank() }
        val ingWords = ing.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (invWords.isEmpty() || ingWords.isEmpty()) return false

        // All words of the shorter name must appear in the longer name
        val (shorter, longer) = if (invWords.size <= ingWords.size) invWords to ingWords else ingWords to invWords
        if (!shorter.all { word -> longer.any { it == word } }) return false

        // If the longer name has a nature-changing word not in the shorter name,
        // it's a fundamentally different ingredient
        val natureChangers = setOf(
            "vinegar", "paste", "sauce", "powder", "flour", "noodles", "paper",
            "extract", "essence", "wine", "stock", "broth", "cheese", "butter",
            "milk", "oil", "cream", "syrup", "juice", "water", "starch", "flakes"
        )
        val extraWords = longer.toSet() - shorter.toSet()
        if (extraWords.any { it in natureChangers }) return false

        return true
    }
}
