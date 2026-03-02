package com.inventory.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import com.inventory.app.R
import com.inventory.app.ui.theme.IconStyle
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.LocalThemeVisuals
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.launch
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════
// Layer 2: "Life" — Idle Breathing
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Breathing budget — max [PaperInkMotion.MaxSimultaneousBreathing] icons breathe at once.
 * Icons acquire a slot on mount, release on dispose. Off-screen LazyColumn items auto-release.
 */
class BreathingBudget {
    private val _count = mutableIntStateOf(0)
    fun tryAcquire(): Boolean {
        if (_count.intValue >= PaperInkMotion.MaxSimultaneousBreathing) return false
        _count.intValue++
        return true
    }
    fun release() {
        _count.intValue = (_count.intValue - 1).coerceAtLeast(0)
    }
}

val LocalBreathingBudget = staticCompositionLocalOf { BreathingBudget() }

/** Breathing personality — each gives icons a unique subtle idle motion. */
enum class InkPersonality(
    val scaleMin: Float = 1f,
    val scaleMax: Float = 1f,
    val translateYMin: Float = 0f,
    val translateYMax: Float = 0f,
    val rotationMin: Float = 0f,
    val rotationMax: Float = 0f,
    val periodMs: Int = PaperInkMotion.BreathePeriodDefault
) {
    HEARTBEAT(scaleMin = 1f, scaleMax = 1.015f, periodMs = 2000),
    SWAY(rotationMin = -0.5f, rotationMax = 0.5f, periodMs = 3000),
    SIMMER(scaleMin = 1f, scaleMax = 1.01f, translateYMin = 0f, translateYMax = -0.3f, periodMs = 2500),
    TREMBLE(translateYMin = -0.3f, translateYMax = 0.3f, rotationMin = -0.3f, rotationMax = 0.3f, periodMs = 1800),
    SETTLE(translateYMin = 0f, translateYMax = -0.4f, periodMs = 3200),
    TICK(rotationMin = -0.5f, rotationMax = 0.5f, periodMs = 2000),
    BREATHE(scaleMin = 1f, scaleMax = 1.015f, periodMs = 2500);
}

/**
 * Modifier that adds subtle idle breathing animation in Paper & Ink mode.
 * Respects [BreathingBudget] (max 3 simultaneous), reduce-motion, and theme.
 *
 * Each instance's period varies ±15% to prevent synchronized robotic motion.
 */
@Suppress("UnnecessaryComposedModifier")
fun Modifier.inkBreathe(personality: InkPersonality = InkPersonality.BREATHE): Modifier = composed {
    val visuals = LocalThemeVisuals.current
    val reduceMotion = LocalReduceMotion.current
    val isInk = visuals.iconStyle == IconStyle.CUSTOM_SKETCH

    if (!isInk || reduceMotion) return@composed this

    val budget = LocalBreathingBudget.current
    val acquired = remember { mutableIntStateOf(0) } // 1 = acquired

    DisposableEffect(Unit) {
        if (budget.tryAcquire()) acquired.intValue = 1
        onDispose { if (acquired.intValue == 1) budget.release() }
    }

    if (acquired.intValue == 0) return@composed this

    // Period varies ±15% per instance
    val periodMultiplier = remember { Random.nextFloat() * 0.3f + 0.85f }
    val period = (personality.periodMs * periodMultiplier).toInt()

    val transition = rememberInfiniteTransition(label = "inkBreathe")

    val scale = if (personality.scaleMin != personality.scaleMax) {
        transition.animateFloat(
            initialValue = personality.scaleMin,
            targetValue = personality.scaleMax,
            animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "breatheScale"
        )
    } else null

    val translateY = if (personality.translateYMin != personality.translateYMax) {
        transition.animateFloat(
            initialValue = personality.translateYMin,
            targetValue = personality.translateYMax,
            animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "breatheTranslateY"
        )
    } else null

    val rotation = if (personality.rotationMin != personality.rotationMax) {
        transition.animateFloat(
            initialValue = personality.rotationMin,
            targetValue = personality.rotationMax,
            animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "breatheRotation"
        )
    } else null

    this.graphicsLayer {
        scale?.let { scaleX = it.value; scaleY = it.value }
        translateY?.let { translationY = it.value }
        rotation?.let { rotationZ = it.value }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// Layer 3: "Response" — Touch Reaction
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Modifier that adds press-into-paper reaction on tap.
 * Scale down on press, spring back with overshoot on release. Light haptic on press.
 * Respects ink theme gating; reduce-motion skips overshoot but keeps haptic.
 */
@Suppress("UnnecessaryComposedModifier")
fun Modifier.inkPress(): Modifier = composed {
    val visuals = LocalThemeVisuals.current
    val reduceMotion = LocalReduceMotion.current
    val isInk = visuals.iconStyle == IconStyle.CUSTOM_SKETCH
    if (!isInk) return@composed this

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    this
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        scale.animateTo(
                            PaperInkMotion.PressScale,
                            tween(PaperInkMotion.PressDuration, easing = EaseOutCubic)
                        )
                    }
                    tryAwaitRelease()
                    scope.launch {
                        if (reduceMotion) {
                            scale.snapTo(1f)
                        } else {
                            scale.animateTo(1f, PaperInkMotion.WobblySpring)
                        }
                    }
                }
            )
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}


// ═══════════════════════════════════════════════════════════════════════════
// Layer 4: "Empathy" — Data-Driven Animation
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Heart icon whose breathing rate reflects the health score.
 * Lower score → faster, larger heartbeat. Always runs (communicates data).
 * Simplified to static scale when reduce-motion is on.
 */
@Composable
fun EmpathyHeartIcon(
    healthScore: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val visuals = LocalThemeVisuals.current
    val reduceMotion = LocalReduceMotion.current
    val isInk = visuals.iconStyle == IconStyle.CUSTOM_SKETCH

    // Determine breathing parameters based on score
    val period: Int
    val scaleMax: Float
    when {
        healthScore >= 90 -> { period = 2500; scaleMax = 1.015f }
        healthScore >= 60 -> { period = 2000; scaleMax = 1.02f }
        healthScore >= 30 -> { period = 1500; scaleMax = 1.025f }
        else -> { period = 1000; scaleMax = 1.03f }
    }

    val animatedScale: Float
    if (reduceMotion) {
        // Static scale proportional to urgency (no pulsing)
        animatedScale = scaleMax
    } else {
        val transition = rememberInfiniteTransition(label = "empathyHeart")
        val s by transition.animateFloat(
            initialValue = 1f,
            targetValue = scaleMax,
            animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "heartScale"
        )
        animatedScale = s
    }

    if (isInk) {
        Icon(
            painter = painterResource(R.drawable.ic_ink_heart),
            contentDescription = "Home Score",
            modifier = modifier.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale },
            tint = tint
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Home Score",
            modifier = modifier.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale },
            tint = tint
        )
    }
}

/**
 * Warning icon whose pulse intensity reflects expiring item count.
 * More items → faster alpha pulse + rotation wobble. Always runs.
 */
@Composable
fun EmpathyWarningIcon(
    expiringCount: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val visuals = LocalThemeVisuals.current
    val reduceMotion = LocalReduceMotion.current
    val isInk = visuals.iconStyle == IconStyle.CUSTOM_SKETCH

    val period: Int
    val alphaMin: Float
    val wobble: Float
    when {
        expiringCount == 0 -> { period = 0; alphaMin = 1f; wobble = 0f }
        expiringCount <= 2 -> { period = 2000; alphaMin = 0.85f; wobble = 0f }
        expiringCount <= 5 -> { period = 1500; alphaMin = 0.7f; wobble = 1f }
        else -> { period = 1000; alphaMin = 0.6f; wobble = 2f }
    }

    val animatedAlpha: Float
    val animatedRotation: Float
    if (period == 0 || reduceMotion) {
        // Static — wobble angle conveys urgency without animation
        animatedAlpha = 1f
        animatedRotation = if (reduceMotion && wobble > 0) wobble * 0.5f else 0f
    } else {
        val transition = rememberInfiniteTransition(label = "empathyWarning")
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = alphaMin,
            animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "warningAlpha"
        )
        val r by if (wobble > 0f) {
            transition.animateFloat(
                initialValue = -wobble,
                targetValue = wobble,
                animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
                label = "warningRotation"
            )
        } else {
            remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        }
        animatedAlpha = a
        animatedRotation = r
    }

    if (isInk) {
        Icon(
            painter = painterResource(R.drawable.ic_ink_warning),
            contentDescription = "Expiring Soon",
            modifier = modifier.graphicsLayer { alpha = animatedAlpha; rotationZ = animatedRotation },
            tint = tint
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Expiring Soon",
            modifier = modifier.graphicsLayer { alpha = animatedAlpha; rotationZ = animatedRotation },
            tint = tint
        )
    }
}

/**
 * Shopping cart icon that tilts based on item count — heavier load = more tilt.
 * Transitions smoothly with GentleSpring on count changes.
 */
@Composable
fun EmpathyCartIcon(
    itemCount: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val visuals = LocalThemeVisuals.current
    val reduceMotion = LocalReduceMotion.current
    val isInk = visuals.iconStyle == IconStyle.CUSTOM_SKETCH

    val targetTilt = when {
        itemCount == 0 -> 0f
        itemCount <= 5 -> 2f
        itemCount <= 10 -> 4f
        else -> 6f
    }

    val animatedTilt by animateFloatAsState(
        targetValue = targetTilt,
        animationSpec = PaperInkMotion.GentleSpring,
        label = "cartTilt"
    )

    // Heavy cart gets slow wobble
    val wobbleRotation: Float
    if (itemCount > 10 && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "empathyCartWobble")
        val w by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2000, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "cartWobble"
        )
        wobbleRotation = w
    } else {
        wobbleRotation = 0f
    }

    val totalRotation = animatedTilt + wobbleRotation

    if (isInk) {
        Icon(
            painter = painterResource(R.drawable.ic_ink_shopping),
            contentDescription = "Shopping",
            modifier = modifier.graphicsLayer { rotationZ = totalRotation },
            tint = tint
        )
    } else {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = "Shopping",
            modifier = modifier.graphicsLayer { rotationZ = totalRotation },
            tint = tint
        )
    }
}

/**
 * Trending-down icon with translateY oscillation + alpha pulse proportional to low stock count.
 */
@Composable
fun EmpathyTrendingIcon(
    lowStockCount: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val visuals = LocalThemeVisuals.current
    val reduceMotion = LocalReduceMotion.current
    val isInk = visuals.iconStyle == IconStyle.CUSTOM_SKETCH

    val period: Int
    val translateYMax: Float
    val alphaMin: Float
    when {
        lowStockCount == 0 -> { period = 0; translateYMax = 0f; alphaMin = 1f }
        lowStockCount <= 3 -> { period = 2500; translateYMax = 0.5f; alphaMin = 0.9f }
        lowStockCount <= 8 -> { period = 1800; translateYMax = 1f; alphaMin = 0.8f }
        else -> { period = 1200; translateYMax = 1.5f; alphaMin = 0.7f }
    }

    val animatedTranslateY: Float
    val animatedAlpha: Float
    if (period == 0 || reduceMotion) {
        animatedTranslateY = if (reduceMotion && translateYMax > 0) translateYMax * 0.5f else 0f
        animatedAlpha = 1f
    } else {
        val transition = rememberInfiniteTransition(label = "empathyTrending")
        val t by transition.animateFloat(
            initialValue = 0f,
            targetValue = translateYMax,
            animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "trendTranslateY"
        )
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = alphaMin,
            animationSpec = infiniteRepeatable(tween(period, easing = EaseOutCubic), RepeatMode.Reverse),
            label = "trendAlpha"
        )
        animatedTranslateY = t
        animatedAlpha = a
    }

    if (isInk) {
        Icon(
            painter = painterResource(R.drawable.ic_ink_trending_down),
            contentDescription = "Low Stock",
            modifier = modifier.graphicsLayer { translationY = animatedTranslateY; alpha = animatedAlpha },
            tint = tint
        )
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = "Low Stock",
            modifier = modifier.graphicsLayer { translationY = animatedTranslateY; alpha = animatedAlpha },
            tint = tint
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// Layer 5: "Surprise" — Easter Eggs
// ═══════════════════════════════════════════════════════════════════════════

/** The target icon for each surprise type — matched by drawable resource. */
enum class SurpriseTarget(@DrawableRes val iconRes: Int) {
    SHOPPING(R.drawable.ic_ink_shopping),
    COOK(R.drawable.ic_ink_cook),
    BOOK(R.drawable.ic_ink_book),
    SPARKLE(R.drawable.ic_ink_sparkle),
    HEART(R.drawable.ic_ink_heart);

    companion object {
        fun fromRes(@DrawableRes res: Int): SurpriseTarget? =
            entries.firstOrNull { it.iconRes == res }
    }
}

enum class SurpriseType(val target: SurpriseTarget) {
    CART_WHEELIE(SurpriseTarget.SHOPPING),
    STEAM_PUFF(SurpriseTarget.COOK),
    PAGE_FLUTTER(SurpriseTarget.BOOK),
    SPARKLE_BURST(SurpriseTarget.SPARKLE),
    HEART_FLUTTER(SurpriseTarget.HEART);
}

/**
 * Manages a single surprise per app session. 5% chance of triggering one.
 * Created once per Activity lifecycle, provided via CompositionLocal.
 */
class SurpriseManager {
    private var triggered = false
    private val type: SurpriseType? =
        if (Random.nextFloat() < PaperInkMotion.SurpriseChance) SurpriseType.entries.random() else null

    fun shouldTrigger(target: SurpriseTarget): SurpriseType? {
        if (triggered || type?.target != target) return null
        triggered = true
        return type
    }
}

val LocalSurpriseManager = staticCompositionLocalOf { SurpriseManager() }
