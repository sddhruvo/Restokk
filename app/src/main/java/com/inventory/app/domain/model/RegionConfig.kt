package com.inventory.app.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * Measurement system used by a region.
 */
enum class MeasurementSystem { METRIC, IMPERIAL }

/**
 * Immutable region configuration — single source of truth for country-specific data.
 * All region-dependent logic (currency, units, date format) derives from this.
 */
data class RegionConfig(
    val countryCode: String,
    val countryName: String,
    val flag: String,
    val defaultCurrencySymbol: String,
    val measurementSystem: MeasurementSystem,
    val isMonthFirst: Boolean
)

/**
 * Central registry of all supported regions.
 * Replaces scattered region data across OnboardingPages, SmartDefaults, and FormatUtils.
 */
object RegionRegistry {

    private val regions: List<RegionConfig> = listOf(
        // ── Americas ──
        RegionConfig("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "$", MeasurementSystem.IMPERIAL, isMonthFirst = true),
        RegionConfig("CA", "Canada", "\uD83C\uDDE8\uD83C\uDDE6", "$", MeasurementSystem.METRIC, isMonthFirst = true),
        RegionConfig("MX", "Mexico", "\uD83C\uDDF2\uD83C\uDDFD", "$", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("BR", "Brazil", "\uD83C\uDDE7\uD83C\uDDF7", "R$", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("AR", "Argentina", "\uD83C\uDDE6\uD83C\uDDF7", "$", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("CO", "Colombia", "\uD83C\uDDE8\uD83C\uDDF4", "$", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("CL", "Chile", "\uD83C\uDDE8\uD83C\uDDF1", "$", MeasurementSystem.METRIC, isMonthFirst = false),
        // ── Europe ──
        RegionConfig("GB", "United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7", "£", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("DE", "Germany", "\uD83C\uDDE9\uD83C\uDDEA", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("FR", "France", "\uD83C\uDDEB\uD83C\uDDF7", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("ES", "Spain", "\uD83C\uDDEA\uD83C\uDDF8", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("IT", "Italy", "\uD83C\uDDEE\uD83C\uDDF9", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("NL", "Netherlands", "\uD83C\uDDF3\uD83C\uDDF1", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("BE", "Belgium", "\uD83C\uDDE7\uD83C\uDDEA", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("PT", "Portugal", "\uD83C\uDDF5\uD83C\uDDF9", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("IE", "Ireland", "\uD83C\uDDEE\uD83C\uDDEA", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("CH", "Switzerland", "\uD83C\uDDE8\uD83C\uDDED", "CHF", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("AT", "Austria", "\uD83C\uDDE6\uD83C\uDDF9", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("SE", "Sweden", "\uD83C\uDDF8\uD83C\uDDEA", "kr", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("NO", "Norway", "\uD83C\uDDF3\uD83C\uDDF4", "kr", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("DK", "Denmark", "\uD83C\uDDE9\uD83C\uDDF0", "kr", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("FI", "Finland", "\uD83C\uDDEB\uD83C\uDDEE", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("PL", "Poland", "\uD83C\uDDF5\uD83C\uDDF1", "zł", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("GR", "Greece", "\uD83C\uDDEC\uD83C\uDDF7", "€", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("TR", "Turkey", "\uD83C\uDDF9\uD83C\uDDF7", "₺", MeasurementSystem.METRIC, isMonthFirst = false),
        // ── Asia & Middle East ──
        RegionConfig("IN", "India", "\uD83C\uDDEE\uD83C\uDDF3", "₹", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("JP", "Japan", "\uD83C\uDDEF\uD83C\uDDF5", "¥", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("KR", "South Korea", "\uD83C\uDDF0\uD83C\uDDF7", "₩", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("CN", "China", "\uD83C\uDDE8\uD83C\uDDF3", "¥", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("SG", "Singapore", "\uD83C\uDDF8\uD83C\uDDEC", "$", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("MY", "Malaysia", "\uD83C\uDDF2\uD83C\uDDFE", "RM", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("TH", "Thailand", "\uD83C\uDDF9\uD83C\uDDED", "฿", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("ID", "Indonesia", "\uD83C\uDDEE\uD83C\uDDE9", "Rp", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("PH", "Philippines", "\uD83C\uDDF5\uD83C\uDDED", "₱", MeasurementSystem.METRIC, isMonthFirst = true),
        RegionConfig("PK", "Pakistan", "\uD83C\uDDF5\uD83C\uDDF0", "Rs", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("BD", "Bangladesh", "\uD83C\uDDE7\uD83C\uDDE9", "৳", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("AE", "UAE", "\uD83C\uDDE6\uD83C\uDDEA", "AED", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("SA", "Saudi Arabia", "\uD83C\uDDF8\uD83C\uDDE6", "SAR", MeasurementSystem.METRIC, isMonthFirst = false),
        // ── Oceania ──
        RegionConfig("AU", "Australia", "\uD83C\uDDE6\uD83C\uDDFA", "$", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("NZ", "New Zealand", "\uD83C\uDDF3\uD83C\uDDFF", "$", MeasurementSystem.METRIC, isMonthFirst = false),
        // ── Africa ──
        RegionConfig("ZA", "South Africa", "\uD83C\uDDFF\uD83C\uDDE6", "R", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("NG", "Nigeria", "\uD83C\uDDF3\uD83C\uDDEC", "₦", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("KE", "Kenya", "\uD83C\uDDF0\uD83C\uDDEA", "KSh", MeasurementSystem.METRIC, isMonthFirst = false),
        RegionConfig("EG", "Egypt", "\uD83C\uDDEA\uD83C\uDDEC", "E£", MeasurementSystem.METRIC, isMonthFirst = false),
        // ── Imperial-system countries (not in picker but needed for unit logic) ──
        RegionConfig("LR", "Liberia", "\uD83C\uDDF1\uD83C\uDDF7", "$", MeasurementSystem.IMPERIAL, isMonthFirst = false),
        RegionConfig("MM", "Myanmar", "\uD83C\uDDF2\uD83C\uDDF2", "K", MeasurementSystem.IMPERIAL, isMonthFirst = false),
    )

    /** All regions for the picker UI. */
    fun allRegions(): List<RegionConfig> = regions

    /** Find a region by ISO country code. Returns null for unknown/CUSTOM codes. */
    fun findByCode(code: String): RegionConfig? = regions.find { it.countryCode == code }

    /** Auto-detect region from device locale. Falls back to a generic config if not in registry. */
    fun detectFromLocale(): RegionConfig {
        val locale = Locale.getDefault()
        val code = locale.country
        findByCode(code)?.let { return it }
        // Build fallback from device locale
        val name = locale.displayCountry.ifBlank { "Unknown" }
        val symbol = try {
            Currency.getInstance(locale).symbol
        } catch (_: Exception) {
            "$"
        }
        return RegionConfig(code, name, "\uD83C\uDF10", symbol, MeasurementSystem.METRIC, isMonthFirst = false)
    }

    /** Build a custom region for "Can't find your country?" flow. */
    fun custom(name: String, currencySymbol: String): RegionConfig =
        RegionConfig("CUSTOM", name, "\uD83C\uDF10", currencySymbol, MeasurementSystem.METRIC, isMonthFirst = false)

    /**
     * Format today's date as a preview string, localized to the region.
     * Uses the region's locale for formatting (e.g., US → "Mar 12, 2026", DE → "12.03.2026").
     */
    fun formatDatePreview(regionCode: String): String {
        val locale = if (regionCode == "CUSTOM" || regionCode.length != 2) {
            Locale.getDefault()
        } else {
            // Pair device language with target country so formatter has both
            // language (for month names) and country (for date order/separators)
            Locale(Locale.getDefault().language, regionCode)
        }
        return try {
            LocalDate.now().format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            )
        } catch (_: Exception) {
            LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }
    }

    /** Country codes that use the imperial measurement system. */
    val imperialCodes: Set<String> = regions
        .filter { it.measurementSystem == MeasurementSystem.IMPERIAL }
        .map { it.countryCode }
        .toSet()

    /** Country codes that use month-first date format (MMM d). */
    val monthFirstCodes: Set<String> = regions
        .filter { it.isMonthFirst }
        .map { it.countryCode }
        .toSet()
}
