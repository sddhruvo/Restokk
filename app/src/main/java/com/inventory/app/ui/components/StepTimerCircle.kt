package com.inventory.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.appColors
import com.inventory.app.worker.TimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Phase enum ────────────────────────────────────────────────────────────

private enum class TimerPhase { IDLE, RUNNING, PAUSED, URGENCY, COMPLETE }

private fun derivePhase(timerState: TimerState?, totalSeconds: Int): TimerPhase = when {
    timerState == null -> TimerPhase.IDLE
    totalSeconds <= 0 -> TimerPhase.IDLE
    !timerState.isRunning && timerState.remainingSeconds == timerState.totalSeconds -> TimerPhase.IDLE
    timerState.remainingSeconds == 0 && !timerState.isRunning -> TimerPhase.COMPLETE
    !timerState.isRunning && timerState.remainingSeconds > 0
        && timerState.remainingSeconds < timerState.totalSeconds -> TimerPhase.PAUSED
    timerState.isRunning && timerState.remainingSeconds <= 15 -> TimerPhase.URGENCY
    timerState.isRunning -> TimerPhase.RUNNING
    else -> TimerPhase.IDLE
}

// ─── Particle data ─────────────────────────────────────────────────────────

private data class BurnParticle(
    val angle: Float,           // radians — position on ring where spawned
    val angularSpread: Float,   // random scatter offset (radians)
    val speed: Float,           // dp per second outward
    val size: Float,            // blob radius in px
    val spawnTimeMs: Long,
    val lifetimeMs: Long,
    val wobbleSeed: Float,
)

private const val MAX_PARTICLES = 8
private const val PARTICLE_LIFETIME_MIN = 400L
private const val PARTICLE_LIFETIME_MAX = 700L

// ─── Wobble arc path builder ───────────────────────────────────────────────

/**
 * Builds an open wobble-bezier arc path (partial circle).
 * Segment indices are absolute so the wobble pattern is stable
 * regardless of how much arc is drawn.
 */
private fun buildWobbleArcPath(
    centerX: Float,
    centerY: Float,
    radius: Float,
    wobbleAmplitude: Float,
    wobbleSeed: Float,
    startAngleRad: Float,
    sweepAngleRad: Float,
    totalSegments: Int = 16,
): Path {
    if (sweepAngleRad <= 0f) return Path()

    val fullAngleStep = (2.0 * PI / totalSegments).toFloat()

    // Find the first and last segment indices that overlap our arc
    val startIdx = (startAngleRad / fullAngleStep).toInt()
    val endAngle = startAngleRad + sweepAngleRad
    val endIdx = ((endAngle) / fullAngleStep).toInt() + 1

    return Path().apply {
        // Move to the start of the arc
        val startR = radius + wobbleEnd(startIdx, wobbleSeed, 0f, wobbleAmplitude)
        moveTo(
            centerX + startR * cos(startAngleRad),
            centerY + startR * sin(startAngleRad)
        )

        for (i in startIdx until endIdx) {
            val segStart = fullAngleStep * i
            val segEnd = fullAngleStep * (i + 1)

            // Clamp segment to our arc range
            val clampedEnd = segEnd.coerceAtMost(endAngle)
            if (clampedEnd <= segStart.coerceAtLeast(startAngleRad)) continue

            val r = radius + wobbleEnd(i + 1, wobbleSeed, 0f, wobbleAmplitude)
            val endX = centerX + r * cos(clampedEnd)
            val endY = centerY + r * sin(clampedEnd)

            val ctrlAngle = (segStart.coerceAtLeast(startAngleRad) + clampedEnd) / 2f
            val ctrlR = radius + wobbleCtrl(i + 1, wobbleSeed, 0f, wobbleAmplitude)
            val ctrlX = centerX + ctrlR * cos(ctrlAngle)
            val ctrlY = centerY + ctrlR * sin(ctrlAngle)

            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }
    }
}

// ─── Format helper ─────────────────────────────────────────────────────────

private fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

// ─── Main composable ───────────────────────────────────────────────────────

/**
 * "Ink Burn" timer circle for cooking playback.
 *
 * A thick hand-drawn ink ring that burns away clockwise as time elapses.
 * Features: wobble paths, burn-edge particles, drip-on-pause, urgency pulse,
 * and a stamp-checkmark completion animation.
 *
 * Consumes pointer events so it doesn't trigger step swipe navigation beneath it.
 */
@Composable
fun StepTimerCircle(
    timerState: TimerState?,
    totalSeconds: Int,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (totalSeconds <= 0) return

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // ── Colors ──
    val inkColor = MaterialTheme.colorScheme.onSurface
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val cookAccent = MaterialTheme.appColors.cookAccent
    val urgencyAmber = Color(0xFFE8A33A)

    // ── Stable callbacks (pointerInput captures at creation, so keep them fresh) ──
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    // ── Derived state ──
    val phase = derivePhase(timerState, totalSeconds)
    val remaining = timerState?.remainingSeconds ?: totalSeconds
    val progress = if (totalSeconds > 0)
        (totalSeconds - remaining).toFloat() / totalSeconds
    else 0f

    // ── Stable wobble seed ──
    val wobbleSeed = remember { Random.nextFloat() * 1000f }

    // ── Pixel conversions ──
    val wobbleAmpPx = with(density) { InkTokens.wobbleMedium.toPx() }
    val timerSizeDp = 130.dp
    val radiusDp = 50.dp
    val radiusPx = with(density) { radiusDp.toPx() }
    val thinStrokePx = with(density) { 3.dp.toPx() }
    val thickStrokePx = with(density) { 10.dp.toPx() }
    val ghostStrokePx = with(density) { 2.dp.toPx() }
    val rippleStrokePx = with(density) { 1.5.dp.toPx() }

    // ── Animations ──

    // 1. Breathing / pulse
    val infiniteTransition = rememberInfiniteTransition(label = "inkBurnTimer")

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = when (phase) {
            TimerPhase.IDLE -> PaperInkMotion.BreatheScaleDefault
            TimerPhase.RUNNING -> 1.008f
            TimerPhase.URGENCY -> 1.02f
            else -> 1.0f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (phase) {
                    TimerPhase.IDLE -> PaperInkMotion.BreathePeriodDefault
                    TimerPhase.RUNNING -> 1000
                    TimerPhase.URGENCY -> PaperInkMotion.HeartBeatUrgentPeriod
                    else -> 2500
                },
                easing = if (phase == TimerPhase.URGENCY) EaseOutCubic else EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathePulse"
    )

    // 2. Burn edge jitter (drives jagged contour)
    val edgeJitter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (phase == TimerPhase.RUNNING || phase == TimerPhase.URGENCY) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edgeJitter"
    )

    // 3. Urgency ripple
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (phase == TimerPhase.URGENCY) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = PaperInkMotion.HeartBeatUrgentPeriod,
                easing = EaseOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    // ── Particle system ──
    val particles = remember { mutableStateListOf<BurnParticle>() }
    var lastTickRemaining by remember { mutableStateOf(totalSeconds) }

    // Spawn particles on each tick
    LaunchedEffect(timerState?.remainingSeconds) {
        val currentRemaining = timerState?.remainingSeconds ?: return@LaunchedEffect
        if (currentRemaining >= lastTickRemaining) {
            lastTickRemaining = currentRemaining
            return@LaunchedEffect
        }
        lastTickRemaining = currentRemaining

        val currentPhase = derivePhase(timerState, totalSeconds)
        if (currentPhase != TimerPhase.RUNNING && currentPhase != TimerPhase.URGENCY) return@LaunchedEffect

        val currentProgress = (totalSeconds - currentRemaining).toFloat() / totalSeconds
        // Burn angle: starts at -90° (top), progress clockwise
        val burnAngle = (-PI / 2 + currentProgress * 2 * PI).toFloat()
        val count = if (currentPhase == TimerPhase.URGENCY) 2 else 1
        val now = System.currentTimeMillis()

        repeat(count) {
            if (particles.size < MAX_PARTICLES) {
                particles.add(
                    BurnParticle(
                        angle = burnAngle,
                        angularSpread = (Random.nextFloat() - 0.5f) * (PI / 6).toFloat(),
                        speed = with(density) { (40 + Random.nextFloat() * 30).dp.toPx() },
                        size = with(density) { (2f + Random.nextFloat() * 1.5f).dp.toPx() },
                        spawnTimeMs = now,
                        lifetimeMs = PARTICLE_LIFETIME_MIN +
                            (Random.nextFloat() * (PARTICLE_LIFETIME_MAX - PARTICLE_LIFETIME_MIN)).toLong(),
                        wobbleSeed = Random.nextFloat() * 100f,
                    )
                )
            }
        }
    }

    // Particle frame loop — update + cleanup
    LaunchedEffect(phase) {
        if (phase != TimerPhase.RUNNING && phase != TimerPhase.URGENCY
            && phase != TimerPhase.COMPLETE
        ) {
            // Clear particles when idle/paused
            if (phase == TimerPhase.PAUSED || phase == TimerPhase.IDLE) {
                // Let existing particles finish their lifetime naturally
                while (particles.isNotEmpty() && isActive) {
                    val now = System.currentTimeMillis()
                    particles.removeAll { now - it.spawnTimeMs > it.lifetimeMs }
                    delay(16)
                }
            }
            return@LaunchedEffect
        }
        // Frame loop to keep Canvas invalidating while particles exist
        while (isActive) {
            val now = System.currentTimeMillis()
            particles.removeAll { now - it.spawnTimeMs > it.lifetimeMs }
            delay(16)
        }
    }

    // ── Pause drip ──
    val dripProgress = remember { Animatable(0f) }
    var dripPlayed by remember { mutableStateOf(false) }
    var dripBurnAngle by remember { mutableStateOf(0f) }

    LaunchedEffect(phase) {
        if (phase == TimerPhase.PAUSED && !dripPlayed) {
            dripBurnAngle = (-PI / 2 + progress * 2 * PI).toFloat()
            dripProgress.snapTo(0f)
            dripProgress.animateTo(1f, tween(600, easing = EaseOutCubic))
            dripPlayed = true
        } else if (phase != TimerPhase.PAUSED) {
            dripPlayed = false
            dripProgress.snapTo(0f)
        }
    }

    // ── Completion stamp ──
    val stampScale = remember { Animatable(0f) }
    val stampRotation = remember { Animatable(0f) }
    val stampAlpha = remember { Animatable(0f) }
    val doneTextAlpha = remember { Animatable(0f) }
    var hasPlayedCompletion by remember { mutableStateOf(false) }
    val completionBurstParticles = remember { mutableStateListOf<BurnParticle>() }

    LaunchedEffect(phase) {
        if (phase == TimerPhase.COMPLETE && !hasPlayedCompletion) {
            hasPlayedCompletion = true

            // 1. Particle burst at last burn position
            val burstAngle = (-PI / 2 + 2 * PI).toFloat() // full circle = back to top
            val now = System.currentTimeMillis()
            repeat(10) {
                completionBurstParticles.add(
                    BurnParticle(
                        angle = burstAngle,
                        angularSpread = (Random.nextFloat() - 0.5f) * (PI * 2).toFloat(),
                        speed = with(density) { (50 + Random.nextFloat() * 40).dp.toPx() },
                        size = with(density) { (2f + Random.nextFloat() * 2f).dp.toPx() },
                        spawnTimeMs = now,
                        lifetimeMs = 500L,
                        wobbleSeed = Random.nextFloat() * 100f,
                    )
                )
            }

            // 2. Wait for burst
            delay(350)

            // 3. Stamp materializes
            stampScale.snapTo(1.3f)
            stampRotation.snapTo(-12f)
            stampAlpha.snapTo(1f)

            // Animate scale with overshoot
            stampScale.animateTo(
                targetValue = 1f,
                animationSpec = PaperInkMotion.WobblySpring
            )
            // Rotation settles (run concurrently? No — sequential is fine for slight stagger)

            stampRotation.animateTo(
                targetValue = 0f,
                animationSpec = PaperInkMotion.BouncySpring
            )

            // 4. "Done!" text fades in
            delay(100)
            doneTextAlpha.animateTo(1f, tween(200))

        } else if (phase != TimerPhase.COMPLETE) {
            hasPlayedCompletion = false
            stampScale.snapTo(0f)
            stampRotation.snapTo(0f)
            stampAlpha.snapTo(0f)
            doneTextAlpha.snapTo(0f)
            completionBurstParticles.clear()
        }
    }

    // Completion burst particle cleanup
    LaunchedEffect(completionBurstParticles.size) {
        if (completionBurstParticles.isEmpty()) return@LaunchedEffect
        while (completionBurstParticles.isNotEmpty() && isActive) {
            val now = System.currentTimeMillis()
            completionBurstParticles.removeAll { now - it.spawnTimeMs > it.lifetimeMs }
            delay(16)
        }
    }

    // ── Render ──
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onLongPress = { currentOnLongPress?.invoke() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(timerSizeDp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            val scaleFactor = when (phase) {
                TimerPhase.IDLE, TimerPhase.RUNNING, TimerPhase.URGENCY -> breatheScale
                else -> 1f
            }

            scale(scaleFactor) {
                // ── Layer 0: Faint paper disc ──
                drawCircle(
                    color = surfaceVariant.copy(alpha = 0.08f),
                    radius = radiusPx + thickStrokePx / 2,
                    center = Offset(cx, cy)
                )

                when (phase) {
                    TimerPhase.IDLE -> drawIdle(
                        cx, cy, radiusPx, wobbleAmpPx, wobbleSeed,
                        thinStrokePx, inkColor, mutedInk, textMeasurer,
                        totalSeconds
                    )

                    TimerPhase.RUNNING, TimerPhase.URGENCY -> {
                        val arcInkColor = if (phase == TimerPhase.URGENCY) {
                            val urgencyFactor = ((15 - remaining) / 15f).coerceIn(0f, 1f)
                            lerp(inkColor.copy(alpha = 0.9f), urgencyAmber, urgencyFactor)
                        } else {
                            inkColor.copy(alpha = 0.9f)
                        }

                        drawRunning(
                            cx, cy, radiusPx, wobbleAmpPx, wobbleSeed,
                            thickStrokePx, ghostStrokePx, arcInkColor, inkColor,
                            progress, edgeJitter, textMeasurer,
                            remaining, timerState?.stepLabel,
                            if (phase == TimerPhase.URGENCY) urgencyAmber else inkColor,
                            urgencyAmber // ember color — always warm
                        )

                        // Urgency ripple ring
                        if (phase == TimerPhase.URGENCY && rippleProgress > 0f) {
                            val rippleRadius = radiusPx + thickStrokePx / 2 +
                                rippleProgress * with(density) { 15.dp.toPx() }
                            drawCircle(
                                color = urgencyAmber.copy(alpha = 0.25f * (1f - rippleProgress)),
                                radius = rippleRadius,
                                center = Offset(cx, cy),
                                style = Stroke(width = rippleStrokePx)
                            )
                        }
                    }

                    TimerPhase.PAUSED -> {
                        drawPaused(
                            cx, cy, radiusPx, wobbleAmpPx, wobbleSeed,
                            thickStrokePx, ghostStrokePx, inkColor, mutedInk,
                            progress, textMeasurer, remaining,
                            dripProgress.value, dripBurnAngle, density
                        )
                    }

                    TimerPhase.COMPLETE -> {
                        // Ghost ring (fully burned)
                        val ghostPath = buildWobbleCirclePath(
                            cx, cy, radiusPx, wobbleAmpPx * 0.5f, wobbleSeed, 16
                        )
                        drawPath(
                            path = ghostPath,
                            color = inkColor.copy(alpha = 0.08f),
                            style = Stroke(
                                width = ghostStrokePx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Stamp
                        if (stampAlpha.value > 0f) {
                            drawStamp(
                                cx, cy, radiusPx, wobbleAmpPx, wobbleSeed,
                                cookAccent, stampScale.value,
                                stampRotation.value, stampAlpha.value,
                                doneTextAlpha.value, textMeasurer
                            )
                        }
                    }
                }

                // ── Draw ember sparks (directional streaks, not circles) ──
                val now = System.currentTimeMillis()
                val allParticles = particles + completionBurstParticles
                for (p in allParticles) {
                    val age = (now - p.spawnTimeMs).toFloat()
                    val lifeFraction = (age / p.lifetimeMs).coerceIn(0f, 1f)
                    if (lifeFraction >= 1f) continue

                    val dist = p.speed * (age / 1000f)
                    val particleAngle = p.angle + p.angularSpread
                    val px = cx + (radiusPx + dist) * cos(particleAngle)
                    val py = cy + (radiusPx + dist) * sin(particleAngle)
                    val alpha = (1f - lifeFraction) * 0.7f
                    val particleColor = urgencyAmber.copy(alpha = alpha)

                    // Streak along travel direction (looks like flying spark)
                    val streakLen = p.size * 2f * (1f - lifeFraction * 0.3f)
                    val tailX = px - streakLen * cos(particleAngle)
                    val tailY = py - streakLen * sin(particleAngle)
                    drawLine(
                        color = particleColor,
                        start = Offset(tailX, tailY),
                        end = Offset(px, py),
                        strokeWidth = p.size * 0.6f * (1f - lifeFraction * 0.5f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// ─── Draw helpers ──────────────────────────────────────────────────────────

private fun DrawScope.drawIdle(
    cx: Float, cy: Float, radius: Float,
    wobbleAmp: Float, wobbleSeed: Float,
    strokeWidth: Float,
    inkColor: Color, mutedInk: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    totalSeconds: Int,
) {
    val circlePath = buildWobbleCirclePath(
        cx, cy, radius, wobbleAmp, wobbleSeed, 16
    )

    // Bleed layer
    drawPath(
        path = circlePath,
        color = inkColor.copy(alpha = inkColor.alpha * InkTokens.fillBleed),
        style = Stroke(
            width = strokeWidth * 2f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Core stroke
    drawPath(
        path = circlePath,
        color = inkColor.copy(alpha = 0.35f),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Timer icon
    val iconResult = textMeasurer.measure(
        text = "⏱",
        style = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center)
    )
    drawText(
        textLayoutResult = iconResult,
        color = mutedInk,
        topLeft = Offset(
            cx - iconResult.size.width / 2f,
            cy - iconResult.size.height / 2f - 8f
        )
    )

    // Total time hint
    val timeResult = textMeasurer.measure(
        text = formatTimer(totalSeconds),
        style = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center)
    )
    drawText(
        textLayoutResult = timeResult,
        color = mutedInk.copy(alpha = 0.7f),
        topLeft = Offset(
            cx - timeResult.size.width / 2f,
            cy + iconResult.size.height / 2f - 6f
        )
    )

    // Hint: "Tap to start"
    val hintResult = textMeasurer.measure(
        text = "Tap to start",
        style = TextStyle(fontSize = 9.sp, textAlign = TextAlign.Center)
    )
    drawText(
        textLayoutResult = hintResult,
        color = mutedInk.copy(alpha = 0.4f),
        topLeft = Offset(
            cx - hintResult.size.width / 2f,
            cy + radius * 0.85f
        )
    )
}

private fun DrawScope.drawRunning(
    cx: Float, cy: Float, radius: Float,
    wobbleAmp: Float, wobbleSeed: Float,
    thickStroke: Float, ghostStroke: Float,
    arcColor: Color, inkColor: Color,
    progress: Float, edgeJitter: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    remaining: Int, stepLabel: String?,
    textColor: Color, emberColor: Color,
) {
    // Start angle: -90° (top of circle) = -PI/2
    val startOffset = (-PI / 2).toFloat()
    val burnAngle = startOffset + progress * (2 * PI).toFloat()
    val remainingSweep = ((1f - progress) * 2 * PI).toFloat()

    // Ghost trail (burned/scorched area) — warm tint like charred paper
    if (progress > 0.01f) {
        val ghostPath = buildWobbleArcPath(
            cx, cy, radius, wobbleAmp * 0.5f, wobbleSeed,
            startOffset, progress * (2 * PI).toFloat(), 16
        )
        drawPath(
            path = ghostPath,
            color = emberColor.copy(alpha = 0.08f),
            style = Stroke(width = ghostStroke * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Remaining arc (unburned ink)
    if (remainingSweep > 0.02f) {
        // Main arc — solid dark ink (the unburned portion)
        val remainingPath = buildWobbleArcPath(
            cx, cy, radius, wobbleAmp, wobbleSeed,
            burnAngle, remainingSweep, 16
        )
        // Bleed layer
        drawPath(
            path = remainingPath,
            color = arcColor.copy(alpha = arcColor.alpha * InkTokens.fillBleed),
            style = Stroke(width = thickStroke * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Core stroke
        drawPath(
            path = remainingPath,
            color = arcColor,
            style = Stroke(width = thickStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Heated zone — last ~20° of arc transitions ink → amber (heating before burn)
        val heatZoneRad = (20f * PI / 180f).toFloat() // 20 degrees in radians
        if (remainingSweep > heatZoneRad * 0.5f) {
            val heatStart = burnAngle
            val heatSweep = heatZoneRad.coerceAtMost(remainingSweep)
            val heatPath = buildWobbleArcPath(
                cx, cy, radius, wobbleAmp, wobbleSeed,
                heatStart, heatSweep, 16
            )
            // Overlay warm tint on the heated zone (blends with dark arc underneath)
            drawPath(
                path = heatPath,
                color = emberColor.copy(alpha = 0.35f),
                style = Stroke(width = thickStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // ── Burn edge — radiating spark lines ──
        val edgeAngle = burnAngle
        for (i in 0 until 5) {
            // Each spark at slightly different angle, flickering with edgeJitter
            val sparkOffset = (i - 2f) * 0.12f + sin((edgeJitter + i) * 4.7 + wobbleSeed).toFloat() * 0.08f
            val sparkAngle = edgeAngle + sparkOffset
            // Spark length varies per-spark (deterministic from seed)
            val sparkLen = thickStroke * (0.8f + sin(wobbleSeed + i * 3.1).toFloat() * 0.6f)
            val sparkWidth = thickStroke * (0.12f + sin(wobbleSeed + i * 1.7).toFloat() * 0.04f)
            // Start from ring edge, extend outward
            val sparkStartR = radius - thickStroke * 0.2f
            val sparkEndR = radius + sparkLen
            val sx = cx + sparkStartR * cos(sparkAngle)
            val sy = cy + sparkStartR * sin(sparkAngle)
            val ex = cx + sparkEndR * cos(sparkAngle)
            val ey = cy + sparkEndR * sin(sparkAngle)
            // Closer sparks are brighter, outer ones fade
            val sparkAlpha = 0.5f - i * 0.07f + edgeJitter * 0.1f
            drawLine(
                color = emberColor.copy(alpha = sparkAlpha.coerceIn(0.1f, 0.6f)),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = sparkWidth,
                cap = StrokeCap.Round
            )
        }
    }

    // Center text: countdown
    val countdownResult = textMeasurer.measure(
        text = formatTimer(remaining),
        style = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
    drawText(
        textLayoutResult = countdownResult,
        color = textColor,
        topLeft = Offset(
            cx - countdownResult.size.width / 2f,
            cy - countdownResult.size.height / 2f - if (stepLabel != null) 6f else 0f
        )
    )

    // Step label (if available, truncated)
    if (stepLabel != null) {
        val labelText = if (stepLabel.length > 12) stepLabel.take(11) + "…" else stepLabel
        val labelResult = textMeasurer.measure(
            text = labelText,
            style = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center)
        )
        drawText(
            textLayoutResult = labelResult,
            color = textColor.copy(alpha = 0.6f),
            topLeft = Offset(
                cx - labelResult.size.width / 2f,
                cy + countdownResult.size.height / 2f - 4f
            )
        )
    }
}

private fun DrawScope.drawPaused(
    cx: Float, cy: Float, radius: Float,
    wobbleAmp: Float, wobbleSeed: Float,
    thickStroke: Float, ghostStroke: Float,
    inkColor: Color, mutedInk: Color,
    progress: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    remaining: Int,
    dripFraction: Float, dripAngle: Float,
    density: androidx.compose.ui.unit.Density,
) {
    val startOffset = (-PI / 2).toFloat()
    val burnAngle = startOffset + progress * (2 * PI).toFloat()
    val remainingSweep = ((1f - progress) * 2 * PI).toFloat()

    // Ghost trail
    if (progress > 0.01f) {
        val ghostPath = buildWobbleArcPath(
            cx, cy, radius, wobbleAmp * 0.5f, wobbleSeed,
            startOffset, progress * (2 * PI).toFloat(), 16
        )
        drawPath(
            path = ghostPath,
            color = inkColor.copy(alpha = 0.05f),
            style = Stroke(width = ghostStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Remaining arc — "dried" look (lower alpha)
    if (remainingSweep > 0.02f) {
        val remainingPath = buildWobbleArcPath(
            cx, cy, radius, wobbleAmp, wobbleSeed,
            burnAngle, remainingSweep, 16
        )
        drawPath(
            path = remainingPath,
            color = inkColor.copy(alpha = 0.65f), // dried — less vivid than running
            style = Stroke(width = thickStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Drip animation
    if (dripFraction > 0f) {
        val dripStartX = cx + radius * cos(dripAngle)
        val dripStartY = cy + radius * sin(dripAngle)
        val gravity = with(density) { 25.dp.toPx() }
        val dripY = dripStartY + dripFraction * dripFraction * gravity
        val dripX = dripStartX // falls straight down-ish
        val scaleY = 1f + dripFraction * 0.6f // stretches as it falls
        val dripSize = thickStroke * 0.35f

        // Main drip blob
        drawOval(
            color = inkColor.copy(alpha = 0.6f * (1f - dripFraction * 0.3f)),
            topLeft = Offset(dripX - dripSize, dripY - dripSize * scaleY),
            size = androidx.compose.ui.geometry.Size(dripSize * 2, dripSize * 2 * scaleY)
        )

        // Splat dots at landing
        if (dripFraction > 0.85f) {
            val splatAlpha = ((dripFraction - 0.85f) / 0.15f) * 0.4f
            for (i in 0..2) {
                val splatAngle = (i * 2.1f + wobbleSeed).toFloat()
                val splatDist = dripSize * 2.5f
                drawCircle(
                    color = inkColor.copy(alpha = splatAlpha),
                    radius = dripSize * 0.4f,
                    center = Offset(
                        dripX + cos(splatAngle) * splatDist,
                        dripY + dripSize * scaleY + sin(splatAngle).coerceAtLeast(0f) * splatDist * 0.5f
                    )
                )
            }
        }
    }

    // Pause icon — two hand-drawn ink bars
    val barWidth = thickStroke * 0.25f
    val barHeight = thickStroke * 1.4f
    val barGap = thickStroke * 0.35f
    val barY = cy - barHeight - 6f
    val barStroke = Stroke(width = barWidth, cap = StrokeCap.Round)
    // Left bar
    drawLine(
        color = mutedInk.copy(alpha = 0.7f),
        start = Offset(cx - barGap, barY),
        end = Offset(cx - barGap, barY + barHeight),
        strokeWidth = barWidth
    )
    // Right bar
    drawLine(
        color = mutedInk.copy(alpha = 0.7f),
        start = Offset(cx + barGap, barY),
        end = Offset(cx + barGap, barY + barHeight),
        strokeWidth = barWidth
    )

    // Countdown below pause bars
    val timeResult = textMeasurer.measure(
        text = formatTimer(remaining),
        style = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
    drawText(
        textLayoutResult = timeResult,
        color = mutedInk,
        topLeft = Offset(cx - timeResult.size.width / 2f, cy + 2f)
    )

    // Hints: "Tap to resume · Hold to reset"
    val hintResult = textMeasurer.measure(
        text = "Tap ▶  ·  Hold ↺",
        style = TextStyle(fontSize = 9.sp, textAlign = TextAlign.Center)
    )
    drawText(
        textLayoutResult = hintResult,
        color = mutedInk.copy(alpha = 0.4f),
        topLeft = Offset(
            cx - hintResult.size.width / 2f,
            cy + radius * 0.85f
        )
    )
}

private fun DrawScope.drawStamp(
    cx: Float, cy: Float, radius: Float,
    wobbleAmp: Float, wobbleSeed: Float,
    cookAccent: Color,
    stampScaleVal: Float, stampRotationVal: Float,
    stampAlphaVal: Float, doneTextAlphaVal: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    rotate(degrees = stampRotationVal, pivot = Offset(cx, cy)) {
        scale(scale = stampScaleVal, pivot = Offset(cx, cy)) {
            // Filled circle
            val stampCircle = buildWobbleCirclePath(
                cx, cy, radius * 0.85f, wobbleAmp * 0.7f, wobbleSeed + 50f, 12
            )
            drawPath(
                path = stampCircle,
                color = cookAccent.copy(alpha = 0.15f * stampAlphaVal)
            )
            // Stamp border
            drawPath(
                path = stampCircle,
                color = cookAccent.copy(alpha = stampAlphaVal * 0.6f),
                style = Stroke(
                    width = wobbleAmp * 0.8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Checkmark
            val checkSize = radius * 0.45f
            val checkPath = Path().apply {
                // Start of check: left point
                moveTo(cx - checkSize * 0.5f, cy + checkSize * 0.05f)
                // Down to bottom of check
                val midX = cx - checkSize * 0.1f
                val midY = cy + checkSize * 0.4f
                lineTo(midX, midY)
                // Up to top-right of check
                lineTo(cx + checkSize * 0.5f, cy - checkSize * 0.35f)
            }
            drawPath(
                path = checkPath,
                color = cookAccent.copy(alpha = stampAlphaVal),
                style = Stroke(
                    width = wobbleAmp * 1.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    // "Done!" text (not rotated/scaled with stamp)
    if (doneTextAlphaVal > 0f) {
        val doneResult = textMeasurer.measure(
            text = "Done!",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        drawText(
            textLayoutResult = doneResult,
            color = cookAccent.copy(alpha = doneTextAlphaVal),
            topLeft = Offset(
                cx - doneResult.size.width / 2f,
                cy + radius * 0.55f
            )
        )
    }
}
