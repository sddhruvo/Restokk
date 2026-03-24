package com.inventory.app.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.domain.model.KitchenStoryMission
import com.inventory.app.domain.model.KitchenStoryState
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ThemedAlertDialog
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI

private val BouncySpring = PaperInkMotion.BouncySpring

// ─── Main Card (Compact 2-line) ─────────────────────────────────────────

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
    var showDismissDialog by rememberSaveable { mutableStateOf(false) }
    var celebrationTriggered by rememberSaveable { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    // Exit state
    var isExiting by remember { mutableStateOf(false) }
    val exitAlpha = remember { Animatable(1f) }

    // Fade-out exit
    LaunchedEffect(isExiting) {
        if (isExiting) {
            exitAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            onExitComplete()
        }
    }

    // All-complete: celebration → fade out
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
    val entranceTranslateY = remember { Animatable(20f) }
    LaunchedEffect(Unit) {
        launch { entranceAlpha.animateTo(1f, tween(400)) }
        entranceTranslateY.animateTo(0f, BouncySpring)
    }

    val currentMission = if (state.currentMissionIndex >= 0)
        state.missions[state.currentMissionIndex] else null

    Box(modifier = modifier) {
        // Card
        val cardModifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entranceAlpha.value * exitAlpha.value
                translationY = entranceTranslateY.value
            }

        if (currentMission?.navTarget != null) {
            AppCard(
                onClick = { onMissionTap(currentMission) },
                modifier = cardModifier
            ) {
                KitchenStoryContent(
                    chapterNumber = state.chapterNumber,
                    missionText = currentMission.text,
                    subtitle = currentMission.dynamicSubtitle,
                    showArrow = true,
                    canDismiss = state.canDismiss,
                    onDismissClick = { showDismissDialog = true }
                )
            }
        } else {
            // All complete or no nav target — non-clickable card
            AppCard(modifier = cardModifier) {
                KitchenStoryContent(
                    chapterNumber = state.chapterNumber,
                    missionText = state.cardSubtitle.ifEmpty { "Your Kitchen is Alive!" },
                    subtitle = null,
                    showArrow = false,
                    canDismiss = state.canDismiss,
                    onDismissClick = { showDismissDialog = true }
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
        ThemedAlertDialog(
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

// ─── Card Content ───────────────────────────────────────────────────────

@Composable
private fun KitchenStoryContent(
    chapterNumber: Int,
    missionText: String,
    subtitle: String?,
    showArrow: Boolean,
    canDismiss: Boolean,
    onDismissClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.spacingLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                Text(
                    "Your Kitchen Story",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Ch. $chapterNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                missionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        if (showArrow) {
            ThemedIcon(
                materialIcon = Icons.Filled.ArrowForward,
                inkIconRes = R.drawable.ic_ink_chevron_right,
                contentDescription = "Go to mission",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        if (canDismiss) {
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.size(32.dp)
            ) {
                ThemedIcon(
                    materialIcon = Icons.Filled.Close,
                    inkIconRes = R.drawable.ic_ink_close,
                    contentDescription = "Dismiss kitchen story",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Completion Celebration ─────────────────────────────────────────────

@Composable
private fun KitchenStoryCompletion() {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(PaperInkMotion.DurationMedium)) }
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
