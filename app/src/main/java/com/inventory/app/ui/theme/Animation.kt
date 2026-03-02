package com.inventory.app.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

// ─── Paper & Ink Motion System ──────────────────────────────────────────
// Shared spring constants used across the entire app.
// See memory/animation-theme.md for the full spec.

object PaperInkMotion {
    val BouncySpring: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 200f)
    val WobblySpring: SpringSpec<Float> = spring(dampingRatio = 0.3f, stiffness = 200f)
    val GentleSpring: SpringSpec<Float> = spring(dampingRatio = 1.0f, stiffness = 200f)
    val SettleSpring: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 120f)

    const val STAGGER_MS = 70L
    const val MAX_STAGGER_ITEMS = 5

    // Duration tokens — use with tween() calls
    const val DurationQuick = 100       // Very fast: dialog exit fade, micro-transitions
    const val DurationShort = 150       // Quick fades, dialog entrance fade
    const val DurationEntry = 200       // Chip bleed-in, ink underline pulse
    const val DurationSettle = 250      // Checkmark write-in, toggle transitions
    const val DurationMedium = 300      // Standard transitions, TextField focus fade
    const val DurationLong = 500        // Slow entrances, shimmer
    const val DurationChart = 800       // Chart draw-on, InkSpinner cycle

    // ─── Layer 1: Birth (entrance sketch-in) ────────────────────────────
    const val EntranceRotationStart = -8f
    const val EntranceDurationAlpha = 250

    // ─── Layer 2: Life (idle breathing) ──────────────────────────────────
    const val BreatheScaleDefault = 1.015f
    const val BreathePeriodDefault = 2500
    const val MaxSimultaneousBreathing = 3

    // ─── Layer 3: Response (touch reaction) ──────────────────────────────
    const val PressScale = 0.88f
    const val PressDuration = 80

    // ─── Layer 6: Themed component transitions ─────────────────────────
    val ShakeSpring: SpringSpec<Float> = spring(dampingRatio = 0.3f, stiffness = 800f)
    const val DialogEnterScale = 0.92f
    const val DialogExitScale = 0.95f

    // ─── Layer 4: Empathy (data-driven) ──────────────────────────────────
    const val HeartBeatCalmPeriod = 2500
    const val HeartBeatUrgentPeriod = 1000

    // ─── Layer 5: Surprise (easter eggs) ─────────────────────────────────
    const val SurpriseChance = 0.05f
    const val SurpriseDelay = 200L
}
