package com.inventory.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inventory.app.ui.theme.CardStyle
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals

/**
 * Drop-in replacement for [AlertDialog] that uses an [InkBorderCard] container
 * with a [PaperInkMotion.BouncySpring] entrance in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [AlertDialog].
 */
@Composable
fun ThemedAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
        )
        return
    }

    val reduceMotion = LocalReduceMotion.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else PaperInkMotion.DialogEnterScale,
        animationSpec = if (reduceMotion) tween(0) else PaperInkMotion.BouncySpring,
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(PaperInkMotion.DurationShort),
        label = "dialogAlpha"
    )

    // INK-2: Use platform-default-width=false + custom scrim Box
    // Color.Black guarantees dark dimming in all color palettes
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Full-screen scrim layer — tap outside card to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = InkTokens.scrimDialog))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            // INK-1: Opaque surface fill — content never bleeds through
            InkBorderCard(
                modifier = modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .widthIn(min = 280.dp, max = 560.dp)
                    .fillMaxWidth(0.9f)
                    // Consume click so tapping the card doesn't dismiss
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    ),
                containerColor = MaterialTheme.colorScheme.surface,
                inkBorder = CardStyle.InkBorder(
                    wobbleAmplitude = InkTokens.wobbleMedium
                ),
                cornerRadius = 20.dp
            ) {
                // INK-3: Increased padding from 24dp → 28dp for breathing room
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = if (icon != null) Alignment.CenterHorizontally
                        else Alignment.Start
                ) {
                    icon?.let {
                        it()
                        Spacer(Modifier.height(16.dp))
                    }
                    title?.let {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            it()
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    text?.let {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            Box(modifier = Modifier.fillMaxWidth()) { it() }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    }
}
