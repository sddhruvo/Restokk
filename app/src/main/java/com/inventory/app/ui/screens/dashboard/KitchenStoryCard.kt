package com.inventory.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.app.domain.model.KitchenStoryMission
import com.inventory.app.domain.model.KitchenStoryState
import com.inventory.app.ui.components.InkProgressLine
import com.inventory.app.ui.components.InkStrikethrough
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val BouncySpring = PaperInkMotion.BouncySpring
private val WobblySpring = PaperInkMotion.WobblySpring

// ─── Torn Paper Shape ─────────────────────────────────────────────────

private class TornPaperShape(
    private val cornerRadiusDp: Float = 16f,
    private val tearAmplitudeDp: Float = 2f,
    private val tearSegments: Int = 20
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cr = with(density) { cornerRadiusDp.dp.toPx() }
        val amp = with(density) { tearAmplitudeDp.dp.toPx() }
        val path = Path().apply {
            // Start at top-left corner (after radius)
            moveTo(cr, 0f)
            // Top edge: sine-wave torn effect
            val segWidth = (size.width - 2 * cr) / tearSegments
            for (i in 0 until tearSegments) {
                val x = cr + segWidth * (i + 1)
                val yOffset = sin((i.toFloat() / tearSegments) * PI * 4).toFloat() * amp
                lineTo(x, yOffset)
            }
            // Top-right corner
            lineTo(size.width - cr, 0f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    size.width - 2 * cr, 0f, size.width, 2 * cr
                ),
                startAngleDegrees = -90f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            // Right edge
            lineTo(size.width, size.height - cr)
            // Bottom-right corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    size.width - 2 * cr, size.height - 2 * cr, size.width, size.height
                ),
                startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            // Bottom edge
            lineTo(cr, size.height)
            // Bottom-left corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    0f, size.height - 2 * cr, 2 * cr, size.height
                ),
                startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            // Left edge
            lineTo(0f, cr)
            // Top-left corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    0f, 0f, 2 * cr, 2 * cr
                ),
                startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

private val tornPaperShape = TornPaperShape()

// ─── Main Card ─────────────────────────────────────────────────────────

@Composable
fun KitchenStoryCard(
    state: KitchenStoryState,
    onMissionTap: (KitchenStoryMission) -> Unit,
    onDismiss: () -> Unit,
    onAllComplete: () -> Unit,
    onDismissSmartDefaults: () -> Unit,
    onShowMeSmartDefaults: () -> Unit = {},
    onExitComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var showDismissDialog by rememberSaveable { mutableStateOf(false) }
    var celebrationTriggered by rememberSaveable { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    // Crumple exit state
    var isExiting by remember { mutableStateOf(false) }
    val exitProgress = remember { Animatable(0f) }

    // Crumple exit animation
    LaunchedEffect(isExiting) {
        if (isExiting) {
            exitProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
            onExitComplete()
        }
    }

    // Detect all-complete and trigger celebration → then crumple exit
    // onAllComplete is NOT called here — crumple's onExitComplete handles persistence
    LaunchedEffect(state.allComplete) {
        if (state.allComplete && !celebrationTriggered) {
            celebrationTriggered = true
            showCelebration = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2500L)
            showCelebration = false
            isExiting = true
        }
    }

    // Entrance animation
    val entranceAlpha = remember { Animatable(0f) }
    val entranceTranslateY = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        launch { entranceAlpha.animateTo(1f, tween(400)) }
        entranceTranslateY.animateTo(0f, BouncySpring)
    }

    // Paper background color — warm tint
    val paperColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val ruledLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val primaryColor = MaterialTheme.colorScheme.primary

    // Crumple transform values
    val crumpleScaleX = 1f - exitProgress.value * 0.1f
    val crumpleScaleY = 1f - exitProgress.value * 0.15f
    val crumpleAlpha = (1f - exitProgress.value).coerceIn(0f, 1f)
    val crumpleTranslateY = with(density) { -10.dp.toPx() } * exitProgress.value

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = entranceAlpha.value * crumpleAlpha
                    translationY = entranceTranslateY.value + crumpleTranslateY
                    scaleX = crumpleScaleX
                    scaleY = crumpleScaleY
                }
                .clip(tornPaperShape)
                .drawBehind {
                    // Ruled lines behind content
                    val lineSpacing = 24.dp.toPx()
                    var y = lineSpacing
                    while (y < size.height) {
                        drawLine(
                            color = ruledLineColor,
                            start = Offset(16.dp.toPx(), y),
                            end = Offset(size.width - 16.dp.toPx(), y),
                            strokeWidth = 0.5.dp.toPx()
                        )
                        y += lineSpacing
                    }
                },
            color = paperColor,
            shape = tornPaperShape,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                KitchenStoryHeader(
                    chapterNumber = state.chapterNumber,
                    canDismiss = state.canDismiss,
                    onDismissClick = { showDismissDialog = true }
                )

                // Mission rows — any uncompleted mission with navTarget is tappable
                state.missions.forEachIndexed { index, mission ->
                    val isCurrent = index == state.currentMissionIndex
                    val canNavigate = !mission.isCompleted && mission.navTarget != null
                    MissionRow(
                        mission = mission,
                        isCurrent = isCurrent,
                        onClick = if (canNavigate) { { onMissionTap(mission) } } else null
                    )
                }

                // Smart Defaults education — appears once when first item is added
                AnimatedVisibility(visible = state.showSmartDefaultsEducation) {
                    SmartDefaultsEducation(
                        onDismiss = onDismissSmartDefaults,
                        onShowMe = onShowMeSmartDefaults
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress section
                ProgressSection(
                    completedCount = state.completedCount,
                    totalCount = state.totalCount,
                    subtitle = state.cardSubtitle,
                    primaryColor = primaryColor
                )
            }
        }

        // Celebration overlay
        if (showCelebration) {
            KitchenStoryCompletion()
        }
    }

    // Dismiss dialog
    if (showDismissDialog) {
        AlertDialog(
            onDismissRequest = { showDismissDialog = false },
            title = { Text("Done exploring?") },
            text = { Text("You've completed ${state.completedCount} of ${state.totalCount} missions. The card won't come back.") },
            confirmButton = {
                TextButton(onClick = {
                    showDismissDialog = false
                    isExiting = true
                    onDismiss()
                }) { Text("I'm good") }
            },
            dismissButton = {
                TextButton(onClick = { showDismissDialog = false }) {
                    Text("Keep it")
                }
            }
        )
    }
}

// ─── Header ────────────────────────────────────────────────────────────

@Composable
private fun KitchenStoryHeader(
    chapterNumber: Int,
    canDismiss: Boolean,
    onDismissClick: () -> Unit
) {
    // Chapter label flip animation — 360° rotationX on chapter change
    val chapterRotation = remember { Animatable(0f) }
    LaunchedEffect(chapterNumber) {
        if (chapterNumber > 1) {
            chapterRotation.snapTo(0f)
            chapterRotation.animateTo(360f, tween(300))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Your Kitchen Story",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Ch. $chapterNumber",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer { rotationX = chapterRotation.value }
            )
        }
        if (canDismiss) {
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss kitchen story",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Mission Row ───────────────────────────────────────────────────────

@Composable
private fun MissionRow(
    mission: KitchenStoryMission,
    isCurrent: Boolean,
    onClick: (() -> Unit)?
) {
    val alpha = when {
        mission.isCompleted -> 0.55f
        isCurrent -> 1f
        else -> 0.7f
    }

    val haptic = LocalHapticFeedback.current
    // Detect false→true transition for mini-burst
    val wasCompleted = rememberSaveable(mission.index) { mutableStateOf(mission.isCompleted) }
    var showBurst by remember(mission.index) { mutableStateOf(false) }
    LaunchedEffect(mission.isCompleted, mission.index) {
        if (mission.isCompleted && !wasCompleted.value) {
            showBurst = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            wasCompleted.value = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox with mini-burst overlay
        Box(
            modifier = Modifier.padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            HandDrawnCheckbox(
                isCompleted = mission.isCompleted,
                isCurrent = isCurrent
            )
            if (showBurst) {
                MiniInkBurst(
                    color = MaterialTheme.colorScheme.primary,
                    onComplete = { showBurst = false }
                )
            }
        }

        // Text content with ink strikethrough overlay
        Column(modifier = Modifier.weight(1f)) {
            // Track whether this mission was already completed on first composition (for strikethrough)
            val wasCompletedOnMount = rememberSaveable(mission.index) { mutableStateOf(mission.isCompleted) }
            val strikeProgress = remember(mission.index) { Animatable(if (wasCompletedOnMount.value) 1f else 0f) }

            // Animate strikethrough when mission newly completes
            LaunchedEffect(mission.isCompleted, mission.index) {
                if (mission.isCompleted && !wasCompletedOnMount.value) {
                    strikeProgress.animateTo(1f, tween(400))
                    wasCompletedOnMount.value = true
                }
            }

            Box {
                Text(
                    text = mission.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                if (mission.isCompleted || strikeProgress.value > 0f) {
                    InkStrikethrough(
                        progress = strikeProgress.value,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
            // Dynamic subtitle for current mission
            if (isCurrent && mission.dynamicSubtitle != null) {
                Text(
                    text = mission.dynamicSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )
            }
        }

        // Arrow for any tappable mission
        if (onClick != null) {
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = "Go",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isCurrent) 0.7f else 0.4f)
            )
        }
    }
}

// ─── Hand-Drawn Checkbox ───────────────────────────────────────────────

@Composable
private fun HandDrawnCheckbox(
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val completedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val futureColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    // Breathing for current
    val breathingTransition = rememberInfiniteTransition(label = "checkboxBreathe")
    val breathingScale = breathingTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val checkProgress = remember { Animatable(if (isCompleted) 1f else 0f) }
    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            checkProgress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    val wobbleSeed = remember { (Math.random() * 100).toFloat() }

    Canvas(modifier = modifier.size(22.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.width * 0.38f

        when {
            isCompleted -> {
                // Filled circle with wobble checkmark
                drawCircle(
                    color = completedColor,
                    radius = radius,
                    center = Offset(cx, cy)
                )
                // Checkmark
                if (checkProgress.value > 0f) {
                    val path = Path().apply {
                        val startX = cx - radius * 0.45f
                        val startY = cy + radius * 0.05f
                        val midX = cx - radius * 0.05f
                        val midY = cy + radius * 0.4f
                        val endX = cx + radius * 0.5f
                        val endY = cy - radius * 0.35f

                        moveTo(startX, startY)
                        if (checkProgress.value < 0.5f) {
                            val t = checkProgress.value / 0.5f
                            lineTo(
                                startX + (midX - startX) * t,
                                startY + (midY - startY) * t
                            )
                        } else {
                            lineTo(midX, midY)
                            val t = (checkProgress.value - 0.5f) / 0.5f
                            lineTo(
                                midX + (endX - midX) * t,
                                midY + (endY - midY) * t
                            )
                        }
                    }
                    drawPath(
                        path = path,
                        color = completedColor.copy(alpha = 0.8f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            isCurrent -> {
                // Filled circle in primary with breathing
                val scale = breathingScale.value
                drawCircle(
                    color = primaryColor,
                    radius = radius * scale,
                    center = Offset(cx, cy)
                )
            }
            else -> {
                // Empty circle with slight wobble
                val wobblePath = Path().apply {
                    val segments = 12
                    for (i in 0..segments) {
                        val angle = (2 * PI * i / segments).toFloat()
                        val wobble = 1f + sin(angle * 3 + wobbleSeed).toFloat() * 0.04f
                        val x = cx + radius * wobble * kotlin.math.cos(angle)
                        val y = cy + radius * wobble * kotlin.math.sin(angle)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = wobblePath,
                    color = futureColor,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

// ─── Mini Ink Burst (per-mission completion) ──────────────────────────

@Composable
private fun MiniInkBurst(
    color: Color,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class BurstParticle(val angle: Float, val speed: Float, val radius: Float)

    val particles = remember {
        List(4) { i ->
            BurstParticle(
                angle = 90f * i + (Math.random() * 30f - 15f).toFloat(),
                speed = 8f + (Math.random() * 8f).toFloat(),
                radius = 1.5f + (Math.random() * 1f).toFloat()
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        onComplete()
    }

    Canvas(modifier = modifier.size(30.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        particles.forEach { p ->
            val radians = p.angle * (PI / 180f).toFloat()
            val dist = p.speed.dp.toPx() * progress.value
            val x = cx + kotlin.math.cos(radians) * dist
            val y = cy + kotlin.math.sin(radians) * dist
            val fadeAlpha = (1f - progress.value * 0.8f).coerceIn(0f, 1f)
            drawCircle(
                color = color.copy(alpha = fadeAlpha),
                radius = p.radius.dp.toPx() * (1f - progress.value * 0.3f),
                center = Offset(x, y)
            )
        }
    }
}

// ─── Smart Defaults Education ──────────────────────────────────────────

@Composable
private fun SmartDefaultsEducation(onDismiss: () -> Unit, onShowMe: () -> Unit = {}) {
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAlpha.animateTo(1f, tween(400))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = enterAlpha.value },
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Smart Defaults in action",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "We auto-detected your item's category, estimated shelf life, and likely storage spot. These predictions improve the more you use the app — edit anything that's not quite right.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onShowMe) {
                    Text("Show me", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onDismiss) {
                    Text("Got it", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─── Progress Section ──────────────────────────────────────────────────

@Composable
private fun ProgressSection(
    completedCount: Int,
    totalCount: Int,
    subtitle: String,
    primaryColor: Color
) {
    val fraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "progressFraction"
    )

    // Count pop animation
    val countScale = remember { Animatable(1f) }
    LaunchedEffect(completedCount) {
        if (completedCount > 0) {
            countScale.animateTo(1.3f, WobblySpring)
            countScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InkProgressLine(
                progress = animatedFraction,
                color = primaryColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "$completedCount/$totalCount",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.graphicsLayer {
                    scaleX = countScale.value
                    scaleY = countScale.value
                }
            )
        }
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// ─── Completion Celebration ────────────────────────────────────────────

@Composable
private fun KitchenStoryCompletion() {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(300)) }
        scale.animateTo(1f, BouncySpring)
        delay(1800L)
        alpha.animateTo(0f, tween(400))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        // Ink particles
        InkBurstParticles()

        Text(
            "Your Kitchen is Alive!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun InkBurstParticles() {
    val inkColors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )

    data class Particle(
        val angle: Float,
        val speed: Float,
        val radius: Float,
        val color: Color
    )

    val particles = remember {
        List(20) { i ->
            Particle(
                angle = (360f / 20f) * i + (Math.random() * 18f).toFloat(),
                speed = 40f + (Math.random() * 60f).toFloat(),
                radius = 2f + (Math.random() * 3f).toFloat(),
                color = inkColors[i % inkColors.size]
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.size(200.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        particles.forEach { p ->
            val radians = p.angle * (PI / 180f).toFloat()
            val dist = p.speed.dp.toPx() * progress.value
            val x = cx + kotlin.math.cos(radians) * dist
            val y = cy + kotlin.math.sin(radians) * dist
            val fadeAlpha = (1f - progress.value * 0.7f).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = fadeAlpha),
                radius = p.radius.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
