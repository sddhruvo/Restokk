package com.inventory.app.util

object ItemNameNormalizer {
    private val quantityPattern = Regex(
        "\\d+\\.?\\d*\\s*(ml|l|g|kg|oz|lb|fl\\s*oz|gal|ct|pk|pack|litre|liter)\\b",
        RegexOption.IGNORE_CASE
    )
    private val marketingWords = Regex(
        "\\b(brand|organic|premium|original|homestyle|classic|" +
        "zero|sugar-free|fat-free|low-fat|non-gmo|gluten-free)\\b",
        RegexOption.IGNORE_CASE
    )
    private val multiSpace = Regex("\\s+")

    fun normalize(raw: String): String {
        return raw.trim().lowercase()
            .replace(quantityPattern, "")
            .replace(marketingWords, "")
            .replace(multiSpace, " ")
            .trim()
    }

    /**
     * Tokenize for TF-IDF matching.
     * Strips quantities/units, lowercases, splits into word tokens.
     * Does NOT strip qualifiers or brands — TF-IDF handles those via IDF weights.
     */
    fun tokenize(raw: String): List<String> {
        return raw.trim().lowercase()
            .replace(quantityPattern, "")
            .replace(Regex("[^a-z0-9\\s-]"), "") // remove punctuation except hyphens
            .split(Regex("[\\s-]+"))
            .filter { it.length > 1 } // drop single-char tokens
            .distinct()
    }

    /** Normalize unit string for deduplication — maps aliases to canonical abbreviations. */
    fun normalizeUnit(unit: String?): String {
        if (unit.isNullOrBlank()) return ""
        val lower = unit.trim().lowercase()
        return when (lower) {
            "kilogram", "kilograms", "kilo", "kilos" -> "kg"
            "gram", "grams" -> "g"
            "litre", "litres", "liter", "liters" -> "l"
            "millilitre", "millilitres", "milliliter", "milliliters", "ml" -> "ml"
            "piece", "pieces", "pcs", "pc", "each", "ea" -> "pcs"
            "pack", "packs", "pk" -> "pack"
            "bottle", "bottles", "btl" -> "bottle"
            "can", "cans" -> "can"
            "box", "boxes" -> "box"
            "bag", "bags" -> "bag"
            "dozen", "doz" -> "dozen"
            "pound", "pounds", "lb", "lbs" -> "lb"
            "ounce", "ounces", "oz" -> "oz"
            else -> lower
        }
    }

    /** Normalize for exact matching (Layer 2) — sorted tokens for order-independent comparison. */
    fun normalizeForExactMatch(raw: String): String {
        return tokenize(raw).map { depluralize(it) }.sorted().joinToString(" ")
    }

    /**
     * Simple plural/singular normalization for kitchen inventory items.
     * Conservative: only handles clear English plural patterns.
     * Does NOT strip "es" generically — too many food words end in "e" (cheese, sauce, juice, rice).
     */
    fun depluralize(token: String): String {
        return when {
            // -ies → -y: berries → berry, cherries → cherry
            token.endsWith("ies") && token.length > 4 -> token.dropLast(3) + "y"
            // -ves → -f/-fe: loaves → loaf, knives → knife (approximate)
            token.endsWith("ves") && token.length > 4 -> token.dropLast(3) + "f"
            // -oes → -o: tomatoes → tomato, potatoes → potato
            token.endsWith("oes") && token.length > 4 -> token.dropLast(2)
            // -shes → -sh: dishes → dish
            token.endsWith("shes") && token.length > 5 -> token.dropLast(2)
            // -ches → -ch: peaches → peach
            token.endsWith("ches") && token.length > 5 -> token.dropLast(2)
            // -xes → -x: boxes → box
            token.endsWith("xes") && token.length > 4 -> token.dropLast(2)
            // Generic -s (but NOT -ss, -us, -is): eggs → egg, milks → milk
            // Avoids: cheese, sauce, juice, rice, lettuce (none end in plain "s")
            token.endsWith("s") && !token.endsWith("ss") && !token.endsWith("us")
                && !token.endsWith("is") && token.length > 2 -> token.dropLast(1)
            else -> token
        }
    }
}
