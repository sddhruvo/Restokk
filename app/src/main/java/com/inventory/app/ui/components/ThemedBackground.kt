package com.inventory.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.BackgroundStyle
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.visuals
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders a visual-style-aware background behind all screen content.
 *
 * - [BackgroundStyle.Flat]: no-op, pure MaterialTheme surface
 * - [BackgroundStyle.Textured]: multi-layer handmade paper — stochastic grain,
 *   visible fibers, edge vignette, breathing alpha, floating ink motes
 * - [BackgroundStyle.RuledLines]: horizontal notebook lines
 *
 * Insert once at the nav/Scaffold level — all screens inherit automatically.
 */
@Composable
fun ThemedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundStyle = MaterialTheme.visuals.backgroundStyle

    when (backgroundStyle) {
        is BackgroundStyle.Flat -> {
            Box(modifier = modifier.fillMaxSize(), content = content)
        }

        is BackgroundStyle.Textured -> {
            val reduceMotion = LocalReduceMotion.current
            val baseColor = MaterialTheme.colorScheme.onSurface

            // --- Breathing paper: grain alpha oscillates 0.10 ↔ 0.18 ---
            val breathingAlpha = if (!reduceMotion) {
                val transition = rememberInfiniteTransition(label = "paper-breathe")
                val alpha by transition.animateFloat(
                    initialValue = 0.10f,
                    targetValue = 0.18f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "grain-alpha"
                )
                alpha
            } else {
                0.14f
            }

            // --- Ink motes: time driver for Lissajous paths ---
            val moteTime = if (!reduceMotion) {
                val transition = rememberInfiniteTransition(label = "ink-motes")
                val time by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 120f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(120_000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "mote-time"
                )
                time
            } else {
                -1f
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Layer 1: Edge vignette
                        drawVignette(baseColor)

                        // Layer 2: Stochastic paper grain
                        drawPaperGrain(baseColor, breathingAlpha)

                        // Layer 3: Visible paper fibers
                        drawPaperFibers(baseColor, breathingAlpha)

                        // Layer 4: Ink motes
                        if (moteTime >= 0f) {
                            drawInkMotes(baseColor, moteTime)
                        }
                    },
                content = content
            )
        }

        is BackgroundStyle.RuledLines -> {
            val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = backgroundStyle.alpha)
            val spacingPx = with(LocalDensity.current) { backgroundStyle.spacing.toPx() }
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .drawBehind {
                        var y = spacingPx
                        while (y < size.height) {
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += spacingPx
                        }
                    },
                content = content
            )
        }
    }
}

// ─── Deterministic Pseudo-Random ────────────────────────────────────────────

/** Stable hash — same (x, y, seed) always returns same 0..1 value. */
private fun stableRandom(x: Int, y: Int, seed: Int = 0): Float {
    var h = x * 374761393 + y * 668265263 + seed * 1274126177
    h = (h xor (h ushr 13)) * 1103515245
    h = h xor (h ushr 16)
    return (h and 0x7FFFFFFF) / 2147483647f
}

// ─── Layer 1: Edge Vignette ─────────────────────────────────────────────────

/** Darkening at screen edges — aged, worn paper feel. */
private fun DrawScope.drawVignette(baseColor: Color) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                baseColor.copy(alpha = 0.06f)
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = max(size.width, size.height) * 0.6f
        )
    )
}

// ─── Layer 2: Stochastic Paper Grain ────────────────────────────────────────

/**
 * Stochastic grain — multiple dots per cell at FULLY random positions.
 * No visible grid pattern. Dots vary in size and alpha for organic feel.
 * Smaller dots, higher density = fine paper grain that blends together.
 */
private fun DrawScope.drawPaperGrain(baseColor: Color, breathingAlpha: Float) {
    val cellPx = 12.dp.toPx()
    val dotsPerCell = 2

    var cx = 0f
    while (cx < size.width) {
        val cxi = cx.toInt()
        var cy = 0f
        while (cy < size.height) {
            val cyi = cy.toInt()

            for (i in 0 until dotsPerCell) {
                val s = i * 100
                // Fully random position within cell — no grid visible
                val x = cx + stableRandom(cxi, cyi, s) * cellPx
                val y = cy + stableRandom(cxi, cyi, s + 1) * cellPx

                val hSize = stableRandom(cxi, cyi, s + 2)
                val hAlpha = stableRandom(cxi, cyi, s + 3)

                // Fine grain: 0.3 to 0.8dp radius
                val radiusPx = (0.3f + hSize * 0.5f).dp.toPx()

                // Alpha variation: 0.6x to 1.3x of breathing alpha
                val alpha = breathingAlpha * (0.6f + hAlpha * 0.7f)

                drawCircle(
                    color = baseColor.copy(alpha = min(alpha, 1f)),
                    radius = radiusPx,
                    center = Offset(x, y)
                )
            }
            cy += cellPx
        }
        cx += cellPx
    }
}

// ─── Layer 3: Paper Fibers ──────────────────────────────────────────────────

/**
 * Visible short line segments at random angles — paper fiber character.
 * Higher alpha than grain so they read as distinct marks.
 * Stochastic placement within cells, ~60% fill rate.
 */
private fun DrawScope.drawPaperFibers(baseColor: Color, breathingAlpha: Float) {
    val cellPx = 24.dp.toPx()

    var cx = 0f
    while (cx < size.width) {
        val cxi = cx.toInt()
        var cy = 0f
        while (cy < size.height) {
            val cyi = cy.toInt()

            val hPresence = stableRandom(cxi, cyi, 50)

            // ~60% of cells get a fiber
            if (hPresence > 0.4f) {
                val hX = stableRandom(cxi, cyi, 51)
                val hY = stableRandom(cxi, cyi, 52)
                val hAngle = stableRandom(cxi, cyi, 53)
                val hLen = stableRandom(cxi, cyi, 54)
                val hAlpha = stableRandom(cxi, cyi, 55)
                val hStroke = stableRandom(cxi, cyi, 56)

                // Random position within cell
                val x = cx + hX * cellPx
                val y = cy + hY * cellPx

                val angle = hAngle * 3.14159f
                val halfLenPx = (3f + hLen * 6f).dp.toPx()  // 6–18dp total

                val dx = cos(angle) * halfLenPx
                val dy = sin(angle) * halfLenPx

                // Higher alpha: 1.0x breathing * (0.6–1.0) — clearly visible
                val fiberAlpha = breathingAlpha * 1.0f * (0.6f + hAlpha * 0.4f)

                // Stroke: 0.5 to 1.2dp
                val strokePx = (0.5f + hStroke * 0.7f).dp.toPx()

                drawLine(
                    color = baseColor.copy(alpha = min(fiberAlpha, 1f)),
                    start = Offset(x - dx, y - dy),
                    end = Offset(x + dx, y + dy),
                    strokeWidth = strokePx
                )
            }
            cy += cellPx
        }
        cx += cellPx
    }
}

// ─── Layer 4: Ink Motes ─────────────────────────────────────────────────────

private data class MoteConfig(
    val freqX1: Float, val freqX2: Float,
    val freqY1: Float, val freqY2: Float,
    val phaseX: Float, val phaseY: Float,
    val radiusDp: Float,
    val alpha: Float
)

private val inkMotes = listOf(
    MoteConfig(1f / 20, 1f / 60, 1f / 30, 1f / 40, 0.0f, 0.0f, 1.5f, 0.10f),
    MoteConfig(1f / 40, 1f / 30, 1f / 40, 1f / 24, 1.7f, 2.3f, 1.2f, 0.08f),
    MoteConfig(1f / 60, 1f / 40, 1f / 20, 1f / 60, 3.1f, 0.7f, 2.0f, 0.11f),
    MoteConfig(1f / 60, 1f / 20, 1f / 60, 1f / 30, 4.3f, 1.1f, 1.3f, 0.09f),
    MoteConfig(1f / 30, 1f / 24, 1f / 24, 1f / 40, 2.1f, 3.7f, 1.7f, 0.10f),
)

private const val TWO_PI = 2f * 3.14159265f

private fun DrawScope.drawInkMotes(baseColor: Color, time: Float) {
    val edgeMargin = 60f

    for (mote in inkMotes) {
        val nx = sin(time * mote.freqX1 * TWO_PI + mote.phaseX) * 0.3f +
                cos(time * mote.freqX2 * TWO_PI + mote.phaseX * 1.7f) * 0.15f + 0.5f
        val ny = sin(time * mote.freqY1 * TWO_PI + mote.phaseY) * 0.3f +
                cos(time * mote.freqY2 * TWO_PI + mote.phaseY * 1.3f) * 0.15f + 0.5f

        val x = nx * size.width
        val y = ny * size.height

        val fadeX = (minOf(x, size.width - x) / edgeMargin).coerceIn(0f, 1f)
        val fadeY = (minOf(y, size.height - y) / edgeMargin).coerceIn(0f, 1f)
        val edgeFade = fadeX * fadeY

        if (edgeFade > 0.01f) {
            drawCircle(
                color = baseColor.copy(alpha = mote.alpha * edgeFade),
                radius = mote.radiusDp.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
