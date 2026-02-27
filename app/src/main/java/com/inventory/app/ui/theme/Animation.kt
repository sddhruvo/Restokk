package com.inventory.app.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

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
}

object AppAnimation {
    // Durations
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500
    const val CHART = 800

    // Spring specs
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Tween specs
    fun <T> tweenMedium() = tween<T>(durationMillis = MEDIUM)
    fun <T> tweenShort() = tween<T>(durationMillis = SHORT)
    fun <T> tweenLong() = tween<T>(durationMillis = LONG)
}
