package com.inventory.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

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
