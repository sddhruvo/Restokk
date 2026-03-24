package com.inventory.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals

/**
 * Drop-in replacement for [Button] with a wobbly ink pill border and
 * subtle ink wash fill in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [Button].
 */
@Composable
fun ThemedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    inkColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        Button(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors, content = content)
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
    val strokePx = with(density) { InkTokens.strokeMedium.toPx() }
    val wobblePx = with(density) { InkTokens.wobbleSmall.toPx() }

    val resolvedInkColor = if (inkColor == Color.Unspecified) colorScheme.primary else inkColor
    val borderColor = if (enabled) resolvedInkColor
        else colorScheme.onSurface.copy(alpha = InkTokens.disabledBorder)
    val fillColor = if (enabled) resolvedInkColor.copy(alpha = InkTokens.fillLight)
        else Color.Transparent
    val contentColor = if (enabled) resolvedInkColor
        else colorScheme.onSurface.copy(alpha = InkTokens.disabledContent)

    Box(
        modifier = modifier
            .drawBehind {
                val cr = minOf(size.width, size.height) / 2f

                drawRoundRect(
                    color = fillColor,
                    cornerRadius = CornerRadius(cr, cr)
                )

                val path = buildWobbleBorderPath(
                    width = size.width,
                    height = size.height,
                    cornerRadius = cr,
                    wobbleAmplitude = wobblePx,
                    wobbleSeed = wobbleSeed,
                    segments = 3
                )

                // Bleed layer
                drawPath(
                    path = path,
                    color = borderColor.copy(alpha = InkTokens.fillBleed),
                    style = Stroke(
                        width = strokePx * 2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Primary border
                drawPath(
                    path = path,
                    color = borderColor,
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
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

/**
 * Drop-in replacement for [TextButton] with a subtle wobbly ink underline
 * in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [TextButton].
 */
@Composable
fun ThemedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
    val strokePx = with(density) { InkTokens.strokeFine.toPx() }

    val contentColor = if (enabled) colorScheme.primary
        else colorScheme.onSurface.copy(alpha = InkTokens.disabledContent)

    Box(
        modifier = modifier
            .drawBehind {
                val y = size.height - 2.dp.toPx()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, y)
                    val segments = 4
                    val segW = size.width / segments
                    for (i in 1..segments) {
                        val endX = segW * i
                        val endY = y + wobbleEnd(i, wobbleSeed, 0f, strokePx * 1.5f)
                        val ctrlX = segW * (i - 0.5f)
                        val ctrlY = y + wobbleCtrl(i, wobbleSeed, 0f, strokePx * 2f)
                        quadraticBezierTo(ctrlX, ctrlY, endX, endY)
                    }
                }
                drawPath(
                    path = path,
                    color = contentColor.copy(alpha = InkTokens.borderMedium),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}
