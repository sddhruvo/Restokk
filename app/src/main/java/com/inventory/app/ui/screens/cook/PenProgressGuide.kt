package com.inventory.app.ui.screens.cook

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val CookAccentGuide = Color(0xFFE85D3A)

// ── Inline section dot (renders inside LazyColumn items) ─────────────────

private enum class DotState { PAST, ACTIVE, VISIBLE, FUTURE }

@Composable
fun InlineSectionDot(
    section: Int,
    currentSection: Int,
    isVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state = when {
        section == currentSection -> DotState.ACTIVE
        isVisible -> DotState.VISIBLE
        section < currentSection -> DotState.PAST
        else -> DotState.FUTURE
    }

    val targetSize = when (state) {
        DotState.PAST -> 6f
        DotState.ACTIVE -> 8f
        DotState.VISIBLE -> 7f
        DotState.FUTURE -> 5f
    }
    val animatedSize by animateFloatAsState(
        targetValue = targetSize,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "dot_size"
    )

    val color = when (state) {
        DotState.PAST -> CookAccentGuide.copy(alpha = 0.5f)
        DotState.ACTIVE -> CookAccentGuide
        DotState.VISIBLE -> CookAccentGuide.copy(alpha = 0.7f)
        DotState.FUTURE -> Color.Gray.copy(alpha = 0.3f)
    }

    // Breathing only for active dot
    val scale = if (state == DotState.ACTIVE) {
        val transition = rememberInfiniteTransition(label = "dot_breathe")
        val breathing by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_breathe_scale"
        )
        breathing
    } else {
        1f
    }

    Canvas(
        modifier = modifier
            .size(12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        val radius = (animatedSize / 2f).dp.toPx()
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

// ── Section tip (subtle text below header, fades based on active state) ──

@Composable
fun SectionTip(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 0.7f else 0f,
        animationSpec = tween(200),
        label = "tip_alpha"
    )
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = 2.dp)
            .graphicsLayer { this.alpha = alpha }
    )
}

// ── Section header with dot (wraps any header content) ───────────────────

@Composable
fun SectionWithDot(
    section: Int,
    currentSection: Int,
    tip: String,
    showGuide: Boolean,
    visibleSections: Set<Int> = emptySet(),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (!showGuide) {
        // Big screen or not scrollable — plain header, no guide elements
        Box(modifier = modifier.fillMaxWidth()) { content() }
        return
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            InlineSectionDot(
                section = section,
                currentSection = currentSection,
                isVisible = section in visibleSections,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)
            )
            content()
        }
        SectionTip(text = tip, isActive = section in visibleSections)
    }
}
