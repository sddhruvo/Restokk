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
    // Americas
    RegionInfo("US", "United States", "$", "Feb 19, 2026", "\uD83C\uDDFA\uD83C\uDDF8"),
    RegionInfo("CA", "Canada", "$", "Feb 19, 2026", "\uD83C\uDDE8\uD83C\uDDE6"),
    RegionInfo("MX", "Mexico", "$", "19/02/2026", "\uD83C\uDDF2\uD83C\uDDFD"),
    RegionInfo("BR", "Brazil", "R$", "19/02/2026", "\uD83C\uDDE7\uD83C\uDDF7"),
    RegionInfo("AR", "Argentina", "$", "19/02/2026", "\uD83C\uDDE6\uD83C\uDDF7"),
    RegionInfo("CO", "Colombia", "$", "19/02/2026", "\uD83C\uDDE8\uD83C\uDDF4"),
    RegionInfo("CL", "Chile", "$", "19-02-2026", "\uD83C\uDDE8\uD83C\uDDF1"),
    // Europe
    RegionInfo("GB", "United Kingdom", "£", "19 Feb 2026", "\uD83C\uDDEC\uD83C\uDDE7"),
    RegionInfo("DE", "Germany", "€", "19.02.2026", "\uD83C\uDDE9\uD83C\uDDEA"),
    RegionInfo("FR", "France", "€", "19/02/2026", "\uD83C\uDDEB\uD83C\uDDF7"),
    RegionInfo("ES", "Spain", "€", "19/02/2026", "\uD83C\uDDEA\uD83C\uDDF8"),
    RegionInfo("IT", "Italy", "€", "19/02/2026", "\uD83C\uDDEE\uD83C\uDDF9"),
    RegionInfo("NL", "Netherlands", "€", "19-02-2026", "\uD83C\uDDF3\uD83C\uDDF1"),
    RegionInfo("BE", "Belgium", "€", "19/02/2026", "\uD83C\uDDE7\uD83C\uDDEA"),
    RegionInfo("PT", "Portugal", "€", "19/02/2026", "\uD83C\uDDF5\uD83C\uDDF9"),
    RegionInfo("IE", "Ireland", "€", "19/02/2026", "\uD83C\uDDEE\uD83C\uDDEA"),
    RegionInfo("CH", "Switzerland", "CHF", "19.02.2026", "\uD83C\uDDE8\uD83C\uDDED"),
    RegionInfo("AT", "Austria", "€", "19.02.2026", "\uD83C\uDDE6\uD83C\uDDF9"),
    RegionInfo("SE", "Sweden", "kr", "2026-02-19", "\uD83C\uDDF8\uD83C\uDDEA"),
    RegionInfo("NO", "Norway", "kr", "19.02.2026", "\uD83C\uDDF3\uD83C\uDDF4"),
    RegionInfo("DK", "Denmark", "kr", "19.02.2026", "\uD83C\uDDE9\uD83C\uDDF0"),
    RegionInfo("FI", "Finland", "€", "19.2.2026", "\uD83C\uDDEB\uD83C\uDDEE"),
    RegionInfo("PL", "Poland", "zł", "19.02.2026", "\uD83C\uDDF5\uD83C\uDDF1"),
    RegionInfo("GR", "Greece", "€", "19/02/2026", "\uD83C\uDDEC\uD83C\uDDF7"),
    RegionInfo("TR", "Turkey", "₺", "19.02.2026", "\uD83C\uDDF9\uD83C\uDDF7"),
    // Asia & Middle East
    RegionInfo("IN", "India", "₹", "19/02/2026", "\uD83C\uDDEE\uD83C\uDDF3"),
    RegionInfo("JP", "Japan", "¥", "2026/02/19", "\uD83C\uDDEF\uD83C\uDDF5"),
    RegionInfo("KR", "South Korea", "₩", "2026. 2. 19.", "\uD83C\uDDF0\uD83C\uDDF7"),
    RegionInfo("CN", "China", "¥", "2026/02/19", "\uD83C\uDDE8\uD83C\uDDF3"),
    RegionInfo("SG", "Singapore", "$", "19/02/2026", "\uD83C\uDDF8\uD83C\uDDEC"),
    RegionInfo("MY", "Malaysia", "RM", "19/02/2026", "\uD83C\uDDF2\uD83C\uDDFE"),
    RegionInfo("TH", "Thailand", "฿", "19/02/2026", "\uD83C\uDDF9\uD83C\uDDED"),
    RegionInfo("ID", "Indonesia", "Rp", "19/02/2026", "\uD83C\uDDEE\uD83C\uDDE9"),
    RegionInfo("PH", "Philippines", "₱", "02/19/2026", "\uD83C\uDDF5\uD83C\uDDED"),
    RegionInfo("PK", "Pakistan", "Rs", "19/02/2026", "\uD83C\uDDF5\uD83C\uDDF0"),
    RegionInfo("BD", "Bangladesh", "৳", "19/02/2026", "\uD83C\uDDE7\uD83C\uDDE9"),
    RegionInfo("AE", "UAE", "AED", "19/02/2026", "\uD83C\uDDE6\uD83C\uDDEA"),
    RegionInfo("SA", "Saudi Arabia", "SAR", "19/02/2026", "\uD83C\uDDF8\uD83C\uDDE6"),
    // Oceania
    RegionInfo("AU", "Australia", "$", "19/02/2026", "\uD83C\uDDE6\uD83C\uDDFA"),
    RegionInfo("NZ", "New Zealand", "$", "19/02/2026", "\uD83C\uDDF3\uD83C\uDDFF"),
    // Africa
    RegionInfo("ZA", "South Africa", "R", "19/02/2026", "\uD83C\uDDFF\uD83C\uDDE6"),
    RegionInfo("NG", "Nigeria", "₦", "19/02/2026", "\uD83C\uDDF3\uD83C\uDDEC"),
    RegionInfo("KE", "Kenya", "KSh", "19/02/2026", "\uD83C\uDDF0\uD83C\uDDEA"),
    RegionInfo("EG", "Egypt", "E£", "19/02/2026", "\uD83C\uDDEA\uD83C\uDDEC"),
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
