package com.inventory.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.inventory.app.ui.theme.IconStyle
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.LocalThemeVisuals
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.launch

/**
 * Theme-aware icon that shows custom hand-drawn vector drawables in Paper & Ink mode
 * and standard Material icons in Modern mode.
 *
 * In Paper & Ink mode, icons "sketch in" on first composition — scale + alpha + slight rotation —
 * simulating a pen stroke landing on paper. The animation is skipped when reduce-motion is on,
 * and does NOT replay on back-navigation (composition restored from back stack).
 */
@Composable
fun ThemedIcon(
    materialIcon: ImageVector,
    @DrawableRes inkIconRes: Int = 0,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val visuals = LocalThemeVisuals.current
    val isInk = visuals.iconStyle == IconStyle.CUSTOM_SKETCH && inkIconRes != 0

    if (isInk) {
        val reduceMotion = LocalReduceMotion.current

        // Layer 1: Birth — entrance animation (plays once per composition)
        val entranceScale = remember { Animatable(if (reduceMotion) 1f else 0f) }
        val entranceAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
        val entranceRotation = remember {
            Animatable(if (reduceMotion) 0f else PaperInkMotion.EntranceRotationStart)
        }

        LaunchedEffect(Unit) {
            if (reduceMotion) return@LaunchedEffect
            launch { entranceAlpha.animateTo(1f, tween(PaperInkMotion.EntranceDurationAlpha, easing = EaseOutCubic)) }
            launch { entranceRotation.animateTo(0f, PaperInkMotion.GentleSpring) }
            entranceScale.animateTo(1f, PaperInkMotion.BouncySpring)
        }

        // Layer 5: Surprise hook — checked after entrance settles
        val surpriseManager = LocalSurpriseManager.current
        val surpriseRotation = remember { Animatable(0f) }
        val surpriseTranslateY = remember { Animatable(0f) }

        LaunchedEffect(entranceScale.isRunning) {
            if (entranceScale.isRunning || reduceMotion) return@LaunchedEffect
            val target = SurpriseTarget.fromRes(inkIconRes) ?: return@LaunchedEffect
            val surprise = surpriseManager.shouldTrigger(target) ?: return@LaunchedEffect
            kotlinx.coroutines.delay(PaperInkMotion.SurpriseDelay)
            when (surprise) {
                SurpriseType.CART_WHEELIE -> {
                    launch { surpriseTranslateY.animateTo(-4f, tween(200)) }
                    surpriseRotation.animateTo(-15f, PaperInkMotion.WobblySpring)
                    launch { surpriseTranslateY.animateTo(0f, PaperInkMotion.GentleSpring) }
                    surpriseRotation.animateTo(0f, PaperInkMotion.GentleSpring)
                }
                SurpriseType.STEAM_PUFF -> {
                    // Simple rotation wobble for cook icon
                    surpriseRotation.animateTo(5f, tween(150))
                    surpriseRotation.animateTo(-3f, tween(150))
                    surpriseRotation.animateTo(0f, PaperInkMotion.GentleSpring)
                }
                SurpriseType.PAGE_FLUTTER -> {
                    surpriseRotation.animateTo(3f, tween(100))
                    surpriseRotation.animateTo(-2f, tween(100))
                    surpriseRotation.animateTo(1f, tween(100))
                    surpriseRotation.animateTo(0f, PaperInkMotion.GentleSpring)
                }
                SurpriseType.SPARKLE_BURST -> {
                    // Quick scale pulse via rotation wiggle
                    surpriseRotation.animateTo(4f, tween(100))
                    surpriseRotation.animateTo(-4f, tween(100))
                    surpriseRotation.animateTo(0f, PaperInkMotion.GentleSpring)
                }
                SurpriseType.HEART_FLUTTER -> {
                    launch { surpriseTranslateY.animateTo(-3f, tween(200)) }
                    surpriseRotation.animateTo(6f, tween(150))
                    surpriseRotation.animateTo(-4f, tween(150))
                    launch { surpriseTranslateY.animateTo(0f, PaperInkMotion.GentleSpring) }
                    surpriseRotation.animateTo(0f, PaperInkMotion.GentleSpring)
                }
            }
        }

        Icon(
            painter = painterResource(inkIconRes),
            contentDescription = contentDescription,
            modifier = modifier.graphicsLayer {
                scaleX = entranceScale.value
                scaleY = entranceScale.value
                alpha = entranceAlpha.value
                rotationZ = entranceRotation.value + surpriseRotation.value
                translationY = surpriseTranslateY.value
            },
            tint = tint
        )
    } else {
        Icon(
            imageVector = materialIcon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}
