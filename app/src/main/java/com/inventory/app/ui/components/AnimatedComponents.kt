package com.inventory.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// ─── Shimmer Effect ─────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200f, 0f),
        end = Offset(translateAnim.value + 200f, 0f)
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f), height = 16.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f), height = 32.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f), height = 12.dp)
        }
    }
}

@Composable
fun ShimmerListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(40.dp), height = 40.dp, shape = RoundedCornerShape(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f), height = 10.dp)
        }
    }
}

@Composable
fun ShimmerStatCard(modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(modifier = Modifier.size(24.dp), height = 24.dp, shape = RoundedCornerShape(4.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f), height = 28.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f), height = 12.dp)
        }
    }
}

// ─── Staggered Animation for Lists ───────────────────────────────────────

@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    slideOffsetDivisor: Int = 4,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / slideOffsetDivisor },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
        modifier = modifier
    ) {
        content()
    }
}
// ─── Animated Empty State ────────────────────────────────────────────

@Composable
fun AnimatedEmptyState(
    icon: ImageVector = Icons.Filled.Inbox,
    title: String,
    message: String = "",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val infiniteTransition = rememberInfiniteTransition(label = "emptyStateBounce")
    val translateY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.8f)
    ) {
        Column(
            modifier = modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Empty state",
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer { translationY = translateY.value },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Animated Counter ──────────────────────────────────────────────

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: @Composable (String) -> Unit
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "counter"
    )
    style("$animatedValue")
}

// ─── Pulse Animation for Status Chips ────────────────────────────────────

@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha.value))
    )
}
// ─── Animated Status Chip ────────────────────────────────────────────

@Composable
fun AnimatedStatusChip(
    text: String,
    color: Color,
    pulse: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (pulse) {
                PulsingDot(color = color)
            }
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

// ─── Expandable Section ──────────────────────────────────────────────

@Composable
fun ExpandableSection(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "expandRotation"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(300))
        ) {
            content()
        }
    }
}

// ─── Animated FAB ──────────────────────────────────────────────────

@Composable
fun AnimatedFab(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        FloatingActionButton(onClick = onClick) {
            icon()
        }
    }
}

// ─── Dashboard Greeting ─────────────────────────────────────────────────

@Composable
fun DashboardGreeting(
    totalItems: Int,
    expiringSoon: Int,
    modifier: Modifier = Modifier
) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val subtitle = when {
        totalItems == 0 -> "Start by adding your first item"
        expiringSoon > 0 -> "$expiringSoon item${if (expiringSoon != 1) "s" else ""} expiring soon"
        else -> "Tracking $totalItems item${if (totalItems != 1) "s" else ""}"
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)),
        modifier = modifier
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Form Progress Indicator ────────────────────────────────────────────

@Composable
fun FormProgressIndicator(
    progress: Float,
    sectionName: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "formProgress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = sectionName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ─── Save Button with Loading State ──────────────────────────────────────

@Composable
fun AnimatedSaveButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isSaved: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { if (!isLoading && !isSaved) onClick() },
        modifier = modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        AnimatedContent(
            targetState = when {
                isLoading -> "loading"
                isSaved -> "saved"
                else -> "idle"
            },
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            label = "saveButtonState"
        ) { state ->
            when (state) {
                "loading" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("Saving...")
                    }
                }
                "saved" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Saved", modifier = Modifier.size(18.dp))
                        Text("Saved!")
                    }
                }
                else -> Text(text)
            }
        }
    }
}

// ─── Ink-Style Processing Animation ─────────────────────────────────────

enum class InkDotState { PENDING, ACTIVE, COMPLETED }

/**
 * Ink-bloom step indicator dot for processing screens.
 * PENDING: small gray dot. ACTIVE: scales in with wobble, then breathes.
 * COMPLETED: settles with spring, draws a tiny ink checkmark.
 */
@Composable
fun InkBloomDot(
    state: InkDotState,
    color: Color,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(if (state == InkDotState.PENDING) 1f else 0f) }
    val checkProgress = remember { Animatable(0f) }

    // Handle state transitions
    LaunchedEffect(state) {
        when (state) {
            InkDotState.PENDING -> {
                scale.snapTo(1f)
            }
            InkDotState.ACTIVE -> {
                // Scale in with WobblySpring
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.3f, stiffness = 200f)
                )
            }
            InkDotState.COMPLETED -> {
                // Settle with SettleSpring
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f)
                )
                // Draw checkmark
                checkProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // Breathing for ACTIVE state
    val breathingTransition = rememberInfiniteTransition(label = "inkDotBreathe")
    val breathingScale = breathingTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val effectiveScale = when (state) {
        InkDotState.ACTIVE -> scale.value * breathingScale.value
        else -> scale.value
    }

    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            InkDotState.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { scaleX = effectiveScale; scaleY = effectiveScale }
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
            }
            InkDotState.ACTIVE -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer { scaleX = effectiveScale; scaleY = effectiveScale }
                        .background(color, CircleShape)
                )
            }
            InkDotState.COMPLETED -> {
                // Ink checkmark via Canvas
                Canvas(
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { scaleX = effectiveScale; scaleY = effectiveScale }
                ) {
                    val w = size.width
                    val h = size.height
                    val cp = checkProgress.value

                    // Checkmark path: short stroke down-right, then long stroke up-right
                    val path = Path().apply {
                        // Start at left-center
                        moveTo(w * 0.15f, h * 0.5f)
                        // Down to bottom-center
                        val midX = w * 0.4f
                        val midY = h * 0.72f
                        if (cp < 0.5f) {
                            // First half: draw only the first stroke
                            val t = cp / 0.5f
                            lineTo(
                                w * 0.15f + (midX - w * 0.15f) * t,
                                h * 0.5f + (midY - h * 0.5f) * t
                            )
                        } else {
                            lineTo(midX, midY)
                            // Second half: draw the second stroke
                            val t = (cp - 0.5f) / 0.5f
                            val endX = w * 0.85f
                            val endY = h * 0.28f
                            lineTo(
                                midX + (endX - midX) * t,
                                midY + (endY - midY) * t
                            )
                        }
                    }

                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * Wobbly ink line that draws itself left→right as progress goes 0.0→1.0.
 * Two layers (bleed + core) matching InkStrikethrough style, plus trailing droplets.
 */
@Composable
fun InkProgressLine(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
    val droplets = remember {
        listOf(
            Triple(30f, 10f, 2.2f),   // angle, distance, radius
            Triple(-25f, 15f, 1.6f),
            Triple(8f, 8f, 1.8f)
        )
    }

    Canvas(modifier = modifier.height(12.dp)) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        // Build wobbly path — 8 bezier segments
        val path = Path().apply {
            val segments = 8
            val segWidth = w / segments
            moveTo(0f, centerY)
            for (i in 1..segments) {
                val endX = segWidth * i
                val endY = centerY + sin((i + wobbleSeed) * 1.3).toFloat() * (h * 0.15f)
                val ctrlX = segWidth * (i - 0.5f)
                val ctrlY = centerY + sin((i + wobbleSeed) * 2.1 + PI / 3).toFloat() * (h * 0.25f)
                quadraticBezierTo(ctrlX, ctrlY, endX, endY)
            }
        }

        val clipRight = w * progress.coerceIn(0f, 1f)

        if (clipRight > 0f) {
            // Layer 1: Bleed (wider, low alpha)
            clipRect(right = clipRight) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.25f),
                    style = Stroke(
                        width = 6.dp.toPx(),
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
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Trailing ink droplets at the moving endpoint
            if (progress > 0.05f) {
                val segments = 8
                val segWidth = w / segments
                // Find Y at the current progress point (approximate with last segment endpoint)
                val segIndex = (progress * segments).toInt().coerceIn(1, segments)
                val endY = centerY + sin((segIndex + wobbleSeed) * 1.3).toFloat() * (h * 0.15f)

                droplets.forEach { (angle, distance, radius) ->
                    val radians = angle * (PI / 180f).toFloat()
                    val dist = distance.dp.toPx() * 0.7f
                    val dx = cos(radians) * dist
                    val dy = sin(radians) * dist
                    val alpha = 0.6f

                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = radius.dp.toPx(),
                        center = Offset(clipRight + dx, endY + dy)
                    )
                }
            }
        }
    }
}
