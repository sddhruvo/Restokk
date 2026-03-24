package com.inventory.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.inventory.app.ui.navigation.LocalBottomNavHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.inventory.app.R
import com.inventory.app.ui.theme.appColors
import com.inventory.app.ui.theme.CardStyle
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.ProgressStyle
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
        enter = fadeIn(animationSpec = tween(PaperInkMotion.DurationMedium)) +
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
        enter = fadeIn(tween(PaperInkMotion.DurationLong)) + scaleIn(tween(PaperInkMotion.DurationLong), initialScale = 0.8f)
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
        animationSpec = tween(PaperInkMotion.DurationMedium),
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
                ThemedIcon(
                    materialIcon = Icons.Filled.KeyboardArrowDown,
                    inkIconRes = R.drawable.ic_ink_expand,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(PaperInkMotion.DurationMedium)) + fadeIn(tween(PaperInkMotion.DurationMedium)),
            exit = shrinkVertically(animationSpec = tween(PaperInkMotion.DurationMedium)) + fadeOut(tween(PaperInkMotion.DurationMedium))
        ) {
            content()
        }
    }
}

// ─── Themed FAB ───────────────────────────────────────────────────

/**
 * Drop-in replacement for [FloatingActionButton] that renders as an
 * [InkBorderCard] circle in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [FloatingActionButton].
 */
@Composable
fun ThemedFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable () -> Unit
) {
    val isInk = MaterialTheme.visuals.isInk
    if (isInk) {
        InkBorderCard(
            modifier = modifier.size(InkTokens.fabSize),
            containerColor = MaterialTheme.colorScheme.primary,
            inkBorder = CardStyle.InkBorder(
                wobbleAmplitude = InkTokens.wobbleMedium,
                strokeWidth = InkTokens.strokeBold,
                segments = 6
            ),
            cornerRadius = InkTokens.fabSize / 2,
            onClick = onClick
        ) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                content()
            }
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = containerColor
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
    val bottomNavHeight = LocalBottomNavHeight.current
    val fabOffset = if (bottomNavHeight > 0.dp) -(bottomNavHeight + 24.dp) else 0.dp
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        ThemedFab(
            onClick = onClick,
            modifier = Modifier.offset(y = fabOffset)
        ) {
            icon()
        }
    }
}

// ─── Dashboard Greeting ─────────────────────────────────────────────────

@Composable
fun DashboardGreeting(
    totalItems: Int,
    expiringSoon: Int,
    userPreference: String = "INVENTORY",
    modifier: Modifier = Modifier
) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val prefTagline = when (userPreference) {
        "WASTE" -> "Let's keep things fresh"
        "COOK" -> "Ready to cook something great?"
        else -> "Here's your kitchen at a glance"
    }
    val subtitle = when {
        totalItems == 0 -> "Start by adding your first item"
        expiringSoon > 0 -> "$prefTagline \u2014 $expiringSoon item${if (expiringSoon != 1) "s" else ""} expiring soon"
        else -> "$prefTagline \u2014 tracking $totalItems item${if (totalItems != 1) "s" else ""}"
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(PaperInkMotion.DurationLong)),
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
        animationSpec = tween(PaperInkMotion.DurationMedium),
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
        ThemedProgressBar(
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
    val isInk = MaterialTheme.visuals.isInk

    val animatedContent: @Composable () -> Unit = {
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
                        ThemedCircularProgress(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current
                        )
                        Text("Saving...")
                    }
                }
                "saved" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedIcon(materialIcon = Icons.Filled.Check, inkIconRes = R.drawable.ic_ink_check, contentDescription = "Saved", modifier = Modifier.size(18.dp))
                        Text("Saved!")
                    }
                }
                else -> Text(text)
            }
        }
    }

    if (!isInk) {
        Button(
            onClick = { if (!isLoading && !isSaved) onClick() },
            modifier = modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            animatedContent()
        }
    } else {
        // Ink mode: wobbly pill border + ink wash fill (matches ThemedButton)
        val colorScheme = MaterialTheme.colorScheme
        val density = LocalDensity.current
        val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
        val strokePx = with(density) { InkTokens.strokeMedium.toPx() }
        val wobblePx = with(density) { InkTokens.wobbleSmall.toPx() }

        val enabled = !isLoading
        val borderColor = if (enabled) colorScheme.primary
            else colorScheme.onSurface.copy(alpha = InkTokens.disabledBorder)
        val fillColor = if (enabled) colorScheme.primary.copy(alpha = InkTokens.fillLight)
            else Color.Transparent
        val contentColor = if (enabled) colorScheme.primary
            else colorScheme.onSurface.copy(alpha = InkTokens.disabledContent)

        Box(
            modifier = modifier
                .fillMaxWidth()
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
                    onClick = { if (!isLoading && !isSaved) onClick() }
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                animatedContent()
            }
        }
    }
}

// ─── Top-Bar Save Action (Reusable for all form screens) ────────────────

/**
 * Ink-blot save action for the [PageScaffold] `actions` slot.
 *
 * **Paper & Ink entrance**: tiny ink dot → organic ink splat with irregular edges →
 * settles into a clean filled circle with a cream checkmark. On save success the
 * checkmark bounces with sparkle dots, the circle shifts to olive green, then
 * shrinks back to a dot and disappears.
 *
 * **Clean theme**: standard filled circle with checkmark, simple scale entrance.
 * **Reduce motion**: instant show/hide, no bloom.
 *
 * @param visible Whether the action should be shown (typically `isDirty || isSaving || isSaved`).
 * @param onClick Callback when the user taps save.
 * @param isLoading Show spinner instead of checkmark.
 * @param isSaved Show success-tinted checkmark.
 */
@Composable
fun SaveAction(
    visible: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isSaved: Boolean = false
) {
    val reduceMotion = com.inventory.app.ui.theme.LocalReduceMotion.current
    val isInk = MaterialTheme.visuals.isInk
    val colorScheme = MaterialTheme.colorScheme
    val colors = MaterialTheme.appColors

    if (isInk) {
        InkBlotSaveAction(
            visible = visible,
            onClick = onClick,
            isLoading = isLoading,
            isSaved = isSaved,
            reduceMotion = reduceMotion,
            inkColor = colors.saveActionIdle,
            savedColor = colors.saveActionSaved,
            checkColor = colors.saveActionCheck,
            spinnerColor = colors.saveActionCheck
        )
    } else {
        CleanSaveAction(
            visible = visible,
            onClick = onClick,
            isLoading = isLoading,
            isSaved = isSaved,
            reduceMotion = reduceMotion,
            primaryColor = colors.saveActionIdle,
            onPrimaryColor = colors.saveActionCheck,
            successColor = colors.saveActionSaved,
            spinnerColor = colorScheme.onSurface
        )
    }
}

// ─── Clean Theme Save Action ─────────────────────────────────────────────

@Composable
private fun CleanSaveAction(
    visible: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean,
    isSaved: Boolean,
    reduceMotion: Boolean,
    primaryColor: Color,
    onPrimaryColor: Color,
    successColor: Color,
    spinnerColor: Color
) {
    val targetAlpha = if (visible) 1f else 0f
    val targetScale = if (visible) 1f else 0.6f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (reduceMotion) snap() else tween(PaperInkMotion.DurationEntry),
        label = "cleanSaveAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (reduceMotion) snap() else PaperInkMotion.BouncySpring,
        label = "cleanSaveScale"
    )

    if (alpha > 0.01f) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            val bgColor by animateColorAsState(
                targetValue = if (isSaved) successColor else primaryColor,
                animationSpec = tween(PaperInkMotion.DurationMedium),
                label = "cleanSaveBg"
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .then(
                        if (!isLoading && !isSaved) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true, radius = 20.dp),
                                onClick = onClick
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    ThemedCircularProgress(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = onPrimaryColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = if (isSaved) "Saved" else "Save",
                        tint = onPrimaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─── Ink Blot Save Action (Paper & Ink theme) ────────────────────────────

/**
 * Generates a noisy radius for the ink splat shape.
 * Uses layered sine waves to create organic, irregular edges.
 *
 * @param angle Angle around the circle (0..2π)
 * @param baseRadius The target clean circle radius
 * @param noiseAmount How much irregularity (0 = perfect circle, 1 = very blobby)
 * @param seed Random offset so each instance looks different
 */
private fun inkSplatRadius(
    angle: Float,
    baseRadius: Float,
    noiseAmount: Float,
    seed: Float
): Float {
    if (noiseAmount < 0.001f) return baseRadius

    // Layer multiple sine waves at different frequencies for organic shape
    val wave1 = sin(angle * 3f + seed * 1.7f) * 0.35f
    val wave2 = sin(angle * 5f + seed * 2.3f) * 0.25f
    val wave3 = sin(angle * 7f + seed * 3.1f) * 0.15f
    val wave4 = sin(angle * 2f + seed * 0.9f) * 0.25f

    val totalNoise = (wave1 + wave2 + wave3 + wave4) * noiseAmount
    return baseRadius * (1f + totalNoise)
}

/**
 * Draws an ink splat path — a circle with noisy radius perturbations.
 */
private fun DrawScope.drawInkSplat(
    center: Offset,
    baseRadius: Float,
    noiseAmount: Float,
    seed: Float,
    color: Color
) {
    val path = Path()
    val steps = 72 // Smooth enough for organic edges

    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
        val r = inkSplatRadius(angle, baseRadius, noiseAmount, seed)
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Fill)
}

/**
 * Draws sparkle dots around the checkmark (success celebration).
 */
private fun DrawScope.drawSparkleDots(
    center: Offset,
    radius: Float,
    alpha: Float,
    color: Color
) {
    if (alpha < 0.01f) return
    val dotRadius = radius * 0.055f
    // 6 dots at asymmetric positions — 3 upper-left, 3 lower-right (matching video)
    val positions = listOf(
        // Upper-left cluster
        Offset(-0.55f, -0.65f),
        Offset(-0.35f, -0.80f),
        Offset(-0.65f, -0.40f),
        // Lower-right cluster
        Offset(0.60f, 0.50f),
        Offset(0.45f, 0.70f),
        Offset(0.75f, 0.35f)
    )
    for (pos in positions) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = dotRadius,
            center = Offset(
                center.x + pos.x * radius,
                center.y + pos.y * radius
            )
        )
    }
}

/**
 * Draws a simple checkmark path.
 */
private fun DrawScope.drawCheckmark(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
    rotation: Float = 0f
) {
    if (alpha < 0.01f) return
    val checkSize = radius * 0.45f
    val strokeW = radius * 0.12f

    rotate(rotation, pivot = center) {
        val path = Path().apply {
            // Checkmark: short arm then long arm
            moveTo(center.x - checkSize * 0.6f, center.y + checkSize * 0.05f)
            lineTo(center.x - checkSize * 0.1f, center.y + checkSize * 0.55f)
            lineTo(center.x + checkSize * 0.7f, center.y - checkSize * 0.45f)
        }
        drawPath(
            path,
            color = color.copy(alpha = alpha),
            style = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private enum class InkSavePhase {
    HIDDEN,     // Not visible
    BLOOMING,   // Ink dot → splat → circle
    IDLE,       // Settled circle with checkmark (dirty state)
    LOADING,    // Spinner
    SUCCESS,    // Checkmark bounce + sparkles + color shift
    EXITING     // Shrink to dot → gone
}

@Composable
private fun InkBlotSaveAction(
    visible: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean,
    isSaved: Boolean,
    reduceMotion: Boolean,
    inkColor: Color,
    savedColor: Color,
    checkColor: Color,
    spinnerColor: Color
) {
    val density = LocalDensity.current
    val canvasSize = 52.dp
    val canvasSizePx = with(density) { canvasSize.toPx() }
    val fullRadius = canvasSizePx * 0.44f // ~46dp diameter circle

    // Seed for unique splat shape per composition
    val splatSeed = remember { (System.nanoTime() % 1000).toFloat() / 100f }

    // ─── Animatables ─────────────────────────────────────────────────────
    // Radius: 0 → fullRadius (controls dot → circle size)
    val radiusAnim = remember { Animatable(0f) }
    // Noise: 1 → 0 (controls blobby → clean circle)
    val noiseAnim = remember { Animatable(0f) }
    // Checkmark alpha: 0 → 1
    val checkAlpha = remember { Animatable(0f) }
    // Checkmark rotation for success bounce
    val checkRotation = remember { Animatable(0f) }
    // Sparkle alpha
    val sparkleAlpha = remember { Animatable(0f) }
    // Color shift (0 = ink, 1 = olive green)
    val colorShift = remember { Animatable(0f) }
    // Overall alpha for final fade-out
    val overallAlpha = remember { Animatable(0f) }

    // Track phase
    var phase by remember { mutableStateOf(InkSavePhase.HIDDEN) }
    var wasVisible by remember { mutableStateOf(false) }
    var wasSaved by remember { mutableStateOf(false) }

    // ─── Phase transitions ───────────────────────────────────────────────

    // Entrance: HIDDEN → BLOOMING → IDLE
    LaunchedEffect(visible) {
        if (visible && !wasVisible) {
            phase = InkSavePhase.BLOOMING
            if (reduceMotion) {
                // Instant show
                overallAlpha.snapTo(1f)
                radiusAnim.snapTo(fullRadius)
                noiseAnim.snapTo(0f)
                checkAlpha.snapTo(1f)
                phase = InkSavePhase.IDLE
            } else {
                overallAlpha.snapTo(1f)
                // Phase 1: Tiny dot appears (0 → 6dp worth)
                launch { radiusAnim.animateTo(fullRadius * 0.12f, tween(120)) }
                delay(120)

                // Phase 2: Ink splat bloom — grow with high noise
                launch { noiseAnim.snapTo(1f) }
                launch {
                    radiusAnim.animateTo(
                        fullRadius * 1.15f, // Overshoot past final size
                        tween(280, easing = FastOutSlowInEasing)
                    )
                }
                delay(280)

                // Phase 3: Settle — noise reduces, radius springs to final
                launch {
                    noiseAnim.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
                }
                launch {
                    radiusAnim.animateTo(
                        fullRadius,
                        spring(dampingRatio = 0.5f, stiffness = 200f)
                    )
                }
                // Checkmark fades in once settling begins
                delay(100)
                launch {
                    checkAlpha.animateTo(1f, tween(200))
                }
                delay(250) // Wait for settle to finish
                phase = InkSavePhase.IDLE
            }
        } else if (!visible && wasVisible) {
            // Exit: shrink to dot and disappear
            if (phase != InkSavePhase.EXITING) {
                phase = InkSavePhase.EXITING
                if (reduceMotion) {
                    overallAlpha.snapTo(0f)
                    radiusAnim.snapTo(0f)
                    checkAlpha.snapTo(0f)
                    colorShift.snapTo(0f)
                    sparkleAlpha.snapTo(0f)
                    noiseAnim.snapTo(0f)
                    phase = InkSavePhase.HIDDEN
                } else {
                    launch { checkAlpha.animateTo(0f, tween(150)) }
                    launch { sparkleAlpha.animateTo(0f, tween(100)) }
                    delay(100)
                    launch {
                        radiusAnim.animateTo(fullRadius * 0.06f, tween(400, easing = FastOutSlowInEasing))
                    }
                    delay(350)
                    launch { overallAlpha.animateTo(0f, tween(100)) }
                    delay(100)
                    // Reset for next entrance
                    radiusAnim.snapTo(0f)
                    colorShift.snapTo(0f)
                    noiseAnim.snapTo(0f)
                    checkRotation.snapTo(0f)
                    phase = InkSavePhase.HIDDEN
                }
            }
        }
        wasVisible = visible
    }

    // Success: checkmark bounce + sparkles + color shift → then auto-exit
    LaunchedEffect(isSaved) {
        if (isSaved && !wasSaved && (phase == InkSavePhase.IDLE || phase == InkSavePhase.LOADING)) {
            phase = InkSavePhase.SUCCESS
            if (reduceMotion) {
                colorShift.snapTo(1f)
            } else {
                // Checkmark bounce with rotation
                launch {
                    checkRotation.animateTo(-12f, tween(100))
                    checkRotation.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 300f))
                }
                // Sparkle dots
                launch {
                    sparkleAlpha.animateTo(1f, tween(200))
                    delay(600)
                    sparkleAlpha.animateTo(0f, tween(300))
                }
                // Color shift brown → olive green
                delay(200)
                launch {
                    colorShift.animateTo(1f, tween(500))
                }
            }
        }
        wasSaved = isSaved
    }

    // Loading phase
    LaunchedEffect(isLoading) {
        if (isLoading && phase == InkSavePhase.IDLE) {
            phase = InkSavePhase.LOADING
        } else if (!isLoading && phase == InkSavePhase.LOADING) {
            phase = InkSavePhase.IDLE
        }
    }

    // Loading spinner rotation
    val infiniteTransition = rememberInfiniteTransition(label = "inkSaveSpinner")
    val spinnerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinnerAngle"
    )

    // ─── Render ──────────────────────────────────────────────────────────

    if (overallAlpha.value > 0.01f || visible) {
        val currentColor = lerp(inkColor, savedColor, colorShift.value)

        Box(
            modifier = Modifier
                .size(canvasSize)
                .semantics { contentDescription = if (isSaved) "Saved" else "Save" }
                .graphicsLayer { alpha = overallAlpha.value }
                .pointerInput(phase) {
                    if (phase == InkSavePhase.IDLE) {
                        detectTapGestures { onClick() }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val currentRadius = radiusAnim.value
                val currentNoise = noiseAnim.value

                if (currentRadius > 0.5f) {
                    // Draw the ink blot / circle
                    if (currentNoise > 0.01f) {
                        drawInkSplat(center, currentRadius, currentNoise, splatSeed, currentColor)
                    } else {
                        drawCircle(currentColor, currentRadius, center)
                    }

                    // Draw sparkle dots (success phase)
                    drawSparkleDots(center, currentRadius, sparkleAlpha.value, checkColor)

                    // Draw checkmark or spinner
                    if (phase == InkSavePhase.LOADING) {
                        // Draw arc spinner
                        val strokeW = currentRadius * 0.10f
                        drawArc(
                            color = spinnerColor,
                            startAngle = spinnerAngle,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - currentRadius * 0.4f,
                                center.y - currentRadius * 0.4f
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                currentRadius * 0.8f,
                                currentRadius * 0.8f
                            ),
                            style = Stroke(
                                width = strokeW,
                                cap = StrokeCap.Round
                            )
                        )
                    } else {
                        drawCheckmark(
                            center = center,
                            radius = currentRadius,
                            color = checkColor,
                            alpha = checkAlpha.value,
                            rotation = checkRotation.value
                        )
                    }
                }
            }
        }
    }
}

/**
 * Linearly interpolate between two colors.
 */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
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
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = PaperInkMotion.WobblySpring
                )
            }
            InkDotState.COMPLETED -> {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = PaperInkMotion.SettleSpring
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

// ─── Ruled Lines Background ─────────────────────────────────────────────

/**
 * Paper & Ink ruled-lines background — faint horizontal lines like notebook paper.
 * Shared composable to replace inline Canvas blocks across onboarding + dashboard.
 */
@Composable
fun RuledLinesBackground(
    modifier: Modifier = Modifier,
    spacing: Dp = 24.dp,
    alpha: Float = 0.05f
) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
    Canvas(modifier = modifier) {
        val spacingPx = spacing.toPx()
        var y = spacingPx
        while (y < size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1f)
            y += spacingPx
        }
    }
}

// ─── Ink Hatched Progress Bar ────────────────────────────────────────────

/**
 * Hand-scratched progress bar with diagonal hatch strokes (////).
 * Each stroke has slight wobble for an organic, hand-drawn feel.
 * Used automatically by [ThemedProgressBar] when Paper & Ink is active.
 */
@Composable
fun InkHatchedProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.15f),
    style: ProgressStyle.InkHatched = ProgressStyle.InkHatched(),
) {
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }

    Canvas(modifier = modifier.height(4.dp)) {
        val fillWidth = size.width * progress().coerceIn(0f, 1f)
        drawInkHatchedBar(
            fillWidth = fillWidth,
            color = color,
            trackColor = trackColor,
            style = style,
            wobbleSeed = wobbleSeed,
        )
    }
}

/**
 * Theme-aware progress bar. Delegates to:
 * - [InkHatchedProgressBar] when Paper & Ink is active (reads params from [ProgressStyle])
 * - [LinearProgressIndicator] when Modern/Standard is active
 *
 * Drop-in replacement for LinearProgressIndicator.
 */
@Composable
fun ThemedProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.15f),
) {
    when (val style = MaterialTheme.visuals.progressStyle) {
        is ProgressStyle.InkHatched -> {
            InkHatchedProgressBar(
                progress = progress,
                modifier = modifier,
                color = color,
                trackColor = trackColor,
                style = style,
            )
        }
        is ProgressStyle.Standard -> {
            LinearProgressIndicator(
                progress = progress,
                modifier = modifier,
                color = color,
                trackColor = trackColor,
            )
        }
    }
}
