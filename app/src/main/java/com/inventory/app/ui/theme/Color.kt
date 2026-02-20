package com.inventory.app.ui.theme

import androidx.compose.ui.graphics.Color

// Theme enum
enum class AppTheme(val key: String, val displayName: String) {
    CLASSIC_GREEN("classic_green", "Classic Green"),
    WARM_CREAM("warm_cream", "Warm Cream"),
    AMOLED_DARK("amoled_dark", "AMOLED Dark");

    companion object {
        fun fromKey(key: String): AppTheme =
            entries.firstOrNull { it.key == key } ?: CLASSIC_GREEN
    }
}

// Primary palette - warm green tones for inventory/pantry feel
val md_theme_light_primary = Color(0xFF2E7D32)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFA5D6A7)
val md_theme_light_onPrimaryContainer = Color(0xFF002204)

val md_theme_light_secondary = Color(0xFF526350)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD4E8D0)
val md_theme_light_onSecondaryContainer = Color(0xFF101F10)

val md_theme_light_tertiary = Color(0xFF39656B)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFBCEBF2)
val md_theme_light_onTertiaryContainer = Color(0xFF001F23)

val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)

val md_theme_light_background = Color(0xFFF8FAF0)
val md_theme_light_onBackground = Color(0xFF1A1C18)
val md_theme_light_surface = Color(0xFFF8FAF0)
val md_theme_light_onSurface = Color(0xFF1A1C18)
val md_theme_light_surfaceVariant = Color(0xFFDEE5D9)
val md_theme_light_onSurfaceVariant = Color(0xFF424940)
val md_theme_light_outline = Color(0xFF72796F)

// Dark theme
val md_theme_dark_primary = Color(0xFF81C784)
val md_theme_dark_onPrimary = Color(0xFF00390B)
val md_theme_dark_primaryContainer = Color(0xFF005315)
val md_theme_dark_onPrimaryContainer = Color(0xFFA5D6A7)

val md_theme_dark_secondary = Color(0xFFB9CCB5)
val md_theme_dark_onSecondary = Color(0xFF243424)
val md_theme_dark_secondaryContainer = Color(0xFF3A4B39)
val md_theme_dark_onSecondaryContainer = Color(0xFFD4E8D0)

val md_theme_dark_tertiary = Color(0xFFA1CED5)
val md_theme_dark_onTertiary = Color(0xFF00363C)
val md_theme_dark_tertiaryContainer = Color(0xFF1F4D53)
val md_theme_dark_onTertiaryContainer = Color(0xFFBCEBF2)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_dark_background = Color(0xFF000000)
val md_theme_dark_onBackground = Color(0xFFE2E3DC)
val md_theme_dark_surface = Color(0xFF000000)
val md_theme_dark_onSurface = Color(0xFFE2E3DC)
val md_theme_dark_surfaceVariant = Color(0xFF1E2A1E)
val md_theme_dark_onSurfaceVariant = Color(0xFFC2C9BD)
val md_theme_dark_outline = Color(0xFF8C9389)

// Status colors used across the app
val ExpiryRed = Color(0xFFD32F2F)
val ExpiryOrange = Color(0xFFFF9800)
val StockYellow = Color(0xFFFFC107)
val StockGreen = Color(0xFF4CAF50)

// Score colors — shared between Dashboard and PantryHealth
val ScoreTeal = Color(0xFF26A69A)
val ScoreBlue = Color(0xFF42A5F5)

/** Map a 0-100 home score to its display color. */
fun scoreToColor(score: Int): Color = when {
    score >= 70 -> StockGreen
    score >= 50 -> ScoreTeal
    score >= 30 -> ExpiryOrange
    score >= 1  -> ScoreBlue
    else        -> Color.Gray
}

// Warm Cream palette — modern minimalist light theme
// Background: very light off-white, cards: white/frosted, primary: iOS blue
val md_theme_cream_primary = Color(0xFF007AFF)           // iOS blue accent
val md_theme_cream_onPrimary = Color(0xFFFFFFFF)
val md_theme_cream_primaryContainer = Color(0xFFD6E8FF)  // light blue tint for stat cards
val md_theme_cream_onPrimaryContainer = Color(0xFF001A40)

val md_theme_cream_secondary = Color(0xFF34C759)         // green accent
val md_theme_cream_onSecondary = Color(0xFFFFFFFF)
val md_theme_cream_secondaryContainer = Color(0xFFD4F5DC)
val md_theme_cream_onSecondaryContainer = Color(0xFF002110)

val md_theme_cream_tertiary = Color(0xFFAF52DE)          // purple accent
val md_theme_cream_onTertiary = Color(0xFFFFFFFF)
val md_theme_cream_tertiaryContainer = Color(0xFFF0DFFB)
val md_theme_cream_onTertiaryContainer = Color(0xFF2B0042)

val md_theme_cream_error = Color(0xFFBA1A1A)
val md_theme_cream_onError = Color(0xFFFFFFFF)
val md_theme_cream_errorContainer = Color(0xFFFFDAD6)
val md_theme_cream_onErrorContainer = Color(0xFF410002)

val md_theme_cream_background = Color(0xFFF8F9FA)        // very light warm off-white
val md_theme_cream_onBackground = Color(0xFF000000)       // black text
val md_theme_cream_surface = Color(0xFFFFFFFF)            // pure white cards
val md_theme_cream_onSurface = Color(0xFF000000)          // black text
val md_theme_cream_surfaceVariant = Color(0xFFF5F5F7)    // subtle light gray
val md_theme_cream_onSurfaceVariant = Color(0xFF6E6E73)  // medium gray for secondary text
val md_theme_cream_outline = Color(0xFFD1D1D6)           // light border gray

// Dashboard card accent colors (per-card icon tints)
val CardBlue = Color(0xFF007AFF)     // Total Items, Reports
val CardOrange = Color(0xFFFF9500)   // Expiring Soon
val CardGreen = Color(0xFF34C759)    // Low Stock, Total Value, Recognize
val CardPurple = Color(0xFFAF52DE)   // Shopping
val CardGold = Color(0xFFC99700)     // Scan barcode

// Report card accent colors
val ReportExpiring = Color(0xFFE65100)
val ReportLowStock = Color(0xFFC62828)
val ReportSpending = Color(0xFF1565C0)
val ReportUsage = Color(0xFF6A1B9A)
val ReportInventory = Color(0xFF2E7D32)
