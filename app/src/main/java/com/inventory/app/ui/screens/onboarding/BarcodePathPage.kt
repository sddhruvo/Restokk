package com.inventory.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.BarcodeCameraPreview
import com.inventory.app.ui.components.ThemedCircularProgress
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay

private val GentleSpring = PaperInkMotion.GentleSpring

/**
 * Barcode path — inline barcode scanner with guided text, lookup indicator,
 * and "not found" fallback to Type path.
 */
@Composable
internal fun BarcodePathPage(
    isLookingUp: Boolean,
    barcodeNotFound: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onFallbackToType: () -> Unit,
    onScanAgain: () -> Unit
) {
    // ── Entrance animation ──
    var headerReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        headerReady = true
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (headerReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "barcHeaderAlpha"
    )
    val headerY by animateFloatAsState(
        targetValue = if (headerReady) 0f else -16f,
        animationSpec = GentleSpring, label = "barcHeaderY"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ──
        Text(
            text = "Point at any barcode",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 32.dp, start = 32.dp, end = 32.dp)
                .graphicsLayer { alpha = headerAlpha; translationY = headerY }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll look it up for you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer { alpha = headerAlpha }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Camera preview ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!barcodeNotFound) {
                BarcodeCameraPreview(
                    onBarcodeDetected = onBarcodeDetected,
                    modifier = Modifier.fillMaxSize(),
                    showOverlay = true,
                    showTorchButton = true
                )

                // ── Looking up overlay ──
                if (isLookingUp) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ThemedCircularProgress(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Looking it up...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // ── Not found state ──
                NotFoundContent(
                    onScanAgain = onScanAgain,
                    onFallbackToType = onFallbackToType
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NotFoundContent(
    onScanAgain: () -> Unit,
    onFallbackToType: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(PaperInkMotion.DurationMedium)) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
            initialOffsetY = { it / 4 }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "That's a new one!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We couldn't find this barcode\nin our database.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Try another barcode")
            }

            Spacer(modifier = Modifier.height(12.dp))

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
}
