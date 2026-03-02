package com.inventory.app.ui.screens.items

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.appColors
import com.inventory.app.ui.theme.visuals
import kotlinx.coroutines.delay

private val BouncySpring = PaperInkMotion.BouncySpring

/** Tour step — exposed so ItemFormScreen can highlight fields */
enum class TourStep { BANNER, CATEGORY_LOCATION, EXPIRY, SUMMARY, DONE }

/** Highlight colors — kept for non-composable modifier references */
val TourHighlightGreen = Color(0xFF4CAF50)
val TourHighlightAmber = Color(0xFFFF9800)

/**
 * Modifier that applies a pulsing border highlight to a form field during the tour.
 * The border fades in, pulses gently, then fades when the step changes.
 */
fun Modifier.tourHighlight(
    active: Boolean,
    color: Color
): Modifier = composed {
    if (!active) return@composed this

    val pulseTransition = rememberInfiniteTransition(label = "tourPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(PaperInkMotion.DurationChart),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    this.border(
        width = 2.dp,
        color = color.copy(alpha = pulseAlpha),
        shape = MaterialTheme.shapes.medium
    )
}

/**
 * Non-blocking overlay that shows ink annotation cards next to auto-filled fields.
 * The entire form stays fully interactive — tapping anywhere dismisses the tour
 * without consuming the touch event (the underlying field still receives it).
 *
 * @param scrollState the form's scroll state, used for auto-scrolling to fields
 * @param categoryRowY Y position of Category+Location row within the scrollable content (px)
 * @param expiryRowY Y position of Expiry row within the scrollable content (px)
 * @param scaffoldTopPadding top padding from Scaffold (px) — annotations offset below this
 * @param onStepChanged called when tour step changes, so the form can highlight fields
 * @param onDismiss called when tour finishes or user taps
 */
@Composable
fun SmartDefaultsTourOverlay(
    scrollState: ScrollState,
    categoryRowY: Int,
    expiryRowY: Int,
    scaffoldTopPadding: Int,
    onStepChanged: (TourStep) -> Unit,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(TourStep.BANNER) }
    var dismissed by remember { mutableStateOf(false) }

    // Step animations
    val bannerAlpha = remember { Animatable(0f) }
    val catAlpha = remember { Animatable(0f) }
    val catSlideX = remember { Animatable(-40f) }
    val expiryAlpha = remember { Animatable(0f) }
    val expirySlideX = remember { Animatable(-40f) }
    val summaryAlpha = remember { Animatable(0f) }

    // Sequencer
    LaunchedEffect(Unit) {
        // Wait for layout to settle (positions to be measured)
        delay(400)

        // Step 0: Banner
        onStepChanged(TourStep.BANNER)
        bannerAlpha.animateTo(1f, tween(PaperInkMotion.DurationMedium))
        delay(800)

        // Step 1: Category + Location
        currentStep = TourStep.CATEGORY_LOCATION
        onStepChanged(TourStep.CATEGORY_LOCATION)
        bannerAlpha.animateTo(0f, tween(200))
        // Scroll so category row is visible (aim for ~100px from top of viewport)
        val targetScroll = (categoryRowY - 100).coerceAtLeast(0)
        scrollState.animateScrollTo(targetScroll)
        delay(100)
        catAlpha.animateTo(1f, tween(200))
        catSlideX.animateTo(0f, BouncySpring)
        delay(1500)

        // Step 2: Expiry
        currentStep = TourStep.EXPIRY
        onStepChanged(TourStep.EXPIRY)
        catAlpha.animateTo(0f, tween(200))
        val expiryScroll = (expiryRowY - 100).coerceAtLeast(0)
        scrollState.animateScrollTo(expiryScroll)
        delay(100)
        expiryAlpha.animateTo(1f, tween(200))
        expirySlideX.animateTo(0f, BouncySpring)
        delay(1500)

        // Step 3: Summary
        currentStep = TourStep.SUMMARY
        onStepChanged(TourStep.SUMMARY)
        expiryAlpha.animateTo(0f, tween(200))
        summaryAlpha.animateTo(1f, tween(PaperInkMotion.DurationMedium))
        delay(2000)

        // Done — fade everything out
        summaryAlpha.animateTo(0f, tween(PaperInkMotion.DurationMedium))
        currentStep = TourStep.DONE
        onStepChanged(TourStep.DONE)
        onDismiss()
    }

    // Dismiss on user touch
    LaunchedEffect(dismissed) {
        if (!dismissed) return@LaunchedEffect
        // Fade all out quickly
        bannerAlpha.snapTo(0f)
        catAlpha.snapTo(0f)
        expiryAlpha.snapTo(0f)
        summaryAlpha.snapTo(0f)
        onStepChanged(TourStep.DONE)
        onDismiss()
    }

    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Transparent touch-catcher — does NOT consume the event
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        dismissed = true
                        // Don't consume — let the underlying field receive the tap
                    }
                }
        )

        // Banner annotation (top of form)
        if (currentStep == TourStep.BANNER && bannerAlpha.value > 0f) {
            InkAnnotationCard(
                text = "Let's see what Smart Defaults did",
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = with(density) { scaffoldTopPadding.toDp() } + 8.dp)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = bannerAlpha.value }
            )
        }

        // Category + Location annotation
        if ((currentStep == TourStep.CATEGORY_LOCATION) && catAlpha.value > 0f) {
            val yOffset = with(density) {
                // Position relative to viewport: field Y - scroll position + scaffold top
                ((categoryRowY - scrollState.value + scaffoldTopPadding).toDp())
            }
            InkAnnotationCard(
                text = "Category & storage \u2014 auto-detected",
                accentColor = MaterialTheme.appColors.tourHighlightGreen,
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp)
                    .offset { IntOffset(catSlideX.value.toInt(), 0) }
                    .offset(y = yOffset + 64.dp) // below the field
                    .graphicsLayer { alpha = catAlpha.value }
            )
        }

        // Expiry annotation
        if ((currentStep == TourStep.EXPIRY) && expiryAlpha.value > 0f) {
            val yOffset = with(density) {
                ((expiryRowY - scrollState.value + scaffoldTopPadding).toDp())
            }
            InkAnnotationCard(
                text = "Shelf life estimate \u2014 tap to adjust",
                accentColor = MaterialTheme.appColors.tourHighlightAmber,
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp)
                    .offset { IntOffset(expirySlideX.value.toInt(), 0) }
                    .offset(y = yOffset + 64.dp)
                    .graphicsLayer { alpha = expiryAlpha.value }
            )
        }

        // Summary annotation (bottom area)
        if (currentStep == TourStep.SUMMARY && summaryAlpha.value > 0f) {
            InkAnnotationCard(
                text = "These get smarter the more you use Restokk",
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
                    .graphicsLayer { alpha = summaryAlpha.value }
            )
        }
    }
}

/**
 * Small floating ink annotation card with a left accent border.
 * Paper & Ink feel: warm background, italic text, rounded corners.
 */
@Composable
private fun InkAnnotationCard(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Left accent border
                drawLine(
                    color = accentColor,
                    start = Offset(0f, 4.dp.toPx()),
                    end = Offset(0f, size.height - 4.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = if (MaterialTheme.visuals.useElevation) 2.dp else 0.dp,
        shadowElevation = if (MaterialTheme.visuals.useElevation) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(4.dp)) // space after accent
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}
