package com.inventory.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals

/**
 * Drop-in replacement for [Switch] that renders as a hand-drawn ink checkbox
 * with an animated checkmark write-in in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [Switch].
 *
 * Semantic role remains [Role.Switch] regardless of visual appearance.
 */
@Composable
fun ThemedSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
        return
    }

    val reduceMotion = LocalReduceMotion.current
    val colorScheme = MaterialTheme.colorScheme

    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(PaperInkMotion.DurationSettle),
        label = "inkCheckmark"
    )

    val borderColor = colorScheme.onSurface.copy(alpha = InkTokens.borderMedium)
    val checkColor = colorScheme.primary
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }

    val density = LocalDensity.current
    val borderStrokePx = with(density) { InkTokens.strokeMedium.toPx() }
    val checkStrokePx = with(density) { InkTokens.strokeBold.toPx() }
    val wobblePx = with(density) { InkTokens.wobbleSmall.toPx() }

    val resolvedModifier = if (onCheckedChange != null) {
        modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            role = Role.Switch,
            enabled = enabled
        )
    } else {
        modifier
    }

    Canvas(
        modifier = resolvedModifier.size(InkTokens.checkboxSize)
    ) {
        val w = size.width
        val h = size.height

        // Wobble square border
        val squarePath = buildWobbleBorderPath(
            width = w,
            height = h,
            cornerRadius = w * 0.12f,
            wobbleAmplitude = wobblePx,
            wobbleSeed = wobbleSeed,
            segments = 2
        )

        drawPath(
            path = squarePath,
            color = borderColor,
            style = Stroke(
                width = borderStrokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Animated checkmark — manual interpolation of two line segments
        if (checkProgress > 0f) {
            val startX = w * 0.20f
            val startY = h * 0.50f
            val midX = w * 0.42f
            val midY = h * 0.72f
            val endX = w * 0.80f
            val endY = h * 0.28f

            val checkPath = Path().apply {
                moveTo(startX, startY)
                // First segment (start → mid): 0..0.4 of progress
                val seg1 = (checkProgress / 0.4f).coerceIn(0f, 1f)
                lineTo(
                    startX + (midX - startX) * seg1,
                    startY + (midY - startY) * seg1
                )
                // Second segment (mid → end): 0.4..1.0 of progress
                if (checkProgress > 0.4f) {
                    val seg2 = ((checkProgress - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    lineTo(
                        midX + (endX - midX) * seg2,
                        midY + (endY - midY) * seg2
                    )
                }
            }

            drawPath(
                path = checkPath,
                color = checkColor,
                style = Stroke(
                    width = checkStrokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
