package com.inventory.app.domain.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── Core recipe data models ────────────────────────────────────────────

data class RecipeIngredient(
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val have_it: Boolean = true
)

/** Rich step — self-contained, owns its ingredients inline (no external index references) */
data class RecipeStep(
    val instruction: String,
    val timerSeconds: Int? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val photoUri: String? = null
)

enum class RecipeSource(val value: String) {
    AI("ai"), MANUAL("manual"), CAPTURED("captured")
}

// ── Parsing utilities ─────────────────────────────────────────────────

/** Backward-compatible parser: handles both List<RecipeStep> and legacy List<String> */
fun parseStepsJson(json: String, gson: Gson): List<RecipeStep> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val type = object : TypeToken<List<RecipeStep>>() {}.type
        val result: List<RecipeStep>? = gson.fromJson(json, type)
        // If parsed objects have blank instructions it was probably List<String> mis-parsed
        if (result != null && result.isNotEmpty() && result.all { it.instruction.isBlank() }) {
            throw Exception("likely old format")
        }
        result ?: emptyList()
    } catch (e: Exception) {
        // Fallback: old format List<String> → wrap + auto-parse timers
        try {
            val strings: List<String> = gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
            strings.map { RecipeStep(instruction = it, timerSeconds = autoParseTimerFromText(it)) }
        } catch (e2: Exception) {
            emptyList()
        }
    }
}

/** Extracts timer duration from natural text: "simmer for 15 minutes" → 900 seconds */
fun autoParseTimerFromText(instruction: String): Int? {
    val regex = Regex(
        """(\d+)\s*(?:(hr|hours?)\b|(min(?:ute)?s?)\b|(sec(?:ond)?s?)\b)""",
        RegexOption.IGNORE_CASE
    )
    val match = regex.find(instruction) ?: return null
    val value = match.groupValues[1].toIntOrNull() ?: return null
    return when {
        match.groupValues[2].isNotEmpty() -> value * 3600  // hours
        match.groupValues[3].isNotEmpty() -> value * 60    // minutes
        match.groupValues[4].isNotEmpty() -> value          // seconds
        else -> null
    }
}

/** Converts List<String> (AI format) → List<RecipeStep> with auto-parsed timers */
fun convertStepsToRichFormat(steps: List<String>): List<RecipeStep> {
    return steps.map { RecipeStep(instruction = it, timerSeconds = autoParseTimerFromText(it)) }
}

// ── Amount parsing & formatting ────────────────────────────────────────

/** Parses recipe amount string to Double. Returns null for "a pinch", "to taste", etc.
 *  Handles: "2", "1.5", "1/2", "1 1/2", "1 and 1/2", "1½", Unicode fractions */
fun parseAmountToDouble(amount: String): Double? {
    if (amount.isBlank()) return null
    val trimmed = amount.trim()

    // Try direct parse: "2", "1.5"
    trimmed.toDoubleOrNull()?.let { return it }

    // Try Unicode fractions: "½", "1½", "2¾"
    val unicodeFractions = mapOf(
        '½' to 0.5, '⅓' to 0.333, '¼' to 0.25, '⅔' to 0.667,
        '¾' to 0.75, '⅛' to 0.125, '⅜' to 0.375, '⅝' to 0.625, '⅞' to 0.875
    )
    for ((char, value) in unicodeFractions) {
        if (char in trimmed) {
            val prefix = trimmed.substringBefore(char).trim()
            val whole = if (prefix.isEmpty()) 0.0 else prefix.toDoubleOrNull() ?: return null
            return whole + value
        }
    }

    // Try mixed number: "1 1/2", "1 and 1/2"
    val mixedRegex = Regex("""^(\d+)\s+(?:and\s+)?(\d+)\s*/\s*(\d+)$""")
    mixedRegex.find(trimmed)?.let {
        val whole = it.groupValues[1].toDoubleOrNull() ?: return null
        val num = it.groupValues[2].toDoubleOrNull() ?: return null
        val den = it.groupValues[3].toDoubleOrNull() ?: return null
        if (den != 0.0) return whole + (num / den)
    }

    // Try simple fraction: "1/2" → 0.5
    val fractionRegex = Regex("""^(\d+)\s*/\s*(\d+)$""")
    fractionRegex.find(trimmed)?.let {
        val num = it.groupValues[1].toDoubleOrNull() ?: return null
        val den = it.groupValues[2].toDoubleOrNull() ?: return null
        if (den != 0.0) return num / den
    }

    return null
}

/** Formats a Double amount to clean string: 500.0 → "500", 1.5 → "1.5" */
fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')
}

// ── Ingredient collection & merging ───────────────────────────────────

/** Collects all ingredients from all steps, then merges/deduplicates */
fun collectAllIngredients(steps: List<RecipeStep>): List<RecipeIngredient> {
    return mergeIngredients(steps.flatMap { it.ingredients })
}

/** Deduplicates ingredients across all steps using UnitSystem for smart merging.
 *  Three merge strategies:
 *  1. Same canonical unit: sum directly ("200g" + "300g" → "500g")
 *  2. Same category, different unit: convert to base, sum, display in first unit
 *  3. Incompatible: concatenate ("200g" + "a pinch" → "200g + a pinch") */
fun mergeIngredients(ingredients: List<RecipeIngredient>): List<RecipeIngredient> {
    return ingredients
        .groupBy { it.name.trim().lowercase() }
        .map { (_, group) ->
            if (group.size == 1) return@map group.first()

            val representative = group.first()
            val parsedAmounts = group.map { parseAmountToDouble(it.amount) to it }
            val allParseable = parsedAmounts.all { it.first != null }
            val canonicalUnits = group.map { UnitSystem.canonicalize(it.unit) }.distinct()
            val sameCanonicalUnit = canonicalUnits.size == 1
            val categories = group.map { UnitSystem.category(it.unit) }.distinct()
            val sameCategoryConvertible = categories.size == 1
                && categories.first() != UnitCategory.ARBITRARY

            when {
                // Case 1: Same canonical unit — sum directly
                allParseable && sameCanonicalUnit -> representative.copy(
                    amount = formatAmount(parsedAmounts.sumOf { it.first!! }),
                    unit = canonicalUnits.first()
                )

                // Case 2: Same category, different unit — convert to base, sum, display in first unit
                allParseable && sameCategoryConvertible -> {
                    val targetUnit = UnitSystem.canonicalize(representative.unit)
                    val totalBase = group.sumOf { ingredient ->
                        val parsed = parseAmountToDouble(ingredient.amount) ?: 0.0
                        UnitSystem.convertToBase(parsed, ingredient.unit) ?: parsed
                    }
                    val targetResolved = UnitSystem.resolve(targetUnit)
                    val factor = targetResolved.toBaseMl ?: targetResolved.toBaseG ?: 1.0
                    val inTargetUnit = if (factor > 0) totalBase / factor else totalBase
                    representative.copy(
                        amount = formatAmount(inTargetUnit),
                        unit = targetUnit
                    )
                }

                // Case 3: Incompatible — concatenate with no data loss
                else -> representative.copy(
                    amount = group.joinToString(" + ") { "${it.amount} ${it.unit}".trim() },
                    unit = ""
                )
            }
        }
}
