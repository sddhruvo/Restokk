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

        val ClassicGreen = AppColors(
            statusExpired = Color(0xFFD32F2F),
            statusExpiring = Color(0xFFFF9800),
            statusLowStock = Color(0xFFFFC107),
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

        val AmoledDark = AppColors(
            statusExpired = Color(0xFFD32F2F),
            statusExpiring = Color(0xFFFF9800),
            statusLowStock = Color(0xFFFFC107),
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
    }
}

val LocalAppColors = staticCompositionLocalOf { AppColors.ClassicGreen }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
