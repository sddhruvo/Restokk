package com.inventory.app.ui.screens.onboarding

import com.inventory.app.domain.model.RegionConfig
import com.inventory.app.domain.model.RegionRegistry

// ─── Page types ─────────────────────────────────────────────────────────
// To add a page: 1) Add a data object here  2) Add it to onboardingPages list
// 3) Add a when-branch in OnboardingPageContent.kt  — done.

sealed class OnboardingPage(val id: String) {
    data object StoryOpens : OnboardingPage("story_opens")
    data object YourKitchen : OnboardingPage("your_kitchen")
    data object FirstMagic : OnboardingPage("first_magic")
}

val onboardingPages = listOf(
    OnboardingPage.StoryOpens,
    OnboardingPage.YourKitchen,
    OnboardingPage.FirstMagic,
)

// ─── Region data ────────────────────────────────────────────────────────

data class RegionInfo(
    val countryCode: String,
    val countryName: String,
    val currencySymbol: String,
    val dateFormatPreview: String,
    val flag: String
) {
    companion object {
        fun fromConfig(config: RegionConfig) = RegionInfo(
            countryCode = config.countryCode,
            countryName = config.countryName,
            currencySymbol = config.defaultCurrencySymbol,
            dateFormatPreview = RegionRegistry.formatDatePreview(config.countryCode),
            flag = config.flag
        )
    }
}

/** Popular regions for the picker — dates are always today's date, locale-formatted. */
val popularRegions: List<RegionInfo>
    get() = RegionRegistry.allRegions().map { RegionInfo.fromConfig(it) }

/** Auto-detect region from device locale. */
fun detectRegion(): RegionInfo = RegionInfo.fromConfig(RegionRegistry.detectFromLocale())

// ─── User preference ────────────────────────────────────────────────────

enum class UserPreference(val label: String, val subtitle: String) {
    WASTE("Never waste food", "Track expiry, reduce waste"),
    INVENTORY("Always know what I have", "Full inventory at a glance"),
    COOK("Cook with what's here", "Recipes from your kitchen")
}
