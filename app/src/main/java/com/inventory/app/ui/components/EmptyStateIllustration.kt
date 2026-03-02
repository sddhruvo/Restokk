package com.inventory.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.inventory.app.ui.components.ThemedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.PaperInkMotion

/**
 * Paper & Ink empty-state illustration with floating idle animation,
 * "Land" entrance, and optional CTA button.
 */
@Composable
fun EmptyStateIllustration(
    icon: ImageVector,
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null
) {
    // "Land" entrance: scale 0.5 → 1.0 with BouncySpring
    var landed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { landed = true }

    val landScale by animateFloatAsState(
        targetValue = if (landed) 1f else 0.5f,
        animationSpec = PaperInkMotion.BouncySpring,
        label = "emptyLand"
    )
    val landAlpha by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium),
        label = "emptyFade"
    )

    // Floating idle animation (translateY ±4dp, 3000ms)
    val floating = rememberInfiniteTransition(label = "emptyFloat")
    val floatY by floating.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyFloatY"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .graphicsLayer {
                scaleX = landScale; scaleY = landScale
                alpha = landAlpha
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { translationY = floatY },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (ctaLabel != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            ThemedButton(onClick = onCtaClick) {
                Text(ctaLabel)
            }
        }
    }
}
