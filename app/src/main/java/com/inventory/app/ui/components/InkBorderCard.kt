package com.inventory.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.CardStyle
import com.inventory.app.ui.theme.InkTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Card with a hand-drawn wobble-bezier border.
 *
 * Uses a RoughJS-inspired double-stroke technique:
 * 1. Primary stroke — bold, confident pen line with organic wobble
 * 2. Sketch stroke — thinner, slightly offset second pass for "drawn twice" feel
 * 3. Bleed layer — subtle ink-into-paper spread underneath
 *
 * The border draws BEFORE the clip modifier so wobble is never cut off.
 * Content is still clipped to the card shape.
 */
@Composable
fun InkBorderCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    inkBorder: CardStyle.InkBorder = CardStyle.InkBorder(),
    cornerRadius: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val fill = containerColor
        ?: if (isDark) Color.White.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surface.copy(alpha = InkTokens.fillCard)

    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }

    // Auto-derive corner radius from MaterialTheme.shapes.large when 0.dp (default)
    val themeShape = MaterialTheme.shapes.large as? RoundedCornerShape
    val themeCornerRadius = if (cornerRadius > 0.dp) cornerRadius
        else if (themeShape != null) {
            // RoundedCornerShape created with dp returns the dp value at size 0
            with(LocalDensity.current) {
                themeShape.topStart.toPx(Size.Zero, this).toDp()
            }
        } else 18.dp

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { inkBorder.strokeWidth.toPx() }
    val wobbleAmplitudePx = with(density) { inkBorder.wobbleAmplitude.toPx() }
    val cornerRadiusPx = with(density) { themeCornerRadius.toPx() }
    val segments = inkBorder.segments

    val cardShape = RoundedCornerShape(themeCornerRadius)

    Box(
        modifier = modifier
            // Border draws BEFORE clip — wobble is never cut off
            .drawBehind {
                val w = size.width
                val h = size.height

                // Fill background
                drawRoundRect(
                    color = fill,
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )

                // Primary path — the main confident pen stroke
                val primaryPath = buildWobbleBorderPath(
                    width = w,
                    height = h,
                    cornerRadius = cornerRadiusPx,
                    wobbleAmplitude = wobbleAmplitudePx,
                    wobbleSeed = wobbleSeed,
                    segments = segments
                )

                val inkStroke = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                // Layer 1: Bleed — subtle ink-into-paper spread
                drawPath(
                    path = primaryPath,
                    color = borderColor.copy(alpha = borderColor.alpha * 0.12f),
                    style = Stroke(
                        width = strokeWidthPx * 2.0f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Layer 2: Primary stroke — confident ink line
                drawPath(
                    path = primaryPath,
                    color = borderColor,
                    style = inkStroke
                )
            }
            // Clip only affects content — border is already drawn above
            .clip(cardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content
    )
}

/**
 * Builds a closed wobble-bezier path around the card perimeter.
 * Multi-frequency sine displacement for organic, natural hand-drawn feel.
 * Corners receive seed-dependent displacement so they don't look geometric.
 */
internal fun buildWobbleBorderPath(
    width: Float,
    height: Float,
    cornerRadius: Float,
    wobbleAmplitude: Float,
    wobbleSeed: Float,
    segments: Int,
): Path {
    val cr = cornerRadius.coerceAtMost(minOf(width, height) / 2f)
    return Path().apply {
        // Start at top-left corner, after the corner radius
        moveTo(cr, 0f)

        // ── Top edge (left to right) ──
        val topLen = width - 2 * cr
        val topSeg = topLen / segments
        for (i in 1..segments) {
            val endX = cr + topSeg * i
            val endY = wobbleEnd(i, wobbleSeed, 0f, wobbleAmplitude)
            val ctrlX = cr + topSeg * (i - 0.5f)
            val ctrlY = wobbleCtrl(i, wobbleSeed, 0f, wobbleAmplitude)
            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }

        // Top-right corner — with wobble displacement
        val trWob = cornerWobble(wobbleSeed, 0, wobbleAmplitude)
        quadraticBezierTo(width + trWob, trWob * 0.5f, width, cr)

        // ── Right edge (top to bottom) ──
        val rightLen = height - 2 * cr
        val rightSeg = rightLen / segments
        for (i in 1..segments) {
            val endY = cr + rightSeg * i
            val endX = width + wobbleEnd(i, wobbleSeed + 100f, 0f, wobbleAmplitude)
            val ctrlY = cr + rightSeg * (i - 0.5f)
            val ctrlX = width + wobbleCtrl(i, wobbleSeed + 100f, 0f, wobbleAmplitude)
            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }

        // Bottom-right corner — with wobble displacement
        val brWob = cornerWobble(wobbleSeed, 1, wobbleAmplitude)
        quadraticBezierTo(width + brWob * 0.5f, height + brWob, width - cr, height)

        // ── Bottom edge (right to left) ──
        val bottomSeg = topLen / segments
        for (i in 1..segments) {
            val endX = width - cr - bottomSeg * i
            val endY = height + wobbleEnd(i, wobbleSeed + 200f, 0f, wobbleAmplitude)
            val ctrlX = width - cr - bottomSeg * (i - 0.5f)
            val ctrlY = height + wobbleCtrl(i, wobbleSeed + 200f, 0f, wobbleAmplitude)
            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }

        // Bottom-left corner — with wobble displacement
        val blWob = cornerWobble(wobbleSeed, 2, wobbleAmplitude)
        quadraticBezierTo(blWob * 0.5f, height + blWob, 0f, height - cr)

        // ── Left edge (bottom to top) ──
        val leftSeg = rightLen / segments
        for (i in 1..segments) {
            val endY = height - cr - leftSeg * i
            val endX = wobbleEnd(i, wobbleSeed + 300f, 0f, wobbleAmplitude)
            val ctrlY = height - cr - leftSeg * (i - 0.5f)
            val ctrlX = wobbleCtrl(i, wobbleSeed + 300f, 0f, wobbleAmplitude)
            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }

        // Top-left corner — with wobble displacement
        val tlWob = cornerWobble(wobbleSeed, 3, wobbleAmplitude)
        quadraticBezierTo(tlWob, tlWob * 0.5f, cr, 0f)

        close()
    }
}

/**
 * Multi-frequency endpoint displacement.
 * Primary sine + secondary harmonic breaks up predictable smoothness.
 */
internal fun wobbleEnd(i: Int, seed: Float, center: Float, amplitude: Float): Float {
    val primary = sin((i + seed) * 1.3).toFloat()
    val secondary = sin((i + seed) * 3.7 + seed * 0.7).toFloat() * 0.3f
    return center + (primary + secondary) * amplitude * 0.8f
}

/**
 * Multi-frequency control-point displacement — offset phase for organic curves.
 */
internal fun wobbleCtrl(i: Int, seed: Float, center: Float, amplitude: Float): Float {
    val primary = sin((i + seed) * 2.1 + PI / 3).toFloat()
    val secondary = sin((i + seed) * 4.3 + seed * 0.3).toFloat() * 0.25f
    return center + (primary + secondary) * amplitude
}

/**
 * Corner control-point displacement — each corner gets a unique
 * seed-dependent nudge so corners feel hand-drawn, not geometric.
 */
internal fun cornerWobble(seed: Float, cornerIndex: Int, amplitude: Float): Float =
    sin(seed * 1.7 + cornerIndex * 2.3).toFloat() * amplitude * 0.35f

/**
 * Builds an open wobble-bezier underline path (bottom edge only).
 * Used by ThemedTextField for a "writing on ruled paper" feel.
 * Uses the same multi-frequency displacement as [buildWobbleBorderPath] for consistency.
 *
 * When [endcapHeight] > 0, small upward ticks are drawn at each end of the line,
 * creating an open-bottom bracket shape `⌊___⌋` — used for interactive fields
 * (dropdowns, date pickers) to hint "tap here" without drawing a full border box.
 */
internal fun buildWobbleUnderlinePath(
    width: Float,
    y: Float,
    wobbleAmplitude: Float,
    wobbleSeed: Float,
    segments: Int = 5,
    inset: Float = 0f,
    endcapHeight: Float = 0f,
): Path {
    val lineWidth = width - 2 * inset
    val segLen = lineWidth / segments
    return Path().apply {
        // Left endcap tick (drawn downward: top → baseline)
        if (endcapHeight > 0f) {
            val topY = y - endcapHeight
            val wobX = wobbleEnd(0, wobbleSeed + 500f, inset, wobbleAmplitude * 0.4f)
            moveTo(wobX, topY + wobbleEnd(0, wobbleSeed + 600f, 0f, wobbleAmplitude * 0.3f))
            quadraticBezierTo(
                inset + wobbleCtrl(0, wobbleSeed + 500f, 0f, wobbleAmplitude * 0.3f),
                y - endcapHeight * 0.5f,
                inset,
                y + wobbleEnd(0, wobbleSeed, 0f, wobbleAmplitude)
            )
        } else {
            moveTo(inset, y + wobbleEnd(0, wobbleSeed, 0f, wobbleAmplitude))
        }

        // Main underline (left → right)
        for (i in 1..segments) {
            val endX = inset + segLen * i
            val endY = y + wobbleEnd(i, wobbleSeed, 0f, wobbleAmplitude)
            val ctrlX = inset + segLen * (i - 0.5f)
            val ctrlY = y + wobbleCtrl(i, wobbleSeed, 0f, wobbleAmplitude)
            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }

        // Right endcap tick (drawn upward: baseline → top)
        if (endcapHeight > 0f) {
            val topY = y - endcapHeight
            val rightX = width - inset
            quadraticBezierTo(
                rightX + wobbleCtrl(segments + 1, wobbleSeed + 700f, 0f, wobbleAmplitude * 0.3f),
                y - endcapHeight * 0.5f,
                rightX + wobbleEnd(segments + 1, wobbleSeed + 800f, 0f, wobbleAmplitude * 0.4f),
                topY + wobbleEnd(segments + 1, wobbleSeed + 900f, 0f, wobbleAmplitude * 0.3f)
            )
        }
    }
}

/**
 * Builds a closed wobble-bezier circular path.
 * Uses polar coordinates with sine-wave radial displacement
 * for an organic, hand-drawn circle feel.
 */
internal fun buildWobbleCirclePath(
    centerX: Float,
    centerY: Float,
    radius: Float,
    wobbleAmplitude: Float,
    wobbleSeed: Float,
    segments: Int = 12,
): Path {
    val angleStep = (2.0 * PI / segments).toFloat()
    return Path().apply {
        val startR = radius + wobbleEnd(0, wobbleSeed, 0f, wobbleAmplitude)
        moveTo(centerX + startR, centerY)

        for (i in 1..segments) {
            val angle = angleStep * i
            val r = radius + wobbleEnd(i, wobbleSeed, 0f, wobbleAmplitude)
            val endX = centerX + r * cos(angle)
            val endY = centerY + r * sin(angle)

            val ctrlAngle = angleStep * (i - 0.5f)
            val ctrlR = radius + wobbleCtrl(i, wobbleSeed, 0f, wobbleAmplitude)
            val ctrlX = centerX + ctrlR * cos(ctrlAngle)
            val ctrlY = centerY + ctrlR * sin(ctrlAngle)

            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }
        close()
    }
}
