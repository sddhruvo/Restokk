package com.inventory.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.inventory.app.ui.theme.alertTitle
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.RuledLinesBackground
import com.inventory.app.ui.components.rememberAiSignInGate
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay

private val BouncySpring = PaperInkMotion.BouncySpring
private val GentleSpring = PaperInkMotion.GentleSpring

/**
 * Act 2 "First Magic" — full page that manages its own sub-screens via AnimatedContent.
 * Reads step from FirstMagicViewModel and dispatches to the right sub-screen.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PathChoicePage(
    viewModel: FirstMagicViewModel,
    onComplete: () -> Unit,
    onBackToYourKitchen: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val aiGate = rememberAiSignInGate()

    // ── Camera permission + launcher ──
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onImageCaptured(bitmap)
        } else {
            // User cancelled the camera — go back to path selection
            viewModel.onCameraRetry()
        }
    }

    // When camera path selected and permission is granted, launch camera
    var pendingCameraLaunch by remember { mutableStateOf(false) }
    LaunchedEffect(state.chosenPath, cameraPermissionState.status.isGranted) {
        if (state.chosenPath == Act2Path.CAMERA && state.step is Act2Step.PathSelection) {
            if (cameraPermissionState.status.isGranted) {
                pendingCameraLaunch = false
                cameraLauncher.launch(null)
            } else if (!pendingCameraLaunch) {
                pendingCameraLaunch = true
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    // Back from sub-steps → PathSelection
    BackHandler(enabled = state.step !is Act2Step.PathSelection) {
        viewModel.onBackFromSubStep()
    }
    // Back from PathSelection → Screen 2
    BackHandler(enabled = state.step is Act2Step.PathSelection) {
        onBackToYourKitchen()
    }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val isForward = when {
                targetState is Act2Step.PathSelection -> false
                initialState is Act2Step.PathSelection -> true
                else -> true
            }
            if (isForward) {
                (slideInHorizontally { it / 3 } + fadeIn(tween(PaperInkMotion.DurationMedium))) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut(tween(PaperInkMotion.DurationMedium)))
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn(tween(PaperInkMotion.DurationMedium))) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut(tween(PaperInkMotion.DurationMedium)))
            }
        },
        modifier = Modifier.fillMaxSize(),
        label = "act2Steps"
    ) { step ->
        when (step) {
            is Act2Step.PathSelection -> PathSelectionContent(
                onPathSelected = { path ->
                    if (path == Act2Path.CAMERA) {
                        aiGate.requireSignIn("identify items from photos") {
                            viewModel.onPathSelected(path)
                        }
                    } else {
                        viewModel.onPathSelected(path)
                    }
                },
                onSkip = { viewModel.onSkip(onComplete) }
            )
            is Act2Step.TypeInput -> TypePathPage(
                typedName = state.typedName,
                smartDefaults = state.smartDefaults,
                onNameChange = { viewModel.onTypedNameChange(it) },
                onSubmit = { viewModel.onSubmitTypedItem() }
            )
            is Act2Step.Reveal -> RevealCard(
                item = step.item,
                isSaving = state.isSaving,
                onSave = { viewModel.onSaveRevealedItem(step.item) },
                onCorrectName = { viewModel.correctItemName(it) }
            )
            is Act2Step.AddMore -> AddMoreContent(
                count = step.count,
                onAddAnother = { viewModel.onAddAnother() },
                onDone = { viewModel.onDoneAdding(onComplete) }
            )
            is Act2Step.MemorySelection -> KitchenMemoryPath(
                memoryItems = state.memoryItems,
                selectedCount = state.memorySelectedCount,
                isSaving = state.isSaving,
                onToggleItem = { viewModel.onToggleMemoryItem(it) },
                onConfirm = { viewModel.onConfirmMemoryItems() },
                onSkip = { viewModel.onSkip(onComplete) }
            )
            is Act2Step.MemoryReveal -> MemoryAggregateReveal(
                items = step.items,
                onRevealComplete = { viewModel.onMemoryRevealComplete() }
            )
            is Act2Step.MemoryCelebration -> MemoryCelebrationScreen(
                itemCount = state.savedItemCount,
                onDone = { viewModel.onMemoryCelebrationDone(onComplete) }
            )
            is Act2Step.CameraProcessing -> CameraProcessingPage(
                phase = state.cameraPhase,
                error = state.cameraError,
                onFallbackToType = { viewModel.onCameraFallbackToType() },
                onRetry = { viewModel.onCameraRetry() }
            )
            is Act2Step.BarcodeScanning -> BarcodePathPage(
                isLookingUp = state.isLookingUpBarcode,
                barcodeNotFound = state.barcodeNotFound,
                onBarcodeDetected = { viewModel.onBarcodeDetected(it) },
                onFallbackToType = { viewModel.onBarcodeFallbackToType() },
                onScanAgain = { viewModel.onBarcodeScanAgain() }
            )
            is Act2Step.TransitionToDashboard -> {
                // Empty — navigation callback already fired
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Path Selection — 2×2 grid of cards
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PathSelectionContent(
    onPathSelected: (Act2Path) -> Unit,
    onSkip: () -> Unit
) {
    val isOnline by com.inventory.app.util.rememberIsOnline()
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val cameraGranted = cameraPermission.status.isGranted
    // Show denied visual only if we've asked before and user denied (shouldShowRationale)
    // or if permission is permanently denied (asked + !rationale on Android 11+)
    val cameraDenied = !cameraGranted && cameraPermission.status.shouldShowRationale
    val reduceMotion = LocalReduceMotion.current

    // ── Orchestrated entrance ──
    var promptReady by remember { mutableStateOf(reduceMotion) }
    var cardsReady by remember { mutableStateOf(reduceMotion) }
    var skipReady by remember { mutableStateOf(reduceMotion) }

    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(200)
        promptReady = true
        delay(400)
        cardsReady = true
        delay(600)
        skipReady = true
    }

    // Prompt: Write-In
    val writeInPx = 20f
    val promptX by animateFloatAsState(
        targetValue = if (promptReady) 0f else -writeInPx,
        animationSpec = BouncySpring, label = "promptX"
    )
    val promptAlpha by animateFloatAsState(
        targetValue = if (promptReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "promptAlpha"
    )

    // Skip link: Fade Up
    val skipY by animateFloatAsState(
        targetValue = if (skipReady) 0f else 12f,
        animationSpec = GentleSpring, label = "skipY"
    )
    val skipAlpha by animateFloatAsState(
        targetValue = if (skipReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "skipAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RuledLinesBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Prompt
            Text(
                text = "Pick up anything from\nyour kitchen. Anything at all.",
                style = MaterialTheme.typography.alertTitle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationX = promptX; alpha = promptAlpha
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2×2 grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PathCard(
                        icon = if (cameraDenied) Icons.Outlined.Lock else Icons.Outlined.CameraAlt,
                        label = "Show me",
                        isRecommended = !cameraDenied,
                        isEnabled = true,
                        hint = when {
                            cameraDenied -> "Needs camera access"
                            !isOnline -> "Needs internet"
                            else -> null
                        },
                        entranceIndex = 0,
                        visible = cardsReady,
                        modifier = Modifier.weight(1f),
                        onClick = { onPathSelected(Act2Path.CAMERA) }
                    )
                    PathCard(
                        icon = Icons.Outlined.QrCodeScanner,
                        label = "Scan it",
                        isRecommended = false,
                        isEnabled = true,
                        hint = if (!isOnline) "Limited offline" else null,
                        entranceIndex = 1,
                        visible = cardsReady,
                        modifier = Modifier.weight(1f),
                        onClick = { onPathSelected(Act2Path.BARCODE) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PathCard(
                        icon = Icons.Outlined.Keyboard,
                        label = "Type it",
                        isRecommended = false,
                        isEnabled = true,

                        entranceIndex = 2,
                        visible = cardsReady,
                        modifier = Modifier.weight(1f),
                        onClick = { onPathSelected(Act2Path.TYPE) }
                    )
                    PathCard(
                        icon = Icons.Outlined.Home,
                        label = "I'm not in\nmy kitchen",
                        isRecommended = false,
                        isEnabled = true,

                        entranceIndex = 3,
                        visible = cardsReady,
                        modifier = Modifier.weight(1f),
                        onClick = { onPathSelected(Act2Path.MEMORY) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Skip link
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .graphicsLayer {
                        translationY = skipY; alpha = skipAlpha
                    }
            ) {
                Text(
                    "Skip for now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Path Card — single card in the 2×2 grid
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PathCard(
    icon: ImageVector,
    label: String,
    isRecommended: Boolean,
    isEnabled: Boolean,
    hint: String? = null,
    entranceIndex: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Staggered Land entrance (70ms apart)
    var landed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(entranceIndex * 70L)
            landed = true
        }
    }
    val landScale by animateFloatAsState(
        targetValue = if (landed) 1f else 0.5f,
        animationSpec = BouncySpring, label = "pathLand$entranceIndex"
    )
    val landAlpha by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = tween(250), label = "pathFade$entranceIndex"
    )

    val disabledAlpha = if (isEnabled) 1f else 0.45f

    Card(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = landScale; scaleY = landScale
                alpha = landAlpha * disabledAlpha
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(36.dp),
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hint != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            // Recommended chip
            if (isRecommended) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(0.dp)
                ) {
                    Text(
                        text = "Best",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Add More screen (after Type path celebration)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun AddMoreContent(
    count: Int,
    onAddAnother: () -> Unit,
    onDone: () -> Unit
) {
    // Auto-transition after 3 items
    if (count >= 3) {
        LaunchedEffect(Unit) {
            delay(800)
            onDone()
        }
    }

    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        contentReady = true
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (contentReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "addMoreFade"
    )
    val riseY by animateFloatAsState(
        targetValue = if (contentReady) 0f else 20f,
        animationSpec = GentleSpring, label = "addMoreRise"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .graphicsLayer { alpha = fadeAlpha; translationY = riseY },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (count >= 3) {
            Text(
                text = "Great start! Let's see your kitchen.",
                style = MaterialTheme.typography.alertTitle,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = "$count of 3",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onAddAnother,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Add another", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onDone) {
                Text(
                    "That's enough for now \u2192",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
