package com.inventory.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Ink droplet data for leading edge and completion bloom
// ---------------------------------------------------------------------------

private data class WashDroplet(
    val angle: Float,      // degrees from horizontal
    val distance: Float,   // dp ahead of the edge
    val radius: Float      // dp
)

// ---------------------------------------------------------------------------
// InkWashSwipeBackground — Paper & Ink swipe reveal
//
// Replaces the flat Box(background=color) in SwipeToDismissBox.
// Draws an organic ink wash that follows swipe progress with:
//   • Wobble bezier edge (not a straight line)
//   • Bleed + core two-layer fill (like InkStrikethrough)
//   • Leading-edge ink droplets
//   • Icon that lands with wobble spring
//   • Self-drawing checkmark / scratch mark
//   • Deepening alpha past commitment threshold
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkWashSwipeBackground(
    progress: Float,
    direction: SwipeToDismissBoxValue,
    modifier: Modifier = Modifier
) {
    // Unique wobble seed per composition — stays stable while this item is alive
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }

    // Leading-edge droplets — pre-computed, stable
    val leadingDroplets = remember {
        listOf(
            WashDroplet(angle = 15f, distance = 10f, radius = 2.2f),
            WashDroplet(angle = -20f, distance = 16f, radius = 1.6f),
            WashDroplet(angle = 5f, distance = 7f, radius = 1.8f)
        )
    }

    // Bloom droplets for completion flash
    val bloomDroplets = remember {
        listOf(
            WashDroplet(angle = 30f, distance = 18f, radius = 2.5f),
            WashDroplet(angle = -45f, distance = 22f, radius = 2.0f),
            WashDroplet(angle = 70f, distance = 14f, radius = 1.8f),
            WashDroplet(angle = -10f, distance = 20f, radius = 2.2f),
            WashDroplet(angle = 110f, distance = 16f, radius = 1.5f)
        )
    }

    val isPurchase = direction == SwipeToDismissBoxValue.StartToEnd
    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
    val isActive = isPurchase || isDelete

    // Base ink color
    val inkColor = if (isPurchase) Color(0xFF4CAF50) else Color(0xFFE53935)

    // Icon spring — appears at 25%, lands with wobble overshoot
    val iconTargetScale = if (isActive && progress > 0.20f) 1f else 0f
    val iconScale by animateFloatAsState(
        targetValue = iconTargetScale,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 220f),
        label = "iconScale"
    )

    // Icon alpha — quick fade in
    val iconTargetAlpha = if (isActive && progress > 0.18f) 1f else 0f
    val iconAlpha by animateFloatAsState(
        targetValue = iconTargetAlpha,
        animationSpec = spring(dampingRatio = 1f, stiffness = 300f),
        label = "iconAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (!isActive || progress <= 0.01f) return@Canvas

        val w = size.width
        val h = size.height

        // -------------------------------------------------------------------
        // 1. Ink wash fill with wobble edge
        // -------------------------------------------------------------------

        // How far the ink has spread (in px)
        val inkExtent = w * progress

        // Commitment deepening: alpha increases past 50%
        val coreAlpha = if (progress > 0.50f) {
            0.55f + (progress - 0.50f) * 0.40f  // 0.55 → 0.75
        } else {
            0.55f
        }
        val bleedAlpha = coreAlpha * 0.28f

        // Build wobble edge path — 5 bezier segments along the height
        // The edge sits at x = inkExtent (for purchase) or x = w - inkExtent (for delete)
        val segments = 5
        val segHeight = h / segments
        val wobbleAmplitude = 8.dp.toPx()  // how much the edge wobbles horizontally

        // --- BLEED LAYER (slightly wider, lower alpha) ---
        val bleedOffset = 6.dp.toPx()  // bleed extends past core

        val bleedPath = Path().apply {
            if (isPurchase) {
                // Fill from left edge to wobbly right edge
                moveTo(0f, 0f)
                lineTo(inkExtent + bleedOffset, 0f)
                // Wobble down the right edge
                for (i in 1..segments) {
                    val endY = segHeight * i
                    val midY = segHeight * (i - 0.5f)
                    val wobbleX = sin((i + wobbleSeed) * 1.7 + PI / 4).toFloat() * wobbleAmplitude
                    val ctrlWobbleX = sin((i + wobbleSeed) * 2.3).toFloat() * wobbleAmplitude * 0.7f
                    quadraticBezierTo(
                        inkExtent + bleedOffset + ctrlWobbleX, midY,
                        inkExtent + bleedOffset + wobbleX, endY
                    )
                }
                lineTo(0f, h)
                close()
            } else {
                // Fill from right edge to wobbly left edge (mirrored)
                moveTo(w, 0f)
                lineTo(w - inkExtent - bleedOffset, 0f)
                for (i in 1..segments) {
                    val endY = segHeight * i
                    val midY = segHeight * (i - 0.5f)
                    val wobbleX = sin((i + wobbleSeed) * 1.7 + PI / 4).toFloat() * wobbleAmplitude
                    val ctrlWobbleX = sin((i + wobbleSeed) * 2.3).toFloat() * wobbleAmplitude * 0.7f
                    quadraticBezierTo(
                        w - inkExtent - bleedOffset - ctrlWobbleX, midY,
                        w - inkExtent - bleedOffset - wobbleX, endY
                    )
                }
                lineTo(w, h)
                close()
            }
        }

        drawPath(
            path = bleedPath,
            color = inkColor.copy(alpha = bleedAlpha)
        )

        // --- CORE LAYER (tighter, higher alpha) ---
        val corePath = Path().apply {
            if (isPurchase) {
                moveTo(0f, 0f)
                lineTo(inkExtent, 0f)
                for (i in 1..segments) {
                    val endY = segHeight * i
                    val midY = segHeight * (i - 0.5f)
                    val wobbleX = sin((i + wobbleSeed) * 1.7 + PI / 4).toFloat() * wobbleAmplitude * 0.6f
                    val ctrlWobbleX = sin((i + wobbleSeed) * 2.3).toFloat() * wobbleAmplitude * 0.4f
                    quadraticBezierTo(
                        inkExtent + ctrlWobbleX, midY,
                        inkExtent + wobbleX, endY
                    )
                }
                lineTo(0f, h)
                close()
            } else {
                moveTo(w, 0f)
                lineTo(w - inkExtent, 0f)
                for (i in 1..segments) {
                    val endY = segHeight * i
                    val midY = segHeight * (i - 0.5f)
                    val wobbleX = sin((i + wobbleSeed) * 1.7 + PI / 4).toFloat() * wobbleAmplitude * 0.6f
                    val ctrlWobbleX = sin((i + wobbleSeed) * 2.3).toFloat() * wobbleAmplitude * 0.4f
                    quadraticBezierTo(
                        w - inkExtent - ctrlWobbleX, midY,
                        w - inkExtent - wobbleX, endY
                    )
                }
                lineTo(w, h)
                close()
            }
        }

        drawPath(
            path = corePath,
            color = inkColor.copy(alpha = coreAlpha)
        )

        // -------------------------------------------------------------------
        // 2. Leading-edge ink droplets
        // -------------------------------------------------------------------
        if (progress > 0.08f && progress < 0.92f) {
            val edgeX = if (isPurchase) inkExtent else w - inkExtent
            val edgeCenterY = h / 2f

            leadingDroplets.forEach { droplet ->
                val radians = droplet.angle * (PI / 180f).toFloat()
                val dirMultiplier = if (isPurchase) 1f else -1f
                val dx = cos(radians) * droplet.distance.dp.toPx() * dirMultiplier
                val dy = sin(radians) * droplet.distance.dp.toPx()

                // Alpha fades with distance from edge
                val distFraction = droplet.distance / 16f
                val dropletAlpha = (coreAlpha * 0.7f) * (1f - distFraction * 0.5f)

                drawCircle(
                    color = inkColor.copy(alpha = dropletAlpha),
                    radius = droplet.radius.dp.toPx(),
                    center = Offset(edgeX + dx, edgeCenterY + dy)
                )
            }
        }

        // -------------------------------------------------------------------
        // 3. Icon (cart or ×) — drawn via Canvas circles/lines
        // -------------------------------------------------------------------
        if (iconScale > 0.01f) {
            val iconSize = 22.dp.toPx() * iconScale
            val iconCx: Float
            val iconCy = h / 2f

            if (isPurchase) {
                iconCx = 24.dp.toPx()
            } else {
                iconCx = w - 24.dp.toPx()
            }

            if (isPurchase) {
                // Draw a simple cart outline
                val cartColor = Color.White.copy(alpha = iconAlpha)
                val strokeWidth = 2.dp.toPx() * iconScale
                val cartStroke = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                // Cart body — trapezoid shape
                val cartPath = Path().apply {
                    val left = iconCx - iconSize * 0.5f
                    val right = iconCx + iconSize * 0.5f
                    val top = iconCy - iconSize * 0.2f
                    val bottom = iconCy + iconSize * 0.3f
                    moveTo(left - iconSize * 0.15f, top - iconSize * 0.3f)
                    lineTo(left, top)
                    lineTo(left + iconSize * 0.08f, bottom)
                    lineTo(right - iconSize * 0.08f, bottom)
                    lineTo(right, top)
                }
                drawPath(cartPath, cartColor, style = cartStroke)

                // Cart handle
                drawLine(
                    cartColor,
                    start = Offset(iconCx - iconSize * 0.65f, iconCy - iconSize * 0.5f),
                    end = Offset(iconCx - iconSize * 0.35f, iconCy - iconSize * 0.5f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    cartColor,
                    start = Offset(iconCx - iconSize * 0.35f, iconCy - iconSize * 0.5f),
                    end = Offset(iconCx - iconSize * 0.5f, iconCy - iconSize * 0.2f),
                    strokeWidth = strokeWidth
                )

                // Wheels
                drawCircle(
                    cartColor,
                    radius = iconSize * 0.08f,
                    center = Offset(iconCx - iconSize * 0.28f, iconCy + iconSize * 0.45f)
                )
                drawCircle(
                    cartColor,
                    radius = iconSize * 0.08f,
                    center = Offset(iconCx + iconSize * 0.28f, iconCy + iconSize * 0.45f)
                )
            } else {
                // Delete: draw an × scratch mark
                val xColor = Color.White.copy(alpha = iconAlpha)
                val strokeWidth = 2.5.dp.toPx() * iconScale
                val arm = iconSize * 0.4f

                // Two crossed lines with slight wobble
                val wobble1 = sin(wobbleSeed * 1.3).toFloat() * 2.dp.toPx()
                val wobble2 = sin(wobbleSeed * 2.1).toFloat() * 2.dp.toPx()

                drawLine(
                    xColor,
                    start = Offset(iconCx - arm, iconCy - arm + wobble1),
                    end = Offset(iconCx + arm, iconCy + arm - wobble1),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    xColor,
                    start = Offset(iconCx + arm, iconCy - arm + wobble2),
                    end = Offset(iconCx - arm, iconCy + arm - wobble2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // -------------------------------------------------------------------
        // 4. Ink checkmark (purchase) / scratch (delete) — draws itself
        //    Appears at 40% progress, fully drawn by 75%
        // -------------------------------------------------------------------
        if (isPurchase && progress > 0.38f) {
            val checkProgress = ((progress - 0.38f) / 0.37f).coerceIn(0f, 1f)

            // Position: to the right of the cart icon
            val checkCx = 56.dp.toPx()
            val checkCy = h / 2f
            val checkScale = 14.dp.toPx()

            // Checkmark geometry — short leg down-right, long leg up-right
            val startX = checkCx - checkScale * 0.4f
            val startY = checkCy
            val midX = checkCx - checkScale * 0.05f
            val midY = checkCy + checkScale * 0.4f
            val endX = checkCx + checkScale * 0.5f
            val endY = checkCy - checkScale * 0.35f

            val checkPath = Path().apply {
                moveTo(startX, startY)

                // Short leg — wobble bezier
                val seg1Wobble = sin((1 + wobbleSeed) * 2.1 + PI / 3).toFloat() * checkScale * 0.06f
                quadraticBezierTo(
                    startX + (midX - startX) * 0.5f,
                    startY + (midY - startY) * 0.5f + seg1Wobble,
                    midX, midY
                )

                // Long leg — wobble bezier
                val seg2Wobble = sin((2 + wobbleSeed) * 1.7).toFloat() * checkScale * 0.05f
                quadraticBezierTo(
                    midX + (endX - midX) * 0.5f,
                    midY + (endY - midY) * 0.5f + seg2Wobble,
                    endX, endY
                )
            }

            // Clip to reveal progressively
            val clipRight = if (checkProgress < 0.4f) {
                // Drawing short leg
                val legP = checkProgress / 0.4f
                startX + (midX - startX) * legP
            } else {
                // Drawing long leg
                val legP = (checkProgress - 0.4f) / 0.6f
                midX + (endX - midX) * legP
            }

            // Bleed layer
            clipRect(right = clipRight) {
                drawPath(
                    path = checkPath,
                    color = Color.White.copy(alpha = 0.25f),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Core stroke
            clipRect(right = clipRight) {
                drawPath(
                    path = checkPath,
                    color = Color.White.copy(alpha = 0.85f * iconAlpha),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Delete scratch mark that draws itself
        if (isDelete && progress > 0.38f) {
            val scratchProgress = ((progress - 0.38f) / 0.37f).coerceIn(0f, 1f)

            // Positioned to the left of the × icon
            val scratchCx = w - 56.dp.toPx()
            val scratchCy = h / 2f
            val scratchLen = 18.dp.toPx()

            // A wobbly horizontal scratch line
            val scratchPath = Path().apply {
                val startX = scratchCx + scratchLen * 0.5f
                val endX = scratchCx - scratchLen * 0.5f
                moveTo(startX, scratchCy)

                val segs = 4
                val segW = (endX - startX) / segs
                for (i in 1..segs) {
                    val sx = startX + segW * i
                    val sy = scratchCy + sin((i + wobbleSeed) * 1.9).toFloat() * 3.dp.toPx()
                    val cx = startX + segW * (i - 0.5f)
                    val cy = scratchCy + sin((i + wobbleSeed) * 2.7 + PI / 3).toFloat() * 4.dp.toPx()
                    quadraticBezierTo(cx, cy, sx, sy)
                }
            }

            val clipLeft = if (isPurchase) {
                // not used in this branch, but for safety
                0f
            } else {
                // Reveal from right to left
                val totalWidth = scratchLen
                val revealedWidth = totalWidth * scratchProgress
                scratchCx + scratchLen * 0.5f - revealedWidth
            }

            clipRect(
                left = clipLeft,
                right = scratchCx + scratchLen * 0.5f + 4.dp.toPx()
            ) {
                // Bleed
                drawPath(
                    path = scratchPath,
                    color = Color.White.copy(alpha = 0.2f),
                    style = Stroke(
                        width = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                // Core
                drawPath(
                    path = scratchPath,
                    color = Color.White.copy(alpha = 0.8f * iconAlpha),
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // -------------------------------------------------------------------
        // 5. Completion bloom — droplets scatter when fully swiped
        // -------------------------------------------------------------------
        if (progress > 0.92f) {
            val bloomP = ((progress - 0.92f) / 0.08f).coerceIn(0f, 1f)
            val bloomCx = if (isPurchase) 48.dp.toPx() else w - 48.dp.toPx()
            val bloomCy = h / 2f

            bloomDroplets.forEach { droplet ->
                val radians = droplet.angle * (PI / 180f).toFloat()
                val dist = droplet.distance.dp.toPx() * bloomP
                val dx = cos(radians) * dist
                val dy = sin(radians) * dist
                val alpha = (1f - bloomP * 0.6f) * 0.7f

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = droplet.radius.dp.toPx() * (0.4f + bloomP * 0.6f),
                    center = Offset(bloomCx + dx, bloomCy + dy)
                )
            }
        }
    }
}
