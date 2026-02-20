package com.inventory.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The 3 Paper & Ink celebration styles, randomly selected when shopping is complete.
 */
enum class CelebrationType {
    TRIUMPHANT_SCRIBBLE,
    INK_FIREWORKS,
    PAGE_FOLD_SEAL
}

// ---------------------------------------------------------------------------
// Shared ink palette — category-inspired colors for celebrations
// ---------------------------------------------------------------------------
private val InkCelebrationColors = listOf(
    Color(0xFF4CAF50), // Produce green
    Color(0xFF2196F3), // Dairy blue
    Color(0xFFFF9800), // Bakery orange
    Color(0xFFE91E63), // Meat pink
    Color(0xFF9C27B0), // Beverages purple
    Color(0xFF795548), // Pantry brown
    Color(0xFF00BCD4), // Frozen teal
    Color(0xFFFF5722), // Snacks red-orange
)

// ---------------------------------------------------------------------------
// 1. TRIUMPHANT SCRIBBLE
// A big wobbly checkmark writes itself across the canvas, then ink splatters bloom.
// ---------------------------------------------------------------------------

private data class ScribbleDroplet(
    val angle: Float,
    val distance: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun TriumphantScribble(
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Main checkmark stroke progress (0 → 1)
    val strokeProgress = remember { Animatable(0f) }

    // Droplet bloom after stroke finishes
    val dropletProgress = remember { Animatable(0f) }

    // Haptic fired once
    var hapticFired by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Draw the checkmark stroke — 450ms with spring feel
        strokeProgress.animateTo(
            1f,
            animationSpec = tween(450, easing = EaseOutCubic)
        )
        // Fire haptic at checkmark completion
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        hapticFired = true
        // Bloom the droplets
        dropletProgress.animateTo(
            1f,
            animationSpec = tween(350, easing = EaseOutCubic)
        )
    }

    // Pre-computed wobble seed and droplets — stable across recompositions
    val wobbleSeed = remember { Random.nextFloat() * 1000f }
    val droplets = remember {
        val colors = InkCelebrationColors.shuffled()
        List(8) { i ->
            ScribbleDroplet(
                angle = Random.nextFloat() * 360f,
                distance = 14f + Random.nextFloat() * 20f,
                radius = 1.8f + Random.nextFloat() * 2.2f,
                color = colors[i % colors.size]
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val centerY = h * 0.5f

        // Checkmark geometry — short leg then long leg
        // Short leg: bottom-left to bottom-center
        val startX = centerX - w * 0.22f
        val startY = centerY - h * 0.02f
        val midX = centerX - w * 0.05f
        val midY = centerY + h * 0.22f
        val endX = centerX + w * 0.25f
        val endY = centerY - h * 0.22f

        // Build wobbly checkmark path with bezier wobble
        val checkPath = Path().apply {
            moveTo(startX, startY)
            // Short leg — 3 segments
            val seg1Segments = 3
            for (i in 1..seg1Segments) {
                val t = i.toFloat() / seg1Segments
                val targetX = startX + (midX - startX) * t
                val targetY = startY + (midY - startY) * t
                val wobbleY = sin((i + wobbleSeed) * 2.1 + PI / 3).toFloat() * (h * 0.015f)
                val ctrlX = startX + (midX - startX) * (t - 0.5f / seg1Segments)
                val ctrlY = startY + (midY - startY) * (t - 0.5f / seg1Segments) + wobbleY * 1.5f
                quadraticBezierTo(ctrlX, ctrlY, targetX, targetY + wobbleY)
            }
            // Long leg — 4 segments
            val seg2Segments = 4
            for (i in 1..seg2Segments) {
                val t = i.toFloat() / seg2Segments
                val targetX = midX + (endX - midX) * t
                val targetY = midY + (endY - midY) * t
                val wobbleY = sin((i + 3 + wobbleSeed) * 1.7).toFloat() * (h * 0.012f)
                val ctrlX = midX + (endX - midX) * (t - 0.5f / seg2Segments)
                val ctrlY = midY + (endY - midY) * (t - 0.5f / seg2Segments) + wobbleY * 1.5f
                quadraticBezierTo(ctrlX, ctrlY, targetX, targetY + wobbleY)
            }
        }

        // Clip right edge: short leg ≈ 30% of stroke, long leg ≈ 70%
        val clipRight = if (strokeProgress.value < 0.3f) {
            // Drawing short leg
            val legProgress = strokeProgress.value / 0.3f
            startX + (midX - startX) * legProgress
        } else {
            // Drawing long leg
            val legProgress = (strokeProgress.value - 0.3f) / 0.7f
            midX + (endX - midX) * legProgress
        }

        // Layer 1: Ink bleed (wide, semi-transparent)
        clipRect(right = clipRight) {
            drawPath(
                path = checkPath,
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                style = Stroke(
                    width = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Layer 2: Core stroke
        clipRect(right = clipRight) {
            drawPath(
                path = checkPath,
                color = Color(0xFF2E7D32).copy(alpha = 0.85f),
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Ink droplets bloom from the checkmark endpoint
        if (dropletProgress.value > 0f) {
            val dp = dropletProgress.value
            // Endpoint of check — approximate at mid-point of the check
            val splashX = midX
            val splashY = midY

            droplets.forEach { droplet ->
                val radians = droplet.angle * (PI / 180f).toFloat()
                val dist = droplet.distance.dp.toPx() * dp
                val dx = cos(radians) * dist
                val dy = sin(radians) * dist
                val alpha = (1f - dp * 0.5f) * 0.75f

                drawCircle(
                    color = droplet.color.copy(alpha = alpha),
                    radius = droplet.radius.dp.toPx() * (0.5f + dp * 0.5f),
                    center = Offset(splashX + dx, splashY + dy)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. INK FIREWORKS
// Ink droplets launch upward from center, arc with gravity, splatter at apex.
// ---------------------------------------------------------------------------

private data class FireworkDroplet(
    val launchAngle: Float,    // degrees from vertical (-60 to 60)
    val launchForce: Float,    // how high it goes (0.5 to 1.0)
    val color: Color,
    val radius: Float,
    val delay: Float           // stagger (0.0 to 0.3)
)

@Composable
fun InkFireworks(
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val progress = remember { Animatable(0f) }
    var hapticFired by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            animationSpec = tween(1200, easing = EaseOutCubic)
        )
    }

    // Fire haptic at peak moment (~40%)
    LaunchedEffect(progress.value) {
        if (!hapticFired && progress.value > 0.4f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hapticFired = true
        }
    }

    val droplets = remember {
        val colors = InkCelebrationColors.shuffled()
        List(18) { i ->
            FireworkDroplet(
                launchAngle = -55f + Random.nextFloat() * 110f,
                launchForce = 0.5f + Random.nextFloat() * 0.5f,
                color = colors[i % colors.size],
                radius = 2f + Random.nextFloat() * 2.5f,
                delay = Random.nextFloat() * 0.25f
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val w = size.width
        val h = size.height
        val launchX = w * 0.5f
        val launchY = h * 0.85f // bottom area

        val p = progress.value

        droplets.forEach { droplet ->
            // Staggered start
            val adjustedP = ((p - droplet.delay) / (1f - droplet.delay)).coerceIn(0f, 1f)
            if (adjustedP <= 0f) return@forEach

            val radians = ((-90f + droplet.launchAngle) * PI / 180f).toFloat()
            val force = droplet.launchForce * h * 0.7f

            // Parabolic trajectory: x = force * cos(angle) * t, y = force * sin(angle) * t + gravity * t^2
            val t = adjustedP
            val gravity = h * 0.6f // gravity pull
            val dx = cos(radians) * force * t
            val dy = sin(radians) * force * t + gravity * t * t

            val x = launchX + dx
            val y = launchY + dy

            // Alpha: fade in quickly, fade out in final 30%
            val alpha = when {
                adjustedP < 0.1f -> adjustedP / 0.1f
                adjustedP > 0.7f -> (1f - (adjustedP - 0.7f) / 0.3f) * 0.8f
                else -> 0.8f
            }

            // Size: start small, grow to peak at 50%, then shrink slightly
            val sizeScale = when {
                adjustedP < 0.5f -> 0.4f + adjustedP * 1.2f
                else -> 1f - (adjustedP - 0.5f) * 0.4f
            }

            // At apex, add a slight splatter effect (extra ring)
            if (adjustedP in 0.35f..0.65f) {
                val splatAlpha = (1f - kotlin.math.abs(adjustedP - 0.5f) / 0.15f).coerceIn(0f, 1f) * 0.2f
                drawCircle(
                    color = droplet.color.copy(alpha = splatAlpha),
                    radius = droplet.radius.dp.toPx() * sizeScale * 2.5f,
                    center = Offset(x, y)
                )
            }

            drawCircle(
                color = droplet.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = droplet.radius.dp.toPx() * sizeScale,
                center = Offset(x, y)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. PAGE FOLD & SEAL
// Background crumples asymmetrically, then a wax-seal stamp lands with bounce.
// ---------------------------------------------------------------------------

@Composable
fun PageFoldAndSeal(
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Phase 1: crumple (0→1 over 300ms)
    val crumpleProgress = remember { Animatable(0f) }
    // Phase 2: seal landing
    val sealScale = remember { Animatable(0.3f) }
    val sealAlpha = remember { Animatable(0f) }
    // Phase 3: idle breathing
    var breathingStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Phase 1: crumple
        crumpleProgress.animateTo(
            1f,
            animationSpec = tween(300, easing = EaseOutCubic)
        )
        delay(100)
        // Phase 2: seal drops in with wobbly spring
        sealAlpha.animateTo(1f, animationSpec = tween(100))
        sealScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = 0.3f,
                stiffness = 200f
            )
        )
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        breathingStarted = true
    }

    // Idle breathing on the seal after landing
    val infiniteTransition = rememberInfiniteTransition(label = "seal_breathe")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seal_breathe_scale"
    )

    val cp = crumpleProgress.value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer {
                // Asymmetric crumple: scaleX and scaleY shrink slightly then recover
                val crumpleX = if (cp < 0.6f) 1f - cp * 0.12f else 1f - 0.072f + (cp - 0.6f) * 0.18f
                val crumpleY = if (cp < 0.6f) 1f - cp * 0.18f else 1f - 0.108f + (cp - 0.6f) * 0.27f
                scaleX = crumpleX
                scaleY = crumpleY
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The seal
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    val effectiveScale = sealScale.value * (if (breathingStarted) breathScale else 1f)
                    scaleX = effectiveScale
                    scaleY = effectiveScale
                    alpha = sealAlpha.value
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val sealRadius = w * 0.42f

            // Outer ring — wax seal edge with scalloped look
            val scallops = 16
            val sealPath = Path().apply {
                for (i in 0 until scallops) {
                    val angle = (2 * PI * i / scallops).toFloat()
                    val nextAngle = (2 * PI * (i + 1) / scallops).toFloat()
                    val outerR = sealRadius * 1.08f
                    val innerR = sealRadius * 0.95f

                    if (i == 0) {
                        moveTo(cx + cos(angle) * outerR, cy + sin(angle) * outerR)
                    }

                    val midAngle = (angle + nextAngle) / 2f
                    val ctrlR = innerR
                    quadraticBezierTo(
                        cx + cos(midAngle) * ctrlR,
                        cy + sin(midAngle) * ctrlR,
                        cx + cos(nextAngle) * outerR,
                        cy + sin(nextAngle) * outerR
                    )
                }
                close()
            }

            // Seal body — warm tertiary color
            drawPath(
                path = sealPath,
                color = Color(0xFFC62828).copy(alpha = 0.85f)
            )

            // Inner circle
            drawCircle(
                color = Color(0xFFD32F2F).copy(alpha = 0.6f),
                radius = sealRadius * 0.72f,
                center = Offset(cx, cy)
            )

            // Checkmark inside the seal — wobbly, hand-drawn feel
            val checkPath = Path().apply {
                val scale = sealRadius * 0.5f
                val checkStartX = cx - scale * 0.45f
                val checkStartY = cy + scale * 0.05f
                val checkMidX = cx - scale * 0.08f
                val checkMidY = cy + scale * 0.35f
                val checkEndX = cx + scale * 0.5f
                val checkEndY = cy - scale * 0.35f

                moveTo(checkStartX, checkStartY)
                quadraticBezierTo(
                    checkStartX + (checkMidX - checkStartX) * 0.5f,
                    checkStartY + (checkMidY - checkStartY) * 0.5f + scale * 0.04f,
                    checkMidX, checkMidY
                )
                quadraticBezierTo(
                    checkMidX + (checkEndX - checkMidX) * 0.4f,
                    checkMidY + (checkEndY - checkMidY) * 0.4f - scale * 0.03f,
                    checkEndX, checkEndY
                )
            }

            drawPath(
                path = checkPath,
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Trip summary data for the receipt
// ---------------------------------------------------------------------------

data class CategoryStat(
    val name: String,
    val count: Int,
    val color: Color
)

// ---------------------------------------------------------------------------
// WRAPPER: AllDoneCelebration — picks the right animation, adds receipt summary + button
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllDoneCelebration(
    celebrationType: CelebrationType,
    purchasedCount: Int,
    estimatedTotal: Double,
    currencySymbol: String,
    pricedItemCount: Int,
    categoryBreakdown: List<CategoryStat>,
    onClearAndFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Staggered reveal phases
    var showTagline by remember { mutableStateOf(false) }
    var showReceipt by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600) // wait for main animation to hit climax
        showTagline = true
        delay(300)
        showReceipt = true
        delay(350)
        showCategories = true
        delay(250)
        showButton = true
    }

    // Ink separator line — draws left to right
    val separatorProgress = remember { Animatable(0f) }
    LaunchedEffect(showReceipt) {
        if (showReceipt) {
            separatorProgress.animateTo(
                1f,
                animationSpec = tween(400, easing = EaseOutCubic)
            )
        }
    }

    // Animated total — counts up like a pen tallying
    val animatedTotal = remember { Animatable(0f) }
    LaunchedEffect(showReceipt) {
        if (showReceipt && estimatedTotal > 0) {
            animatedTotal.animateTo(
                estimatedTotal.toFloat(),
                animationSpec = tween(800, easing = EaseOutCubic)
            )
        }
    }

    // Wobble seed for the separator line
    val wobbleSeed = remember { Random.nextFloat() * 100f }

    // Rotating taglines for variety
    val tagline = remember {
        listOf(
            "All done!",
            "List conquered!",
            "Nailed it!",
            "Shopping complete!",
            "Every last one!"
        ).random()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // The celebration animation
        when (celebrationType) {
            CelebrationType.TRIUMPHANT_SCRIBBLE -> TriumphantScribble()
            CelebrationType.INK_FIREWORKS -> InkFireworks()
            CelebrationType.PAGE_FOLD_SEAL -> PageFoldAndSeal()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tagline — Fade Up entrance
        AnimatedVisibility(
            visible = showTagline,
            enter = fadeIn(tween(200)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = 1f, stiffness = 200f)
            )
        ) {
            Text(
                text = tagline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Ink separator line ───
        AnimatedVisibility(
            visible = showReceipt,
            enter = fadeIn(tween(100))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(4.dp)
            ) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(0f, h / 2f)
                    val segments = 10
                    for (i in 1..segments) {
                        val x = w * i / segments
                        val wobble = sin((i + wobbleSeed) * 1.7) * h * 0.35f
                        lineTo(x, h / 2f + wobble.toFloat())
                    }
                }
                clipRect(right = w * separatorProgress.value) {
                    drawPath(
                        path,
                        color = Color(0xFF8D6E63).copy(alpha = 0.35f),
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Receipt stats: item count + estimated total ───
        AnimatedVisibility(
            visible = showReceipt,
            enter = fadeIn(tween(200)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$purchasedCount item${if (purchasedCount != 1) "s" else ""} crossed off",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (estimatedTotal > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "\u2248 ",  // ≈ symbol
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${"%.2f".format(animatedTotal.value.toDouble())}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (pricedItemCount < purchasedCount) {
                        Text(
                            text = "based on $pricedItemCount of $purchasedCount items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // ─── Category breakdown pills ───
        if (categoryBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            AnimatedVisibility(
                visible = showCategories,
                enter = fadeIn(tween(150))
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryBreakdown.take(5).forEachIndexed { index, stat ->
                        // Staggered entrance per pill — 70ms apart (Paper & Ink spec)
                        var pillVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 70L)
                            pillVisible = true
                        }
                        AnimatedVisibility(
                            visible = pillVisible,
                            enter = fadeIn(tween(200)) + slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = 0.5f,
                                    stiffness = 200f
                                )
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        stat.color.copy(alpha = 0.10f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        0.5.dp,
                                        stat.color.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    // Ink dot
                                    Canvas(Modifier.size(6.dp)) {
                                        drawCircle(color = stat.color.copy(alpha = 0.75f))
                                    }
                                    Text(
                                        text = "${stat.name} \u00d7${stat.count}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = stat.color.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── "Clear & Finish" button — settles in last ───
        AnimatedVisibility(
            visible = showButton,
            enter = fadeIn(tween(200)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f)
            )
        ) {
            FilledTonalButton(
                onClick = onClearAndFinish,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    "Clear & Finish",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
