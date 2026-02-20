package com.inventory.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class InkDroplet(
    val angle: Float,
    val distance: Float,
    val radius: Float
)

/**
 * Draws an organic ink-bleed strikethrough across the composable.
 * Two layers: a wider bleed (low alpha) and a narrower core stroke.
 * Ink droplets flick off the endpoint in the final 15% of the animation.
 */
@Composable
fun InkStrikethrough(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Pre-compute wobble seed and droplets so they stay stable across recompositions.
    // These are keyed to the composable instance — each InkStrikethrough gets its own unique wobble.
    // If this composable is reused with different items (e.g., via key()), the seed will reset correctly.
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
    val droplets = remember {
        listOf(
            InkDroplet(angle = 25f, distance = 12f, radius = 2.5f),
            InkDroplet(angle = -35f, distance = 18f, radius = 1.8f),
            InkDroplet(angle = 10f, distance = 9f, radius = 2.0f)
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        // Build a wobbly path using quadratic bezier segments
        val path = Path().apply {
            val segments = 6
            val segWidth = w / segments
            moveTo(0f, centerY)
            for (i in 1..segments) {
                val endX = segWidth * i
                val endY = centerY + sin((i + wobbleSeed) * 1.3).toFloat() * (h * 0.06f)
                val ctrlX = segWidth * (i - 0.5f)
                val ctrlY = centerY + sin((i + wobbleSeed) * 2.1 + PI / 3).toFloat() * (h * 0.1f)
                quadraticBezierTo(ctrlX, ctrlY, endX, endY)
            }
        }

        val clipRight = w * progress

        // Layer 1: Bleed (wider, low alpha)
        clipRect(right = clipRight) {
            drawPath(
                path = path,
                color = color.copy(alpha = 0.25f),
                style = Stroke(
                    width = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Layer 2: Core stroke (narrower, high alpha)
        clipRect(right = clipRight) {
            drawPath(
                path = path,
                color = color.copy(alpha = 0.85f),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Ink droplets — visible when progress > 0.85, fly outward from stroke endpoint
        if (progress > 0.85f) {
            val dropletProgress = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
            val endX = clipRight
            val endY = centerY + sin((6 + wobbleSeed) * 1.3).toFloat() * (h * 0.06f)

            droplets.forEach { droplet ->
                val radians = droplet.angle * (PI / 180f).toFloat()
                val dist = droplet.distance.dp.toPx() * dropletProgress
                val dx = cos(radians) * dist
                val dy = sin(radians) * dist
                val alpha = (1f - dropletProgress * 0.6f) * 0.85f

                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = droplet.radius.dp.toPx(),
                    center = Offset(endX + dx, endY + dy)
                )
            }
        }
    }
}
