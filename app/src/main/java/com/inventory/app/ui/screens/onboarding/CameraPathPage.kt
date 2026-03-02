package com.inventory.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.RuledLinesBackground
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay
import kotlin.math.sin

private val BouncySpring = PaperInkMotion.BouncySpring
private val GentleSpring = PaperInkMotion.GentleSpring

/**
 * Camera path processing screen — shown while AI analyzes the captured photo.
 * Paper & Ink animated ink dots + status text with timeout escalation.
 */
@Composable
internal fun CameraProcessingPage(
    phase: CameraPhase,
    error: String?,
    onFallbackToType: () -> Unit,
    onRetry: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isFailed = phase == CameraPhase.FAILED
    val reduceMotion = LocalReduceMotion.current

    // ── Entrance animation ──
    var contentReady by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(200)
        contentReady = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "camAlpha"
    )
    val contentY by animateFloatAsState(
        targetValue = if (contentReady) 0f else 20f,
        animationSpec = GentleSpring, label = "camY"
    )

    // ── Pulsing ink dots (3 dots, staggered) — static when reduce-motion ──
    val infiniteTransition = rememberInfiniteTransition(label = "inkDots")
    val dotProgress by if (reduceMotion) {
        remember { mutableStateOf(0.5f) } // static mid-point
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dotPulse"
        )
    }

    // ── Status text based on phase ──
    val statusText = when (phase) {
        CameraPhase.PROCESSING -> "Reading..."
        CameraPhase.SLOW -> "Taking a closer look..."
        CameraPhase.VERY_SLOW -> "Almost there..."
        CameraPhase.FAILED -> error ?: "Something went wrong"
    }

    // ── Buttons appear on failure ──
    var buttonsReady by remember { mutableStateOf(false) }
    LaunchedEffect(isFailed) {
        if (isFailed) {
            if (!reduceMotion) delay(300)
            buttonsReady = true
        } else {
            buttonsReady = false
        }
    }
    val btnAlpha by animateFloatAsState(
        targetValue = if (buttonsReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "failBtnAlpha"
    )
    val btnY by animateFloatAsState(
        targetValue = if (buttonsReady) 0f else 16f,
        animationSpec = GentleSpring, label = "failBtnY"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RuledLinesBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .graphicsLayer { alpha = contentAlpha; translationY = contentY },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Ink dots animation ──
            if (!isFailed) {
                Canvas(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                ) {
                    val dotRadius = 5.dp.toPx()
                    val spacing = 24.dp.toPx()
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    for (i in 0..2) {
                        val dotPhase = (dotProgress + i * 0.33f) % 1f
                        val scale = 0.5f + 0.5f * sin(dotPhase * Math.PI.toFloat())
                        val alpha = 0.3f + 0.7f * scale
                        drawCircle(
                            color = primaryColor.copy(alpha = alpha),
                            radius = dotRadius * scale,
                            center = Offset(centerX + (i - 1) * spacing, centerY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ── Status text ──
            Text(
                text = statusText,
                style = if (isFailed) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isFailed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (!isFailed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Identifying your item...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // ── Failure buttons ──
            if (isFailed && btnAlpha > 0.01f) {
                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = btnAlpha; translationY = btnY
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Try another photo")
                    }

                    ThemedButton(
                        onClick = onFallbackToType,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Type it instead")
                    }
                }
            }

            // ── "Let's try typing?" link for very slow ──
            if (phase == CameraPhase.VERY_SLOW) {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onFallbackToType) {
                    Text(
                        "Let's try typing instead?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
