package com.inventory.app.domain.model

import android.content.Context
import android.util.Log
import com.inventory.app.R
import org.json.JSONObject

enum class UnitCategory { VOLUME, WEIGHT, COUNT, ARBITRARY }

data class ResolvedUnit(
    val canonical: String,
    val category: UnitCategory,
    val toBaseMl: Double?,   // VOLUME only: ml per 1 unit
    val toBaseG: Double?,    // WEIGHT only: g per 1 unit
    val original: String
)

/** Data-driven, region-aware unit resolution. Loaded once from res/raw/unit_system.json.
 *  Call UnitSystem.initialize(context, regionCode) from AppModule or Application.onCreate(). */
object UnitSystem {

    private val TAG = "UnitSystem"

    // Maps lowercase alias → resolved data
    private var universal: Map<String, ResolvedUnit> = emptyMap()
    private var produce: Map<String, UnitCategory> = emptyMap()
    private var regional: Map<String, ResolvedUnit> = emptyMap()
    private var initialized = false

    fun initialize(context: Context, regionCode: String) {
        try {
            val json = context.resources.openRawResource(R.raw.unit_system).bufferedReader().readText()
            val root = JSONObject(json)

            // Parse universal aliases
            val universalAliases = root.getJSONObject("universal").getJSONObject("aliases")
            val universalMap = mutableMapOf<String, ResolvedUnit>()
            universalAliases.keys().forEach { key ->
                val entry = universalAliases.getJSONObject(key)
                universalMap[key.lowercase()] = parseEntry(key, entry)
            }
            universal = universalMap

            // Parse produce units
            val produceJson = root.getJSONObject("universal").getJSONObject("produce")
            val produceMap = mutableMapOf<String, UnitCategory>()
            produceJson.keys().forEach { key ->
                produceMap[key.lowercase()] = UnitCategory.COUNT
            }
            produce = produceMap

            // Parse regional overrides (matching region first, then override universal entries)
            val regionsJson = root.optJSONObject("regions")
            val regionMap = mutableMapOf<String, ResolvedUnit>()
            regionsJson?.optJSONObject(regionCode)?.let { regionEntries ->
                regionEntries.keys().forEach { key ->
                    val entry = regionEntries.getJSONObject(key)
                    regionMap[key.lowercase()] = parseEntry(key, entry)
                }
            }
            regional = regionMap

            initialized = true
            Log.d(TAG, "Initialized for region=$regionCode, universal=${universal.size}, regional=${regional.size}, produce=${produce.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize UnitSystem", e)
        }
    }

    private fun parseEntry(key: String, entry: JSONObject): ResolvedUnit {
        val canonical = entry.optString("canonical", key)
        val category = when (entry.optString("category", "ARBITRARY")) {
            "WEIGHT" -> UnitCategory.WEIGHT
            "VOLUME" -> UnitCategory.VOLUME
            "COUNT" -> UnitCategory.COUNT
            else -> UnitCategory.ARBITRARY
        }
        val toBaseMl = if (entry.has("ml")) entry.getDouble("ml") else null
        val toBaseG = if (entry.has("g")) entry.getDouble("g") else null
        return ResolvedUnit(
            canonical = canonical,
            category = category,
            toBaseMl = toBaseMl,
            toBaseG = toBaseG,
            original = key
        )
    }

    /** Full resolution: tries regional → universal → produce → fuzzy → ARBITRARY */
    fun resolve(rawUnit: String): ResolvedUnit {
        if (rawUnit.isBlank()) return arbitraryUnit(rawUnit)
        val lower = rawUnit.trim().lowercase()

        // Layer 1: Regional override first (higher priority)
        regional[lower]?.let { return it }

        // Layer 2: Universal aliases
        universal[lower]?.let { return it }

        // Layer 3: Produce units
        if (produce.containsKey(lower)) {
            return ResolvedUnit(canonical = lower, category = UnitCategory.COUNT, toBaseMl = null, toBaseG = null, original = rawUnit)
        }

        // Layer 4: Fuzzy fallback — Levenshtein distance ≤ 2
        val allKeys = regional.keys + universal.keys + produce.keys
        val fuzzyMatch = allKeys.minByOrNull { levenshtein(lower, it) }
        if (fuzzyMatch != null && levenshtein(lower, fuzzyMatch) <= 2) {
            regional[fuzzyMatch]?.let { return it.copy(original = rawUnit) }
            universal[fuzzyMatch]?.let { return it.copy(original = rawUnit) }
            if (produce.containsKey(fuzzyMatch)) {
                return ResolvedUnit(canonical = fuzzyMatch, category = UnitCategory.COUNT, toBaseMl = null, toBaseG = null, original = rawUnit)
            }
        }

        return arbitraryUnit(rawUnit)
    }

    private fun arbitraryUnit(raw: String) = ResolvedUnit(
        canonical = raw.trim(), category = UnitCategory.ARBITRARY,
        toBaseMl = null, toBaseG = null, original = raw
    )

    /** Returns canonical form: "tablespoon" → "tbsp", "gm" → "g" */
    fun canonicalize(rawUnit: String): String = resolve(rawUnit).canonical

    /** Returns the category of a unit */
    fun category(rawUnit: String): UnitCategory = resolve(rawUnit).category

    /** Convert amount to base unit within category.
     *  VOLUME → ml, WEIGHT → g. Returns null for COUNT/ARBITRARY. */
    fun convertToBase(amount: Double, rawUnit: String): Double? {
        val resolved = resolve(rawUnit)
        return when (resolved.category) {
            UnitCategory.VOLUME -> resolved.toBaseMl?.let { amount * it }
            UnitCategory.WEIGHT -> resolved.toBaseG?.let { amount * it }
            else -> null
        }
    }

    /** Convert amount between units of the same category.
     *  Returns null if cross-category or ARBITRARY. */
    fun convert(amount: Double, fromUnit: String, toUnit: String): Double? {
        val from = resolve(fromUnit)
        val to = resolve(toUnit)
        if (from.category != to.category) return null
        if (from.category == UnitCategory.ARBITRARY || from.category == UnitCategory.COUNT) return null

        val baseAmount = convertToBase(amount, fromUnit) ?: return null
        val toFactor = when (to.category) {
            UnitCategory.VOLUME -> to.toBaseMl ?: return null
            UnitCategory.WEIGHT -> to.toBaseG ?: return null
            else -> return null
        }
        if (toFactor == 0.0) return null
        return baseAmount / toFactor
    }

    /** Smart display for scaled amounts: 8 tbsp → "½ cup", 1500g → "1.5 kg", 3.0 pc → "3" */
    fun formatScaled(amount: Double, unit: String): String {
        val resolved = resolve(unit)

        // Weight: auto-upgrade g → kg at 1000g+
        if (resolved.category == UnitCategory.WEIGHT && resolved.canonical == "g" && amount >= 1000) {
            val kg = amount / 1000.0
            return "${formatAmountDisplay(kg)} kg"
        }

        // Volume: auto-upgrade tbsp → cup at 8+ tbsp (2 tbsp = 1/8 cup ≈ 30ml)
        if (resolved.category == UnitCategory.VOLUME && resolved.canonical == "tbsp" && amount >= 8) {
            val cupFactor = resolved.toBaseMl ?: 15.0
            val cupMl = resolve("cup").toBaseMl ?: 240.0
            val cups = (amount * cupFactor) / cupMl
            return "${formatAmountDisplay(cups)} cup"
        }

        // Volume: auto-upgrade ml → L at 1000ml+
        if (resolved.category == UnitCategory.VOLUME && resolved.canonical == "ml" && amount >= 1000) {
            val liters = amount / 1000.0
            return "${formatAmountDisplay(liters)} L"
        }

        // COUNT: drop decimal for whole numbers
        if (resolved.category == UnitCategory.COUNT && amount == amount.toLong().toDouble()) {
            return "${amount.toLong()} ${resolved.canonical}".trim()
        }

        return "${formatAmountDisplay(amount)} ${resolved.canonical}".trim()
    }

    private fun formatAmountDisplay(value: Double): String {
        // Try common fractions for display: 0.5 → "½", 0.25 → "¼", etc.
        val fractionMap = mapOf(0.5 to "½", 0.25 to "¼", 0.75 to "¾", 0.333 to "⅓", 0.667 to "⅔")
        val frac = value % 1.0
        if (frac > 0) {
            val whole = value.toLong()
            fractionMap.entries.minByOrNull { Math.abs(it.key - frac) }?.let { (k, v) ->
                if (Math.abs(k - frac) < 0.02) {
                    return if (whole == 0L) v else "$whole $v"
                }
            }
        }
        return if (value == value.toLong().toDouble()) value.toLong().toString()
        else "%.2f".format(value).trimEnd('0').trimEnd('.')
    }

    /** Levenshtein distance for fuzzy matching */
    private fun levenshtein(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[m][n]
    }
}
