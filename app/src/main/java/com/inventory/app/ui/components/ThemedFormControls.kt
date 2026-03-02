package com.inventory.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals

/**
 * Drop-in replacement for [Checkbox] with a hand-drawn ink checkbox
 * in Paper & Ink mode. Same visual as [ThemedSwitch]'s ink checkbox
 * but with [Role.Checkbox] semantics.
 *
 * Modern mode delegates to the standard [Checkbox].
 */
@Composable
fun ThemedCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
        return
    }

    val reduceMotion = LocalReduceMotion.current
    val colorScheme = MaterialTheme.colorScheme

    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(PaperInkMotion.DurationSettle),
        label = "inkCheckbox"
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
            role = Role.Checkbox,
            enabled = enabled
        )
    } else {
        modifier
    }

    Canvas(
        modifier = resolvedModifier.defaultMinSize(
            minWidth = InkTokens.checkboxSize,
            minHeight = InkTokens.checkboxSize
        )
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

        // Animated checkmark
        if (checkProgress > 0f) {
            val startX = w * 0.20f
            val startY = h * 0.50f
            val midX = w * 0.42f
            val midY = h * 0.72f
            val endX = w * 0.80f
            val endY = h * 0.28f

            val checkPath = Path().apply {
                moveTo(startX, startY)
                val seg1 = (checkProgress / 0.4f).coerceIn(0f, 1f)
                lineTo(
                    startX + (midX - startX) * seg1,
                    startY + (midY - startY) * seg1
                )
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

/**
 * Drop-in replacement for [RadioButton] with a wobbly ink circle
 * and animated dot fill in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [RadioButton].
 */
@Composable
fun ThemedRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        RadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled)
        return
    }

    val reduceMotion = LocalReduceMotion.current
    val colorScheme = MaterialTheme.colorScheme

    // Animate fill: 0 → 1 on selection (used for fill + border transitions)
    val fillProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else PaperInkMotion.BouncySpring,
        label = "inkRadioFill"
    )

    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }

    val density = LocalDensity.current
    val wobblePx = with(density) { InkTokens.wobbleSmall.toPx() }

    val resolvedModifier = if (onClick != null) {
        modifier.selectable(
            selected = selected,
            onClick = onClick,
            role = Role.RadioButton,
            enabled = enabled
        )
    } else {
        modifier
    }

    Canvas(
        modifier = resolvedModifier.defaultMinSize(
            minWidth = InkTokens.radioSize,
            minHeight = InkTokens.radioSize
        )
    ) {
        val w = size.width
        val h = size.height

        // Wobble circle border path
        val circlePath = buildWobbleBorderPath(
            width = w,
            height = h,
            cornerRadius = w / 2f,
            wobbleAmplitude = wobblePx,
            wobbleSeed = wobbleSeed,
            segments = 3
        )

        if (fillProgress > 0f) {
            // ── Selected: "bubble in" — fill the entire circle like a scantron answer
            // Solid primary fill at high alpha — unmistakable
            drawCircle(
                color = colorScheme.primary.copy(alpha = 0.65f * fillProgress),
                radius = (minOf(w, h) / 2f) * fillProgress,
                center = center
            )
            // Bold primary border
            drawPath(
                path = circlePath,
                color = colorScheme.primary,
                style = Stroke(
                    width = InkTokens.strokeBold.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else {
            // ── Unselected: faint empty ring
            drawPath(
                path = circlePath,
                color = colorScheme.onSurface.copy(alpha = InkTokens.borderSubtle),
                style = Stroke(
                    width = InkTokens.strokeFine.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
