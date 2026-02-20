package com.inventory.app.ui.screens.recognition

import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.DropdownField
import com.inventory.app.ui.components.InkBloomDot
import com.inventory.app.ui.components.InkDotState
import com.inventory.app.ui.components.InkProgressLine
import com.inventory.app.util.CategoryVisuals
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.inventory.app.ui.components.AnimatedCounter
import com.inventory.app.ui.components.InkFireworks
import com.inventory.app.util.CategoryVisual
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// ── Processing step messages ─────────────────────────────────────────

private data class FridgeProcessingStep(val message: String, val delayMs: Long)

private val fridgeProcessingSteps = listOf(
    FridgeProcessingStep("Scanning the image...", 1500),
    FridgeProcessingStep("Identifying items...", 2000),
    FridgeProcessingStep("Counting quantities...", 2500),
    FridgeProcessingStep("Classifying categories...", 2500),
    FridgeProcessingStep("Final review...", 8000)
)

// ── Area icon helper ─────────────────────────────────────────────────

private fun areaIcon(areaId: String): ImageVector = when (areaId) {
    "fridge_shelves" -> Icons.Filled.Kitchen
    "fridge_door" -> Icons.Filled.DoorFront
    "freezer" -> Icons.Filled.AcUnit
    "pantry" -> Icons.Filled.ViewModule
    "counter" -> Icons.Filled.Countertops
    "spice_rack" -> Icons.Filled.LocalFireDepartment
    else -> Icons.Filled.Kitchen
}

// ── Review data structures ──────────────────────────────────────────

private data class IndexedFridgeItem(val flatIndex: Int, val item: EditableFridgeItem)

private data class ReviewCategoryGroup(
    val categoryName: String,
    val visual: CategoryVisual,
    val items: List<IndexedFridgeItem>
)

private fun groupItemsByCategory(items: List<EditableFridgeItem>): List<ReviewCategoryGroup> {
    return items.mapIndexed { index, item -> IndexedFridgeItem(index, item) }
        .groupBy { it.item.categoryName.ifBlank { "Other" } }
        .map { (categoryName, indexedItems) ->
            ReviewCategoryGroup(
                categoryName = categoryName,
                visual = CategoryVisuals.get(categoryName),
                items = indexedItems
            )
        }
        .sortedByDescending { it.items.size }
}

// ── Pulsing question mark for confidence callouts ───────────────────

@Composable
private fun PulsingQuestionMark(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingQuestion")
    val scale = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "questionScale"
    )
    val alpha = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "questionAlpha"
    )
    Icon(
        Icons.Filled.HelpOutline,
        contentDescription = "Uncertain",
        modifier = modifier
            .size(14.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        tint = color
    )
}

// ── Main Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeScanScreen(
    navController: NavController,
    viewModel: FridgeScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Camera launcher (TakePicturePreview — returns bitmap)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onImageCaptured(bitmap)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                if (bitmap != null) {
                    viewModel.onImageCaptured(bitmap)
                }
            } catch (e: Exception) {
                Log.e("FridgeScan", "Failed to load image from gallery", e)
            }
        }
    }

    // Back handling
    val hasResults = uiState.state is FridgeScanState.Review
    val isInTourIdle = uiState.isInTourMode && uiState.state is FridgeScanState.Idle
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasResults || isInTourIdle) {
        if (hasResults && uiState.isInTourMode) {
            // In tour mode review → go back to area selection
            showDiscardDialog = true
        } else if (hasResults) {
            showDiscardDialog = true
        } else if (isInTourIdle) {
            // In tour Idle (camera not yet taken) → go back to area selection
            viewModel.returnToAreaSelection()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (uiState.isInTourMode) "Return to areas?" else "Discard scan results?") },
            text = { Text(
                if (uiState.isInTourMode) "Going back will discard results for this area. Items saved from previous areas are kept."
                else "Going back will discard all identified items. This cannot be undone."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    if (uiState.isInTourMode) {
                        viewModel.returnToAreaSelection()
                    } else {
                        navController.popBackStack()
                    }
                }) { Text(if (uiState.isInTourMode) "Return to Areas" else "Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep reviewing") }
            }
        )
    }

    // Dynamic title
    val title = when (uiState.state) {
        is FridgeScanState.AreaSelection -> "Kitchen Tour"
        is FridgeScanState.Review -> "Review Items"
        is FridgeScanState.Processing -> {
            uiState.currentArea?.let { "Scanning: ${it.name}" } ?: "Scanning..."
        }
        is FridgeScanState.Idle -> {
            uiState.currentArea?.let { "Scanning: ${it.name}" } ?: "Kitchen Scan"
        }
        is FridgeScanState.TourSummary -> "Tour Complete"
        else -> "Kitchen Scan"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            hasResults -> showDiscardDialog = true
                            isInTourIdle -> viewModel.returnToAreaSelection()
                            else -> navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.state is FridgeScanState.Review) {
                        TextButton(onClick = {
                            cameraLauncher.launch(null)
                        }) {
                            Text("Retake")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState.state) {
            is FridgeScanState.AreaSelection -> {
                AreaSelectionContent(
                    completedAreas = uiState.completedAreas,
                    totalItemsScanned = uiState.allScannedItemNames.size,
                    onSelectArea = { viewModel.selectArea(it) },
                    onQuickScan = { viewModel.quickScan() },
                    onFinishTour = { viewModel.finishTour() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            is FridgeScanState.Idle -> {
                IdleContent(
                    areaName = uiState.currentArea?.name,
                    onTakePhoto = { cameraLauncher.launch(null) },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                )
            }

            is FridgeScanState.Processing -> {
                ProcessingContent(
                    bitmap = uiState.capturedBitmap,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            is FridgeScanState.Review -> {
                ReviewContent(
                    items = state.items,
                    units = uiState.units,
                    categories = uiState.categories,
                    onUpdateName = viewModel::updateItemName,
                    onUpdateQuantity = viewModel::updateItemQuantity,
                    onUpdateUnit = viewModel::updateItemUnit,
                    onUpdateCategory = viewModel::updateItemCategory,
                    onUpdateMatchType = viewModel::updateMatchType,
                    onRemoveItem = viewModel::removeItem,
                    onAddAll = viewModel::addAllToInventory,
                    onRetake = {
                        cameraLauncher.launch(null)
                    },
                    onBackToAreas = { viewModel.returnToAreaSelection() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            is FridgeScanState.Saving -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Adding item ${state.current} of ${state.total}...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            is FridgeScanState.AreaSuccess -> {
                AreaSuccessContent(
                    count = state.count,
                    areaName = state.areaName,
                    isInTourMode = uiState.isInTourMode,
                    completedAreas = uiState.completedAreas,
                    onScanNextArea = { viewModel.continueToNextArea() },
                    onViewSummary = { viewModel.finishTour() },
                    onDone = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                )
            }

            is FridgeScanState.TourSummary -> {
                TourSummaryContent(
                    areaResults = state.areaResults,
                    totalItems = state.totalItems,
                    categoryBreakdown = state.categoryBreakdown,
                    onDone = {
                        viewModel.reset()
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                )
            }

            is FridgeScanState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    "Scan Failed",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Text(
                                state.message,
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Button(
                        onClick = {
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                        Text("Try Again", Modifier.padding(start = 8.dp))
                    }
                    if (uiState.isInTourMode) {
                        OutlinedButton(
                            onClick = { viewModel.returnToAreaSelection() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Return to Areas") }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.reset() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Back") }
                    }
                }
            }
        }
    }
}

// ── Area Selection ───────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreaSelectionContent(
    completedAreas: Map<String, Int>,
    totalItemsScanned: Int,
    onSelectArea: (String) -> Unit,
    onQuickScan: () -> Unit,
    onFinishTour: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // Header card
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Kitchen Tour",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    "Scan your kitchen area by area. AI identifies items and assigns the correct storage location automatically.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Tally chip (if items scanned)
        if (totalItemsScanned > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            "$totalItemsScanned items scanned across ${completedAreas.size} area${if (completedAreas.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp))
                    }
                )
            }
        }

        // Area title
        Text(
            "Choose an area to scan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Area grid (2 columns using FlowRow)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            KitchenArea.KITCHEN_AREAS.forEach { area ->
                val isCompleted = completedAreas.containsKey(area.id)
                val itemCount = completedAreas[area.id]
                AreaCard(
                    area = area,
                    isCompleted = isCompleted,
                    itemCount = itemCount,
                    enabled = true,
                    onClick = { onSelectArea(area.id) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Scan link
        TextButton(
            onClick = onQuickScan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Icon(Icons.Filled.CameraAlt, null, Modifier.size(16.dp))
            Text("Quick Scan (skip area selection)", Modifier.padding(start = 8.dp))
        }

        // Finish tour button (if areas completed)
        if (completedAreas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onFinishTour,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                Text("Done - View Summary", Modifier.padding(start = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Area Card ────────────────────────────────────────────────────────

@Composable
private fun AreaCard(
    area: KitchenArea,
    isCompleted: Boolean,
    itemCount: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = areaIcon(area.id)

    AppCard(
        modifier = modifier
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .clickable(enabled = enabled) { onClick() },
        containerColor = if (isCompleted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    icon, null,
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    area.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isCompleted && itemCount != null) "$itemCount items added"
                    else area.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Checkmark overlay
            if (isCompleted) {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
        }
    }
}

// ── Idle State ───────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    areaName: String?,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        if (areaName != null) "Scan: $areaName" else "Kitchen Scanner",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    if (areaName != null)
                        "Take a photo of your $areaName. AI will identify items and assign them to the correct storage location."
                    else
                        "Take a photo of your fridge, pantry, or any shelf. AI will identify every item it can see and add them to your inventory.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
            Text("Take Photo", Modifier.padding(start = 8.dp))
        }

        OutlinedButton(
            onClick = onPickGallery,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(18.dp))
            Text("Pick from Gallery", Modifier.padding(start = 8.dp))
        }
    }
}

// ── Processing State (Paper & Ink Theme) ─────────────────────────────

@Composable
private fun ProcessingContent(
    bitmap: android.graphics.Bitmap?,
    modifier: Modifier = Modifier
) {
    var currentStep by remember(bitmap) { mutableStateOf(0) }
    val completedSteps = remember(bitmap) { mutableStateListOf<Int>() }

    // Ink progress line — animates from step/5 to (step+1)/5 per step
    val inkProgress = remember(bitmap) { Animatable(0f) }

    // Step visibility flags for Write-In entrance stagger
    val stepVisible = remember(bitmap) { mutableStateListOf<Boolean>().apply {
        repeat(fridgeProcessingSteps.size) { add(false) }
    }}

    LaunchedEffect(bitmap) {
        for (i in fridgeProcessingSteps.indices) {
            currentStep = i
            // Reveal step with stagger delay
            delay(i * 70L)
            stepVisible[i] = true
            // Animate ink progress for this step
            val targetProgress = (i + 1).toFloat() / fridgeProcessingSteps.size
            inkProgress.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(
                    durationMillis = fridgeProcessingSteps[i].delayMs.toInt(),
                    easing = FastOutSlowInEasing
                )
            )
            if (i < fridgeProcessingSteps.size - 1) {
                completedSteps.add(i)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier) {
        // Blurred photo background
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
                    .graphicsLayer { alpha = 0.15f },
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            // Photo thumbnail card
            if (bitmap != null) {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .aspectRatio(0.75f)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Ink progress line (replaces pulsing AutoAwesome icon)
            InkProgressLine(
                progress = inkProgress.value,
                color = primaryColor,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Identifying items...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step rows with Write-In entrance + InkBloomDot indicators
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fridgeProcessingSteps.forEachIndexed { index, step ->
                    val isCompleted = index in completedSteps
                    val isActive = index == currentStep && !isCompleted
                    val isVisible = stepVisible.getOrElse(index) { false }

                    val dotState = when {
                        isCompleted -> InkDotState.COMPLETED
                        isActive -> InkDotState.ACTIVE
                        else -> InkDotState.PENDING
                    }

                    // Write-In entrance animations
                    val translateX = remember { Animatable((-20f)) }
                    val alphaAnim = remember { Animatable(0f) }

                    LaunchedEffect(isVisible) {
                        if (isVisible) {
                            // Run both animations concurrently:
                            // BouncySpring for position, GentleSpring for alpha
                            launch {
                                translateX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
                                )
                            }
                            launch {
                                alphaAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio = 1.0f, stiffness = 200f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = translateX.value * density
                                alpha = alphaAnim.value
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InkBloomDot(
                            state = dotState,
                            color = primaryColor
                        )

                        Text(
                            text = step.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                                isActive -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

// ── Review State ─────────────────────────────────────────────────────

@Composable
private fun ReviewContent(
    items: List<EditableFridgeItem>,
    units: List<com.inventory.app.data.local.entity.UnitEntity>,
    categories: List<com.inventory.app.data.local.entity.CategoryEntity>,
    onUpdateName: (Int, String) -> Unit,
    onUpdateQuantity: (Int, String) -> Unit,
    onUpdateUnit: (Int, String) -> Unit,
    onUpdateCategory: (Int, String) -> Unit,
    onUpdateMatchType: (Int, FridgeMatchType, Long?) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onAddAll: () -> Unit,
    onRetake: () -> Unit = {},
    onBackToAreas: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Empty results — show tips card
    if (items.isEmpty()) {
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt, null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "No Items Found",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Tips for better results:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "Move closer to the shelf",
                            "Ensure good lighting",
                            "Avoid blurry photos",
                            "Try a different angle"
                        ).forEach { tip ->
                            Text(
                                "\u2022  $tip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                Text("Try Again", Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = onBackToAreas,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back to Areas") }
        }
        return
    }

    val activeItems = items.filter { it.matchType != FridgeMatchType.SKIP }
    val newCount = activeItems.count { it.matchType == FridgeMatchType.CREATE_NEW }
    val updateCount = activeItems.count { it.matchType == FridgeMatchType.UPDATE_EXISTING }

    // Group items by category
    val categoryGroups = remember(items) { groupItemsByCategory(items) }

    // Expand/collapse state — all expanded by default
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Compute stagger indices for items across groups (keyed on size so edits don't reset animation)
    val staggerIndices = remember(items.size) {
        val map = mutableMapOf<Int, Int>()
        var position = 0
        categoryGroups.forEach { group ->
            group.items.forEach { indexedItem ->
                map[indexedItem.flatIndex] = position
                position++
            }
        }
        map
    }

    // Large results truncation
    val isLargeResult = items.size > 20
    var showAllItems by remember { mutableStateOf(false) }
    val visibleLimit = if (!showAllItems && isLargeResult) 20 else Int.MAX_VALUE

    // Animated tally counter — counts up as items stagger in
    var tallyTarget by remember { mutableStateOf(0) }
    LaunchedEffect(items.size) {
        delay(100)
        tallyTarget = items.size
    }

    Column(modifier = modifier) {
        // Header with animated tally
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedCounter(targetValue = tallyTarget) { count ->
                Text(
                    "$count items found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (newCount > 0) {
                    Text(
                        "$newCount new",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (updateCount > 0) {
                    Text(
                        "$updateCount already in inventory",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Pre-compute visible groups and items BEFORE LazyColumn (avoids mutable vars in builder)
        val expandedSnapshot = expandedCategories.toMap()
        val visibleGroups = remember(categoryGroups, expandedSnapshot, visibleLimit) {
            val result = mutableListOf<Triple<ReviewCategoryGroup, Boolean, List<IndexedFridgeItem>>>()
            var count = 0
            var done = false
            for (group in categoryGroups) {
                if (done) break
                val isExp = expandedSnapshot.getOrDefault(group.categoryName, true)
                if (!isExp) {
                    result.add(Triple(group, false, emptyList()))
                } else {
                    val remaining = visibleLimit - count
                    val vis = if (remaining >= group.items.size) group.items
                    else { done = true; group.items.take(remaining) }
                    count += vis.size
                    result.add(Triple(group, true, vis))
                }
            }
            result
        }

        // Category-grouped item list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            visibleGroups.forEach { (group, isExpanded, visItems) ->
                // Category header
                item(key = "header_${group.categoryName}") {
                    ReviewCategoryHeader(
                        group = group,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedCategories[group.categoryName] = !isExpanded
                        }
                    )
                }

                // Items in this category (if expanded)
                if (isExpanded) {
                    items(
                        items = visItems,
                        key = { "fridge_${it.flatIndex}" }
                    ) { indexedItem ->
                        val staggerPosition = staggerIndices[indexedItem.flatIndex] ?: 0

                        ReviewItemWithAnimation(
                            staggerIndex = staggerPosition
                        ) {
                            ReviewItemCard(
                                item = indexedItem.item,
                                index = indexedItem.flatIndex,
                                units = units,
                                categories = categories,
                                onNameChange = { onUpdateName(indexedItem.flatIndex, it) },
                                onQuantityChange = { onUpdateQuantity(indexedItem.flatIndex, it) },
                                onUnitChange = { onUpdateUnit(indexedItem.flatIndex, it) },
                                onCategoryChange = { onUpdateCategory(indexedItem.flatIndex, it) },
                                onMatchTypeChange = { type, id -> onUpdateMatchType(indexedItem.flatIndex, type, id) },
                                onRemove = { onRemoveItem(indexedItem.flatIndex) }
                            )
                        }
                    }
                }
            }

            // "Show all" button when truncated
            if (isLargeResult && !showAllItems) {
                item(key = "show_all_button") {
                    OutlinedButton(
                        onClick = { showAllItems = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Show all ${items.size} items")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Bottom action
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onAddAll,
                modifier = Modifier.fillMaxWidth(),
                enabled = activeItems.isNotEmpty()
            ) {
                Text("Add ${activeItems.size} Items to Inventory")
            }
        }
    }
}

// ── Review Category Header ──────────────────────────────────────────

@Composable
private fun ReviewCategoryHeader(
    group: ReviewCategoryGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevronRotation"
    )

    // Fade-in entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category color dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(group.visual.color, CircleShape)
            )
            // Category icon
            Icon(
                group.visual.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = group.visual.color
            )
            // Category name
            Text(
                group.categoryName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Item count
            Text(
                "(${group.items.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Chevron
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Write-In Stagger Animation Wrapper ──────────────────────────────

@Composable
private fun ReviewItemWithAnimation(
    staggerIndex: Int,
    content: @Composable () -> Unit
) {
    val shouldAnimate = staggerIndex < 5
    val translateX = remember { Animatable(if (shouldAnimate) -20f else 0f) }
    val alphaAnim = remember { Animatable(if (shouldAnimate) 0f else 1f) }

    if (shouldAnimate) {
        LaunchedEffect(Unit) {
            delay(staggerIndex * 70L)
            coroutineScope {
                launch {
                    translateX.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
                    )
                }
                launch {
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 1.0f, stiffness = 200f)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = translateX.value * density
            alpha = alphaAnim.value
        }
    ) {
        content()
    }
}

// ── Review Item Card ─────────────────────────────────────────────────

@Composable
private fun ReviewItemCard(
    item: EditableFridgeItem,
    index: Int,
    units: List<com.inventory.app.data.local.entity.UnitEntity>,
    categories: List<com.inventory.app.data.local.entity.CategoryEntity>,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMatchTypeChange: (FridgeMatchType, Long?) -> Unit,
    onRemove: () -> Unit
) {
    val isSkipped = item.matchType == FridgeMatchType.SKIP
    val categoryVisual = CategoryVisuals.get(item.categoryName)
    val isUnsure = item.confidence == "low" || item.confidence == "medium"
    val confidenceColor = when (item.confidence) {
        "high" -> MaterialTheme.colorScheme.primary
        "medium" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .then(if (isSkipped) Modifier.alpha(0.5f) else Modifier)
        .then(
            if (isUnsure && !isSkipped)
                Modifier.border(
                    width = 1.dp,
                    color = if (item.confidence == "low") MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
            else Modifier
        )

    // Ink-wash background tint for uncertain items
    val inkWashColor = if (isUnsure && !isSkipped) {
        if (item.confidence == "low") MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
    } else null

    AppCard(modifier = cardModifier, containerColor = inkWashColor) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cross-area dedup warning badge
            if (item.dupWarning != null && !isSkipped) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = item.dupWarning,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // "AI unsure" banner for medium/low confidence
            if (isUnsure && !isSkipped) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (item.confidence == "low")
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulsingQuestionMark(
                        color = if (item.confidence == "low") MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Not fully sure about ${item.name} — please verify",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.confidence == "low") MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Row 1: Category dot + name + confidence + menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category color dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(categoryVisual.color, CircleShape)
                )

                // Match badge
                when (item.matchType) {
                    FridgeMatchType.CREATE_NEW -> {
                        Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text("New", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    FridgeMatchType.UPDATE_EXISTING -> {
                        Icon(Icons.Filled.Inventory2, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                        Text("Exists", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    FridgeMatchType.SKIP -> {
                        Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text("Skip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Confidence badge
                Text(
                    item.confidence,
                    style = MaterialTheme.typography.labelSmall,
                    color = confidenceColor
                )

                // Action menu
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.MoreVert, "Options", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Create new") },
                            onClick = { onMatchTypeChange(FridgeMatchType.CREATE_NEW, null); showMenu = false },
                            leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) }
                        )
                        if (item.inventoryCandidates.isNotEmpty()) {
                            item.inventoryCandidates.forEach { candidate ->
                                DropdownMenuItem(
                                    text = {
                                        val qtyStr = if (candidate.currentQuantity == candidate.currentQuantity.toLong().toDouble())
                                            candidate.currentQuantity.toLong().toString()
                                        else String.format("%.1f", candidate.currentQuantity)
                                        Text("Update \"${candidate.name}\" ($qtyStr)")
                                    },
                                    onClick = { onMatchTypeChange(FridgeMatchType.UPDATE_EXISTING, candidate.id); showMenu = false },
                                    leadingIcon = { Icon(Icons.Filled.Inventory2, null, Modifier.size(18.dp)) }
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text("Skip") },
                            onClick = { onMatchTypeChange(FridgeMatchType.SKIP, null); showMenu = false },
                            leadingIcon = { Icon(Icons.Filled.Block, null, Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                            onClick = { onRemove(); showMenu = false },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            if (!isSkipped) {
                // Row 2: Name
                OutlinedTextField(
                    value = item.name,
                    onValueChange = onNameChange,
                    label = { Text("Item name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Row 3: Qty + Unit + Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.quantity,
                        onValueChange = onQuantityChange,
                        label = { Text("Qty") },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    DropdownField(
                        label = "Unit",
                        options = units,
                        selectedOption = units.find {
                            it.abbreviation.equals(item.unit, ignoreCase = true) ||
                            it.name.equals(item.unit, ignoreCase = true)
                        },
                        onOptionSelected = { unit ->
                            onUnitChange(unit?.abbreviation ?: "")
                        },
                        optionLabel = { it.abbreviation },
                        modifier = Modifier.weight(1f),
                        allowNone = true
                    )
                }

                // Row 4: Category
                DropdownField(
                    label = "Category",
                    options = categories,
                    selectedOption = categories.find { it.name.equals(item.categoryName, ignoreCase = true) },
                    onOptionSelected = { cat ->
                        onCategoryChange(cat?.name ?: "")
                    },
                    optionLabel = { it.name },
                    modifier = Modifier.fillMaxWidth(),
                    allowNone = true
                )
            }
        }
    }
}

// ── Area Success State ───────────────────────────────────────────────

@Composable
private fun AreaSuccessContent(
    count: Int,
    areaName: String?,
    isInTourMode: Boolean,
    completedAreas: Map<String, Int>,
    onScanNextArea: () -> Unit,
    onViewSummary: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.CheckCircle, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (areaName != null) "$count items added from $areaName!"
            else "$count items added!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (isInTourMode && completedAreas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val totalItems = completedAreas.values.sum()
            Text(
                "$totalItems total across ${completedAreas.size} area${if (completedAreas.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isInTourMode) {
            Button(
                onClick = onScanNextArea,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Scan Next Area") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onViewSummary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                Text("Done - View Summary", Modifier.padding(start = 8.dp))
            }
        } else {
            Button(
                onClick = onScanNextArea,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Scan Another Area") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Done") }
        }
    }
}

// ── Tour Summary State (Paper & Ink Celebration) ─────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TourSummaryContent(
    areaResults: Map<String, Int>,
    totalItems: Int,
    categoryBreakdown: Map<String, Int> = emptyMap(),
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Staggered reveal phases
    var showHeadline by remember { mutableStateOf(false) }
    var showCounter by remember { mutableStateOf(false) }
    var showSeparator1 by remember { mutableStateOf(false) }
    var showPills by remember { mutableStateOf(false) }
    var showSeparator2 by remember { mutableStateOf(false) }
    var showAreaCard by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600)
        showHeadline = true
        delay(300)
        showCounter = true
        delay(200)
        showSeparator1 = true
        delay(150)
        showPills = true
        delay(150)
        showSeparator2 = true
        delay(100)
        showAreaCard = true
        delay(100)
        showButton = true
    }

    // Ink separator progress
    val separator1Progress = remember { Animatable(0f) }
    val separator2Progress = remember { Animatable(0f) }
    LaunchedEffect(showSeparator1) {
        if (showSeparator1) {
            separator1Progress.animateTo(1f, tween(400, easing = EaseOutCubic))
        }
    }
    LaunchedEffect(showSeparator2) {
        if (showSeparator2) {
            separator2Progress.animateTo(1f, tween(400, easing = EaseOutCubic))
        }
    }

    // Animated counter target
    var counterTarget by remember { mutableStateOf(0) }
    LaunchedEffect(showCounter) {
        if (showCounter) {
            counterTarget = totalItems
        }
    }

    // Headline Write-In animation
    val headlineTranslateX = remember { Animatable(-20f) }
    val headlineAlpha = remember { Animatable(0f) }
    LaunchedEffect(showHeadline) {
        if (showHeadline) {
            launch {
                headlineTranslateX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 200f))
            }
            launch {
                headlineAlpha.animateTo(1f, spring(dampingRatio = 1.0f, stiffness = 200f))
            }
        }
    }

    // Area card fade
    val areaCardAlpha = remember { Animatable(0f) }
    LaunchedEffect(showAreaCard) {
        if (showAreaCard) {
            areaCardAlpha.animateTo(1f, tween(300))
        }
    }

    // Button fade
    val buttonAlpha = remember { Animatable(0f) }
    LaunchedEffect(showButton) {
        if (showButton) {
            buttonAlpha.animateTo(1f, tween(200))
        }
    }

    // Sorted category pills (top 6)
    val sortedCategories = remember(categoryBreakdown) {
        categoryBreakdown.entries
            .sortedByDescending { it.value }
            .map { (name, count) -> name to count }
    }
    val visiblePills = sortedCategories.take(6)
    val extraCount = (sortedCategories.size - 6).coerceAtLeast(0)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. InkFireworks — fires on enter
        InkFireworks(
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Headline — Write-In entrance
        Text(
            "Kitchen Tour Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                translationX = headlineTranslateX.value * density
                alpha = headlineAlpha.value
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Animated counter
        AnimatedCounter(targetValue = counterTarget) { count ->
            Text(
                "$count items mapped across ${areaResults.size} area${if (areaResults.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = if (showCounter) 1f else 0f
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Ink separator line 1
        InkSeparatorLine(
            progress = separator1Progress.value,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        // 5. Category pills
        if (visiblePills.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                visiblePills.forEachIndexed { index, (catName, count) ->
                    CategoryPill(
                        categoryName = catName,
                        count = count,
                        staggerIndex = index,
                        visible = showPills
                    )
                }
                if (extraCount > 0) {
                    CategoryPill(
                        categoryName = "+$extraCount more",
                        count = 0,
                        staggerIndex = visiblePills.size,
                        visible = showPills,
                        isOverflow = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6. Ink separator line 2
        InkSeparatorLine(
            progress = separator2Progress.value,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Area breakdown card
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = areaCardAlpha.value }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Area Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                areaResults.forEach { (areaName, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                areaName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            "$count item${if (count != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 8. Done button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = buttonAlpha.value }
        ) { Text("Done") }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Ink Separator Line (wobbly Canvas line) ──────────────────────────

@Composable
private fun InkSeparatorLine(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val wobbleSeed = remember { (Math.random() * 100).toFloat() }
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier.height(4.dp)
    ) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h / 2f)
            val segments = 10
            for (i in 1..segments) {
                val x = w * i / segments
                val wobble = sin((i + wobbleSeed) * 1.7) * h * 0.4f
                lineTo(x, h / 2f + wobble.toFloat())
            }
        }
        val clipRight = w * progress.coerceIn(0f, 1f)
        if (clipRight > 0f) {
            clipRect(right = clipRight) {
                // Bleed layer
                drawPath(
                    path,
                    color = lineColor.copy(alpha = 0.2f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                // Core layer
                drawPath(
                    path,
                    color = lineColor.copy(alpha = 0.5f),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

// ── Category Pill (animated chip for celebration) ────────────────────

@Composable
private fun CategoryPill(
    categoryName: String,
    count: Int,
    staggerIndex: Int,
    visible: Boolean,
    isOverflow: Boolean = false
) {
    val visual = if (!isOverflow) CategoryVisuals.get(categoryName) else null
    val dotColor = visual?.color ?: MaterialTheme.colorScheme.onSurfaceVariant

    // Scale entrance with BouncySpring + stagger
    val scale = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(staggerIndex * 70L)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f))
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(
                dotColor.copy(alpha = 0.10f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (!isOverflow) {
                // Colored dot (8dp)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor.copy(alpha = 0.75f), CircleShape)
                )
            }
            Text(
                text = if (isOverflow) categoryName else "$categoryName \u00d7$count",
                style = MaterialTheme.typography.labelSmall,
                color = dotColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
