package com.inventory.app.domain.model

import com.inventory.app.util.ItemNameNormalizer

/**
 * Links recipe ingredients to their relevant steps using text matching.
 * Three-pass algorithm: exact phrase → all-tokens → core-word with nature-changer guard.
 * Unmatched ingredients fall back to step 0 (prep step).
 */
object StepIngredientLinker {

    private val natureChangers = setOf(
        "vinegar", "paste", "sauce", "powder", "flour", "noodle", "paper",
        "extract", "essence", "wine", "stock", "broth", "cheese", "butter",
        "milk", "oil", "cream", "syrup", "juice", "water", "starch", "flake"
    )

    private val stopWords = setOf(
        "the", "a", "an", "some", "of", "to", "in", "and", "or", "with", "for",
        "into", "onto", "over", "from", "about", "your", "until", "then",
        "fresh", "dried", "frozen", "canned", "chopped", "diced", "sliced",
        "minced", "grated", "crushed", "ground", "whole", "large", "small",
        "medium", "thin", "thick", "finely", "roughly", "coarsely"
    )

    fun linkIngredientsToSteps(
        ingredients: List<RecipeIngredient>,
        steps: List<RecipeStep>
    ): List<RecipeStep> {
        if (ingredients.isEmpty() || steps.isEmpty()) return steps

        val stepTokenSets = steps.map { tokenize(it.instruction) }
        val stepTokenLists = steps.map { tokenizeOrdered(it.instruction) }
        val stepIngredientsMap = mutableMapOf<Int, MutableList<RecipeIngredient>>()

        for (ingredient in ingredients) {
            val ingTokens = tokenize(ingredient.name)
            val ingTokenList = tokenizeOrdered(ingredient.name)
            if (ingTokens.isEmpty()) {
                // Can't match — assign to step 0
                stepIngredientsMap.getOrPut(0) { mutableListOf() }.add(ingredient)
                continue
            }

            val matchedIndices = mutableSetOf<Int>()

            for ((index, stepTokenSet) in stepTokenSets.withIndex()) {
                if (matchesStep(ingTokens, ingTokenList, stepTokenSet, stepTokenLists[index])) {
                    matchedIndices.add(index)
                }
            }

            // Safety net: unmatched → step 0
            if (matchedIndices.isEmpty()) {
                matchedIndices.add(0)
            }

            for (index in matchedIndices) {
                stepIngredientsMap.getOrPut(index) { mutableListOf() }.add(ingredient)
            }
        }

        return steps.mapIndexed { i, step ->
            val linked = stepIngredientsMap[i]
            if (linked != null) step.copy(ingredients = linked) else step
        }
    }

    private fun matchesStep(
        ingTokens: Set<String>,
        ingTokenList: List<String>,
        stepTokenSet: Set<String>,
        stepTokenList: List<String>
    ): Boolean {
        // Pass 1: exact phrase match (contiguous sequence)
        if (ingTokenList.size > 1 && containsPhrase(stepTokenList, ingTokenList)) return true

        // Pass 2: all tokens present (any order)
        if (ingTokens.size > 1 && ingTokens.all { it in stepTokenSet }) return true

        // Pass 3: core-word match with nature-changer guard
        val hasNatureChanger = ingTokens.size > 1 && ingTokens.any { it in natureChangers }
        val matchCount = ingTokens.count { it in stepTokenSet }

        if (hasNatureChanger) {
            // Require at least 2 tokens to prevent false positives
            return matchCount >= 2
        }

        // No nature-changer: any significant token (>2 chars) is enough
        return ingTokens.any { it.length > 2 && it in stepTokenSet }
    }

    /** Checks if [phrase] appears as a contiguous subsequence in [tokens]. */
    private fun containsPhrase(tokens: List<String>, phrase: List<String>): Boolean {
        if (phrase.size > tokens.size) return false
        val limit = tokens.size - phrase.size
        outer@ for (start in 0..limit) {
            for (j in phrase.indices) {
                if (tokens[start + j] != phrase[j]) continue@outer
            }
            return true
        }
        return false
    }

    /** Tokenize for matching — returns a SET for fast membership checks. */
    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in stopWords }
            .map { ItemNameNormalizer.depluralize(it) }
            .toSet()
    }

    /** Tokenize preserving order — for phrase matching. */
    private fun tokenizeOrdered(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in stopWords }
            .map { ItemNameNormalizer.depluralize(it) }
    }
}
