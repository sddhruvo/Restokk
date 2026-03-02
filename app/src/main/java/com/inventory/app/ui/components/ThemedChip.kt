package com.inventory.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals

/**
 * Drop-in replacement for [FilterChip] with a wobbly ink pill border
 * and ink bleed fill on selection in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [FilterChip].
 */
@Composable
fun ThemedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = label,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = colors,
        )
        return
    }

    val reduceMotion = LocalReduceMotion.current
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
    val strokePx = with(density) { InkTokens.strokeMedium.toPx() }
    val wobblePx = with(density) { InkTokens.wobbleSmall.toPx() }

    val fillAlpha by animateFloatAsState(
        targetValue = if (selected) InkTokens.fillLight else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(PaperInkMotion.DurationEntry),
        label = "chipFill"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (selected) InkTokens.borderBold else InkTokens.borderMedium,
        animationSpec = if (reduceMotion) tween(0) else tween(PaperInkMotion.DurationEntry),
        label = "chipBorder"
    )

    val borderColor = colorScheme.onSurface
    val contentColor = colorScheme.onSurface

    Box(
        modifier = modifier
            .height(32.dp)
            .drawBehind {
                val cr = size.height / 2f

                // Fill
                if (fillAlpha > 0f) {
                    drawRoundRect(
                        color = colorScheme.primary.copy(alpha = fillAlpha),
                        cornerRadius = CornerRadius(cr, cr)
                    )
                }

                val path = buildWobbleBorderPath(
                    width = size.width,
                    height = size.height,
                    cornerRadius = cr,
                    wobbleAmplitude = wobblePx,
                    wobbleSeed = wobbleSeed,
                    segments = 3
                )

                // Border
                drawPath(
                    path = path,
                    color = borderColor.copy(alpha = borderAlpha),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            .clip(RoundedCornerShape(50))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.invoke()
                    label()
                    trailingIcon?.invoke()
                }
            }
        }
    }
}

/**
 * Drop-in replacement for [InputChip] with wobbly ink border in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [InputChip].
 */
@Composable
fun ThemedInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = InputChipDefaults.inputChipColors(),
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        InputChip(
            selected = selected,
            onClick = onClick,
            label = label,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = colors,
        )
        return
    }

    // InputChip reuses the same ink style as FilterChip
    ThemedFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
    )
}
