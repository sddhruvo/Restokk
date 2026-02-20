package com.inventory.app.ui.screens.onboarding

import java.util.Currency
import java.util.Locale

// ─── Page types ─────────────────────────────────────────────────────────
// To add a page: 1) Add a data object here  2) Add it to onboardingPages list
// 3) Add a when-branch in OnboardingPageContent.kt  — done.

sealed class OnboardingPage(val id: String, val canSkip: Boolean = true) {
    data object Welcome : OnboardingPage("welcome", canSkip = false)
    data object RegionSetup : OnboardingPage("region")
    data object FirstItems : OnboardingPage("first_items")
    data object AllSet : OnboardingPage("all_set", canSkip = false)
}

val onboardingPages = listOf(
    OnboardingPage.Welcome,
    OnboardingPage.RegionSetup,
    OnboardingPage.FirstItems,
    OnboardingPage.AllSet,
)

// ─── Region data ────────────────────────────────────────────────────────

data class RegionInfo(
    val countryCode: String,
    val countryName: String,
    val currencySymbol: String,
    val dateFormatPreview: String,
    val flag: String
)

/** Popular regions for the picker. */
val popularRegions = listOf(
    RegionInfo("US", "United States", "$", "Feb 19, 2026", "\uD83C\uDDFA\uD83C\uDDF8"),
    RegionInfo("GB", "United Kingdom", "£", "19 Feb 2026", "\uD83C\uDDEC\uD83C\uDDE7"),
    RegionInfo("IN", "India", "₹", "19/02/2026", "\uD83C\uDDEE\uD83C\uDDF3"),
    RegionInfo("CA", "Canada", "$", "Feb 19, 2026", "\uD83C\uDDE8\uD83C\uDDE6"),
    RegionInfo("AU", "Australia", "$", "19/02/2026", "\uD83C\uDDE6\uD83C\uDDFA"),
    RegionInfo("DE", "Germany", "€", "19.02.2026", "\uD83C\uDDE9\uD83C\uDDEA"),
    RegionInfo("FR", "France", "€", "19/02/2026", "\uD83C\uDDEB\uD83C\uDDF7"),
    RegionInfo("ES", "Spain", "€", "19/02/2026", "\uD83C\uDDEA\uD83C\uDDF8"),
    RegionInfo("IT", "Italy", "€", "19/02/2026", "\uD83C\uDDEE\uD83C\uDDF9"),
    RegionInfo("BR", "Brazil", "R$", "19/02/2026", "\uD83C\uDDE7\uD83C\uDDF7"),
    RegionInfo("JP", "Japan", "¥", "2026/02/19", "\uD83C\uDDEF\uD83C\uDDF5"),
    RegionInfo("MX", "Mexico", "$", "19/02/2026", "\uD83C\uDDF2\uD83C\uDDFD"),
    RegionInfo("NZ", "New Zealand", "$", "19/02/2026", "\uD83C\uDDF3\uD83C\uDDFF"),
    RegionInfo("ZA", "South Africa", "R", "19/02/2026", "\uD83C\uDDFF\uD83C\uDDE6"),
    RegionInfo("AE", "UAE", "AED", "19/02/2026", "\uD83C\uDDE6\uD83C\uDDEA"),
)

/** Auto-detect region from device locale. */
fun detectRegion(): RegionInfo {
    val locale = Locale.getDefault()
    val countryCode = locale.country
    // Try to find in popular regions
    popularRegions.find { it.countryCode == countryCode }?.let { return it }
    // Fallback: build from locale
    val countryName = locale.displayCountry.ifBlank { "Unknown" }
    val currencySymbol = try {
        Currency.getInstance(locale).symbol
    } catch (_: Exception) {
        "$"
    }
    val datePreview = try {
        java.time.LocalDate.of(2026, 2, 19)
            .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
    } catch (_: Exception) {
        "19 Feb 2026"
    }
    return RegionInfo(countryCode, countryName, currencySymbol, datePreview, "\uD83C\uDF10")
}

// ─── Post-onboarding route choice ───────────────────────────────────────

enum class StartChoice {
    SCAN_KITCHEN,
    ADD_MANUALLY
}
