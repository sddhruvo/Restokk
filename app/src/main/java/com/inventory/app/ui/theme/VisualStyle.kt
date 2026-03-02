package com.inventory.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventory.app.R

// ─── Visual Style Axis ──────────────────────────────────────────────────
// Independent from color palette. User picks a ColorPalette (AppTheme)
// AND a VisualStyle. Mix and match freely.

enum class VisualStyle(val key: String, val displayName: String) {
    MODERN("modern", "Modern"),
    PAPER_INK("paper_ink", "Paper & Ink");

    companion object {
        fun fromKey(key: String): VisualStyle =
            entries.firstOrNull { it.key == key } ?: MODERN
    }
}

// ─── Card Style ─────────────────────────────────────────────────────────

sealed interface CardStyle {
    /** Standard Material3 Card — current behavior. */
    data object Standard : CardStyle

    /** Hand-drawn wobble-bezier border card. */
    data class InkBorder(
        val strokeWidth: Dp = 2.dp,
        val wobbleAmplitude: Dp = 2.dp,
        val segments: Int = 6,
    ) : CardStyle
}

// ─── Background Style ───────────────────────────────────────────────────

sealed interface BackgroundStyle {
    /** Flat Material3 surface color — current behavior. */
    data object Flat : BackgroundStyle

    /** Subtle paper texture overlay. */
    data class Textured(
        val alpha: Float = 0.04f,
    ) : BackgroundStyle

    /** Horizontal ruled lines like a notebook. */
    data class RuledLines(
        val spacing: Dp = 28.dp,
        val alpha: Float = 0.06f,
    ) : BackgroundStyle
}

// ─── Border Style ───────────────────────────────────────────────────────

sealed interface BorderStyle {
    data object None : BorderStyle
    data class Ink(val color: Color = Color.Unspecified, val width: Dp = 1.5.dp, val wobble: Dp = 3.dp) : BorderStyle
    data class Solid(val color: Color = Color.Unspecified, val width: Dp = 1.dp) : BorderStyle
}

// ─── Progress Style ─────────────────────────────────────────────────────

sealed interface ProgressStyle {
    /** Standard Material3 LinearProgressIndicator. */
    data object Standard : ProgressStyle

    /** Hand-scratched diagonal hatch strokes (////). */
    data class InkHatched(
        val baseSpacing: Float = 0.45f,
        val strokeThickness: Float = 0.28f,
        val washAlpha: Float = 0.15f,
    ) : ProgressStyle
}

// ─── Divider Style ──────────────────────────────────────────────────────

sealed interface DividerStyle {
    /** Standard Material3 HorizontalDivider. */
    data object Standard : DividerStyle

    /** Wobbly ink rule line — bleed + core stroke, bezier wobble. */
    data class InkRule(
        val wobbleAmplitude: Float = 0.15f,
        val bleedAlpha: Float = 0.25f,
        val coreAlpha: Float = 0.85f,
        val segments: Int = 8,
    ) : DividerStyle
}

// ─── Icon Style ─────────────────────────────────────────────────────────

enum class IconStyle { MATERIAL_FILLED, OUTLINED, CUSTOM_SKETCH }

// ─── Handwritten Font Family ─────────────────────────────────────────────

val HandwrittenFontFamily = FontFamily(
    Font(R.font.mali_regular, FontWeight.Normal),
    Font(R.font.mali_medium, FontWeight.Medium),
    Font(R.font.mali_semibold, FontWeight.SemiBold),
    Font(R.font.mali_bold, FontWeight.Bold),
)

/** Typography scale using the Mali handwritten font for Paper & Ink visual style. */
private val HandwrittenTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HandwrittenFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ─── Theme Visuals ──────────────────────────────────────────────────────

data class ThemeVisuals(
    val cardStyle: CardStyle,
    val typography: Typography,
    val shapes: Shapes,
    val backgroundStyle: BackgroundStyle,
    val useElevation: Boolean,
    val defaultCardElevation: Dp,
    val cardBorderStyle: BorderStyle,
    val iconStyle: IconStyle,
    val progressStyle: ProgressStyle,
    val dividerStyle: DividerStyle,
    val scaffoldTransparent: Boolean,
) {
    companion object {
        /** Modern: replicates current behavior exactly. Zero visual change. */
        val Modern = ThemeVisuals(
            cardStyle = CardStyle.Standard,
            typography = com.inventory.app.ui.theme.Typography,
            shapes = AppShapes,
            backgroundStyle = BackgroundStyle.Flat,
            useElevation = true,
            defaultCardElevation = 2.dp,
            cardBorderStyle = BorderStyle.None,
            iconStyle = IconStyle.MATERIAL_FILLED,
            progressStyle = ProgressStyle.Standard,
            dividerStyle = DividerStyle.Standard,
            scaffoldTransparent = false,
        )

        /** Paper & Ink: hand-drawn, organic feel. */
        val PaperInk = ThemeVisuals(
            cardStyle = CardStyle.InkBorder(),
            typography = HandwrittenTypography,
            shapes = PaperInkShapes,
            backgroundStyle = BackgroundStyle.Textured(),
            useElevation = false,
            defaultCardElevation = 0.dp,
            cardBorderStyle = BorderStyle.Ink(),
            iconStyle = IconStyle.CUSTOM_SKETCH,
            progressStyle = ProgressStyle.InkHatched(),
            dividerStyle = DividerStyle.InkRule(),
            scaffoldTransparent = true,
        )
    }
}

/** Organic shapes for Paper & Ink — softer, rounder radii than Modern. */
private val PaperInkShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// ─── Convenience Extension ──────────────────────────────────────────────

/** True when the active visual style is Paper & Ink. */
val ThemeVisuals.isInk: Boolean
    get() = cardStyle is CardStyle.InkBorder

// ─── Ink Design Tokens ─────────────────────────────────────────────────
// Shared visual constants for ALL Paper & Ink themed components.
// Every Themed* composable reads from here — zero hardcoded visual values.

object InkTokens {
    // ── Stroke widths (pen thickness) ──
    val strokeBold = 2.5.dp       // Cards, FAB, dialog borders, checkmark, spinner
    val strokeMedium = 1.5.dp     // TextField, chips, checkbox border, dividers
    val strokeFine = 1.0.dp       // Text button underline, subtle accents

    // ── Fill alphas (ink wash intensity) ──
    const val fillBleed = 0.08f       // Bleed layers underneath strokes (spread effect)
    const val fillLight = 0.12f       // Chip selected, button fill (subtle wash)
    const val fillMedium = 0.15f      // FAB fill (slightly more prominent)
    const val fillOpaque = 0.92f      // Snackbar container (readable above content)

    // ── Border alphas (ink density at different interaction states) ──
    const val borderSketch = 0.30f    // Sketch/secondary stroke overlay in InkBorderCard
    const val borderSubtle = 0.40f    // Unfocused text fields, drag handle, inactive borders
    const val borderMedium = 0.50f    // Unselected chips, text button underline
    const val borderBold = 0.90f      // Focused text fields, selected chips, card borders

    // ── Disabled state alphas (Material3 convention) ──
    const val disabledBorder = 0.25f  // Disabled button/chip border
    const val disabledContent = 0.38f // Disabled text, icons

    // ── Scrim alphas (overlay dimming) ──
    const val scrimSheet = 0.30f      // Bottom sheet dim (lighter — partial overlay)
    const val scrimDialog = 0.35f     // Dialog dim (heavier — full focus steal)

    // ── Wobble amplitudes (hand-drawn irregularity per component scale) ──
    val wobbleLarge = 3.dp            // Cards (InkBorderCard default)
    val wobbleMedium = 2.dp           // FABs, dialogs, large components
    val wobbleSmall = 1.5.dp          // Text fields, chips, checkboxes

    // ── Component sizes ──
    val checkboxSize = 22.dp          // Ink checkbox/switch visual
    val radioSize = 20.dp             // Ink radio button visual
    val dragHandleWidth = 32.dp       // Bottom sheet drag handle
    val dragHandleHeight = 4.dp       // Bottom sheet drag handle
    val fabSize = 56.dp               // Standard FAB size

    // ── Error ──
    val shakeOffset = 3.dp            // Horizontal shake distance on error (TextField)
}

// ─── CompositionLocal ───────────────────────────────────────────────────

val LocalThemeVisuals = staticCompositionLocalOf { ThemeVisuals.Modern }

val MaterialTheme.visuals: ThemeVisuals
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeVisuals.current
