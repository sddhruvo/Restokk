package com.inventory.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens that change per theme.
 *
 * Tier 1: Must be customised for each theme (status, score, card accents, report accents).
 * Tier 2: Optional overrides — sensible defaults shared across themes.
 */
data class AppColors(
    // ── Tier 1: Status ──
    val statusExpired: Color,
    val statusExpiring: Color,
    val statusLowStock: Color,
    val statusMediumStock: Color,
    val statusGoodStock: Color,
    val statusInStock: Color,

    // ── Tier 1: Score ──
    val scoreTeal: Color,
    val scoreBlue: Color,

    // ── Tier 1: Card accents ──
    val accentBlue: Color,
    val accentOrange: Color,
    val accentGreen: Color,
    val accentPurple: Color,
    val accentGold: Color,

    // ── Tier 1: Report accents ──
    val reportExpiring: Color,
    val reportLowStock: Color,
    val reportSpending: Color,
    val reportUsage: Color,
    val reportInventory: Color,

    // ── Tier 2: Save action (ink blot) ──
    val saveActionIdle: Color = DefaultSaveIdle,
    val saveActionSaved: Color = DefaultSaveSaved,
    val saveActionCheck: Color = DefaultSaveCheck,

    // ── Tier 2: Feature accents (shared defaults, override if needed) ──
    val cookAccent: Color = DefaultCookAccent,
    val starRating: Color = DefaultStarRating,
    val favoriteActive: Color = DefaultFavoriteActive,
    val difficultyEasy: Color = DefaultDifficultyEasy,
    val difficultyMedium: Color = DefaultDifficultyMedium,
    val difficultyHard: Color = DefaultDifficultyHard,
    val tourHighlightGreen: Color = DefaultTourHighlightGreen,
    val tourHighlightAmber: Color = DefaultTourHighlightAmber,
    val swipeDeleteColor: Color = DefaultSwipeDeleteColor,
    val entityDefaultColor: Color = DefaultEntityDefaultColor,
) {
    /**
     * 4-tier stock-level → color mapping.
     *
     * The user's low-stock setting defines the "critical" cutoff.
     * The remaining range (threshold → 100%) is split into 3 equal bands.
     *
     * Example with default 25%: 0%=expired, 1-25%=critical, 26-50%=medium, 51-75%=good, 76-100%=full.
     */
    fun stockColor(ratio: Float, lowThreshold: Float): Color {
        if (ratio <= 0f) return statusExpired
        if (ratio <= lowThreshold) return statusLowStock
        val band = (1f - lowThreshold) / 3f
        if (ratio <= lowThreshold + band) return statusMediumStock
        if (ratio <= lowThreshold + band * 2f) return statusGoodStock
        return statusInStock
    }

    /** Map a 0-100 home score to its display color. */
    fun scoreToColor(score: Int): Color = when {
        score >= 70 -> statusInStock
        score >= 50 -> scoreTeal
        score >= 30 -> statusExpiring
        score >= 1  -> scoreBlue
        else        -> Color.Gray
    }

    companion object {
        // ── Tier 2 defaults (reused across themes) ──
        private val DefaultCookAccent = Color(0xFFE85D3A)
        private val DefaultStarRating = Color(0xFFFFB800)
        private val DefaultFavoriteActive = Color(0xFFE85D3A)
        private val DefaultDifficultyEasy = Color(0xFF4CAF50)
        private val DefaultDifficultyMedium = Color(0xFFFF9500)
        private val DefaultDifficultyHard = Color(0xFFFF3B30)
        private val DefaultTourHighlightGreen = Color(0xFF4CAF50)
        private val DefaultTourHighlightAmber = Color(0xFFFF9800)
        private val DefaultSwipeDeleteColor = Color(0xFFE53935)
        private val DefaultEntityDefaultColor = Color(0xFF6c757d)

        // Save action defaults — warm dark ink for light themes
        private val DefaultSaveIdle = Color(0xFF3C2F1E)     // Warm sepia-brown ink
        private val DefaultSaveSaved = Color(0xFF4A5E30)    // Muted olive green (dried ink)
        private val DefaultSaveCheck = Color(0xFFF5F0E6)    // Warm cream checkmark

        val ClassicGreen = AppColors(
            statusExpired = Color(0xFFD32F2F),
            statusExpiring = Color(0xFFFF9800),
            statusLowStock = Color(0xFFFFC107),
            statusMediumStock = Color(0xFFFF9800),
            statusGoodStock = Color(0xFF66BB6A),
            statusInStock = Color(0xFF4CAF50),
            scoreTeal = Color(0xFF26A69A),
            scoreBlue = Color(0xFF42A5F5),
            accentBlue = Color(0xFF007AFF),
            accentOrange = Color(0xFFFF9500),
            accentGreen = Color(0xFF34C759),
            accentPurple = Color(0xFFAF52DE),
            accentGold = Color(0xFFC99700),
            reportExpiring = Color(0xFFE65100),
            reportLowStock = Color(0xFFC62828),
            reportSpending = Color(0xFF1565C0),
            reportUsage = Color(0xFF6A1B9A),
            reportInventory = Color(0xFF2E7D32),
        )

        val WarmCream = AppColors(
            statusExpired = Color(0xFFD32F2F),
            statusExpiring = Color(0xFFFF9800),
            statusLowStock = Color(0xFFFFC107),
            statusMediumStock = Color(0xFFFF9800),
            statusGoodStock = Color(0xFF66BB6A),
            statusInStock = Color(0xFF4CAF50),
            scoreTeal = Color(0xFF26A69A),
            scoreBlue = Color(0xFF42A5F5),
            accentBlue = Color(0xFF007AFF),
            accentOrange = Color(0xFFFF9500),
            accentGreen = Color(0xFF34C759),
            accentPurple = Color(0xFFAF52DE),
            accentGold = Color(0xFFC99700),
            reportExpiring = Color(0xFFE65100),
            reportLowStock = Color(0xFFC62828),
            reportSpending = Color(0xFF1565C0),
            reportUsage = Color(0xFF6A1B9A),
            reportInventory = Color(0xFF2E7D32),
            // Clean slate-blue for modern cream — not brown (no paper texture here)
            saveActionIdle = Color(0xFF1C3A5E),      // Deep navy ink
            saveActionSaved = Color(0xFF2D6A4F),      // Forest teal green
            saveActionCheck = Color(0xFFFFFFFF),       // Pure white check
        )

        val AmoledDark = AppColors(
            statusExpired = Color(0xFFD32F2F),
            statusExpiring = Color(0xFFFF9800),
            statusLowStock = Color(0xFFFFC107),
            statusMediumStock = Color(0xFFFF9800),
            statusGoodStock = Color(0xFF66BB6A),
            statusInStock = Color(0xFF4CAF50),
            scoreTeal = Color(0xFF26A69A),
            scoreBlue = Color(0xFF42A5F5),
            accentBlue = Color(0xFF007AFF),
            accentOrange = Color(0xFFFF9500),
            accentGreen = Color(0xFF34C759),
            accentPurple = Color(0xFFAF52DE),
            accentGold = Color(0xFFC99700),
            reportExpiring = Color(0xFFE65100),
            reportLowStock = Color(0xFFC62828),
            reportSpending = Color(0xFF1565C0),
            reportUsage = Color(0xFF6A1B9A),
            reportInventory = Color(0xFF2E7D32),
            // Luminous against pure black — glow-ink feel
            saveActionIdle = Color(0xFFD4C5A9),      // Warm parchment/gold (light on dark)
            saveActionSaved = Color(0xFF81C784),      // Soft green glow (matches dark primary)
            saveActionCheck = Color(0xFF0A0A0A),       // Near-black check on light circle
        )
    }
}

val LocalAppColors = staticCompositionLocalOf { AppColors.ClassicGreen }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
