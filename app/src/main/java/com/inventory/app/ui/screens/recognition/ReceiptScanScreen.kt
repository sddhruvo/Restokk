package com.inventory.app.ui.screens.recognition

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.UnitEntity
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.BarcodeCameraPreview
import com.inventory.app.ui.components.DropdownField
import com.inventory.app.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class ReviewStage { SUMMARY, PAGER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    navController: NavController,
    viewModel: ReceiptScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Document Scanner setup
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = scanResult?.pages
            if (!pages.isNullOrEmpty()) {
                val pageUri = pages[0].imageUri
                try {
                    val inputStream = context.contentResolver.openInputStream(pageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        viewModel.onImageCaptured(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("ReceiptScan", "Failed to load scanned document", e)
                }
            }
        }
    }

    // Gallery fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    viewModel.onImageCaptured(bitmap)
                }
            } catch (e: Exception) {
                Log.e("ReceiptScan", "Failed to load image from gallery", e)
            }
        }
    }

    // Helper to launch scanner
    fun launchScanner() {
        if (activity == null) return
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("ReceiptScan", "Failed to start document scanner", e)
                galleryLauncher.launch("image/*")
            }
    }

    // Back confirmation when scan results would be lost
    val hasResults = uiState.state is ReceiptScanState.Review
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasResults) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard scan results?") },
            text = { Text("Going back will discard all scanned items. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    navController.popBackStack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep reviewing") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasResults) showDiscardDialog = true
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState.state) {
            is ReceiptScanState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
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
                                    Icons.Filled.Receipt, null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    "Receipt Scanner",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Text(
                                "Scan your shopping receipt using the built-in document scanner. " +
                                "It will automatically crop, straighten, and enhance the image for best results. " +
                                "Grok AI will then extract all products with quantities and prices.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = { launchScanner() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                        Text("Scan Receipt", Modifier.padding(start = 8.dp))
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(18.dp))
                        Text("Pick from Gallery", Modifier.padding(start = 8.dp))
                    }
                }
            }

            is ReceiptScanState.Capturing -> {
                LaunchedEffect(Unit) {
                    viewModel.reset()
                }
            }

            is ReceiptScanState.ReadingText, is ReceiptScanState.ParsingWithAI -> {
                AIProcessingScreen(
                    bitmap = uiState.capturedBitmap,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            is ReceiptScanState.Review -> {
                ReviewFlowScreen(
                    items = state.items,
                    units = uiState.units,
                    categories = uiState.categories,
                    onUpdateName = viewModel::updateItemName,
                    onUpdateQuantity = viewModel::updateItemQuantity,
                    onUpdatePrice = viewModel::updateItemPrice,
                    onUpdateUnit = viewModel::updateItemUnit,
                    onUpdateCategory = viewModel::updateItemCategory,
                    onUpdateExpiryDate = viewModel::updateItemExpiryDate,
                    onUpdateBarcode = viewModel::updateItemBarcode,
                    onRemoveItem = viewModel::removeItem,
                    onUpdateMatchType = viewModel::updateMatchType,
                    onMarkReviewed = viewModel::markAsReviewed,
                    onMarkAllReviewed = viewModel::markAllReviewed,
                    onAddAll = viewModel::addAllToInventory,
                    onRetake = {
                        viewModel.reset()
                        launchScanner()
                    },
                    currencySymbol = uiState.currencySymbol,
                    modifier = Modifier.padding(padding)
                )
            }

            is ReceiptScanState.Saving -> {
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

            is ReceiptScanState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
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
                        "${state.count} items added!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Scan Another Receipt") }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Done") }
                }
            }

            is ReceiptScanState.Error -> {
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
                            viewModel.reset()
                            launchScanner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                        Text("Try Again", Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                }
            }
        }
    }
}

// ── AI Processing Screen ────────────────────────────────────────────────

private data class ProcessingStep(
    val message: String,
    val delayMs: Long
)

private val processingSteps = listOf(
    ProcessingStep("Compressing image...", 1500),
    ProcessingStep("Sending to AI...", 2000),
    ProcessingStep("Reading product names...", 2500),
    ProcessingStep("Matching prices to items...", 2500),
    ProcessingStep("Checking your shopping list...", 2000),
    ProcessingStep("Estimating expiry dates...", 2000),
    ProcessingStep("Almost done...", 8000)
)

@Composable
private fun AIProcessingScreen(
    bitmap: android.graphics.Bitmap?,
    modifier: Modifier = Modifier
) {
    // Cycle through processing steps — keyed on bitmap so they reset on new scan attempt
    var currentStep by remember(bitmap) { mutableStateOf(0) }
    val completedSteps = remember(bitmap) { mutableStateListOf<Int>() }

    LaunchedEffect(bitmap) {
        for (i in processingSteps.indices) {
            currentStep = i
            delay(processingSteps[i].delayMs)
            if (i < processingSteps.size - 1) {
                completedSteps.add(i)
            }
        }
    }

    // Pulsing glow for the AI icon
    val infiniteTransition = rememberInfiniteTransition(label = "aiPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = modifier) {
        // Background: blurred receipt preview
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

            // Receipt thumbnail
            if (bitmap != null) {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .aspectRatio(0.65f)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Scanned receipt",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // AI icon with pulsing glow
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { alpha = glowAlpha },
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Grok AI is analyzing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "your receipt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Processing steps
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                processingSteps.forEachIndexed { index, step ->
                    val isCompleted = index in completedSteps
                    val isActive = index == currentStep
                    val isVisible = index <= currentStep

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(300)) + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(300)
                        )
                    ) {
                        ProcessingStepRow(
                            text = step.message,
                            isCompleted = isCompleted,
                            isActive = isActive,
                            glowAlpha = glowAlpha
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

@Composable
private fun ProcessingStepRow(
    text: String,
    isCompleted: Boolean,
    isActive: Boolean,
    glowAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted -> {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                isActive -> {
                    // Pulsing dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer { alpha = glowAlpha }
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }

        Text(
            text = text,
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

// ── Two-Stage Review Flow ───────────────────────────────────────────────

@Composable
private fun ReviewFlowScreen(
    items: List<EditableReceiptItem>,
    units: List<UnitEntity>,
    categories: List<CategoryEntity>,
    onUpdateName: (Int, String) -> Unit,
    onUpdateQuantity: (Int, String) -> Unit,
    onUpdatePrice: (Int, String) -> Unit,
    onUpdateUnit: (Int, String) -> Unit,
    onUpdateCategory: (Int, String) -> Unit,
    onUpdateExpiryDate: (Int, LocalDate?) -> Unit,
    onUpdateBarcode: (Int, String) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onUpdateMatchType: (Int, ReceiptMatchType, Long?) -> Unit,
    onMarkReviewed: (Int) -> Unit,
    onMarkAllReviewed: () -> Unit,
    onAddAll: () -> Unit,
    onRetake: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf(ReviewStage.SUMMARY) }
    var initialPagerPage by remember { mutableStateOf(0) }

    when (stage) {
        ReviewStage.SUMMARY -> {
            ReviewSummaryScreen(
                items = items,
                currencySymbol = currencySymbol,
                onConfirmAll = {
                    onMarkAllReviewed()
                    onAddAll()
                },
                onReviewItems = { startPage ->
                    initialPagerPage = startPage
                    stage = ReviewStage.PAGER
                },
                onRetake = onRetake,
                modifier = modifier
            )
        }
        ReviewStage.PAGER -> {
            ReviewPagerScreen(
                items = items,
                units = units,
                categories = categories,
                initialPage = initialPagerPage,
                currencySymbol = currencySymbol,
                onUpdateName = onUpdateName,
                onUpdateQuantity = onUpdateQuantity,
                onUpdatePrice = onUpdatePrice,
                onUpdateUnit = onUpdateUnit,
                onUpdateCategory = onUpdateCategory,
                onUpdateExpiryDate = onUpdateExpiryDate,
                onUpdateBarcode = onUpdateBarcode,
                onRemoveItem = onRemoveItem,
                onUpdateMatchType = onUpdateMatchType,
                onMarkReviewed = onMarkReviewed,
                onConfirm = onAddAll,
                onBackToSummary = { stage = ReviewStage.SUMMARY },
                modifier = modifier
            )
        }
    }
}

// ── Stage 1: Summary Screen ─────────────────────────────────────────────

@Composable
private fun ReviewSummaryScreen(
    items: List<EditableReceiptItem>,
    currencySymbol: String,
    onConfirmAll: () -> Unit,
    onReviewItems: (Int) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeItems = items.filter { it.matchType != ReceiptMatchType.SKIP }
    val totalPrice = activeItems.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
    val updateCount = activeItems.count { it.matchType == ReceiptMatchType.UPDATE_EXISTING }
    val newCount = activeItems.count { it.matchType == ReceiptMatchType.CREATE_NEW }
    val shoppingCount = items.count { it.matchedShoppingId != null }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${activeItems.size} items found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (totalPrice > 0) {
                    Text(
                        "Total: ${currencySymbol}${String.format("%.2f", totalPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Summary chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (newCount > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("$newCount new") },
                        icon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }
                    )
                }
                if (updateCount > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("$updateCount update") },
                        icon = { Icon(Icons.Filled.Inventory2, null, Modifier.size(16.dp)) }
                    )
                }
                if (shoppingCount > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("$shoppingCount from list") },
                        icon = { Icon(Icons.Filled.ShoppingCart, null, Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Compact scrollable item list — tap to jump to pager
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            itemsIndexed(items) { index, item ->
                SummaryItemRow(
                    item = item,
                    currencySymbol = currencySymbol,
                    onClick = { onReviewItems(index) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Bottom buttons — compact layout for small screens
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirmAll,
                    modifier = Modifier.weight(1f),
                    enabled = activeItems.isNotEmpty()
                ) {
                    Text("Confirm (${activeItems.size})")
                }
                OutlinedButton(
                    onClick = { onReviewItems(0) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Review")
                }
            }
            TextButton(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CameraAlt, null, Modifier.size(14.dp))
                Text("Retake", Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SummaryItemRow(
    item: EditableReceiptItem,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isSkipped = item.matchType == ReceiptMatchType.SKIP
    val matchIcon = when (item.matchType) {
        ReceiptMatchType.CREATE_NEW -> Icons.Filled.Add
        ReceiptMatchType.UPDATE_EXISTING -> Icons.Filled.Inventory2
        ReceiptMatchType.SKIP -> Icons.Filled.Block
    }
    val matchColor = when (item.matchType) {
        ReceiptMatchType.CREATE_NEW -> MaterialTheme.colorScheme.primary
        ReceiptMatchType.UPDATE_EXISTING -> MaterialTheme.colorScheme.tertiary
        ReceiptMatchType.SKIP -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .then(if (isSkipped) Modifier.alpha(0.5f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(matchIcon, null, modifier = Modifier.size(18.dp), tint = matchColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.categoryName.isNotBlank()) {
                Text(
                    item.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (item.matchedShoppingId != null) {
            Icon(
                Icons.Filled.ShoppingCart, null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        // Quantity + unit
        val qtyUnit = buildString {
            append(item.quantity)
            if (item.unit.isNotBlank()) append(" ${item.unit}")
        }
        Text(
            qtyUnit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.price.isNotBlank()) {
            Text(
                "${currencySymbol}${item.price}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Stage 2: Pager Review ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ReviewPagerScreen(
    items: List<EditableReceiptItem>,
    units: List<UnitEntity>,
    categories: List<CategoryEntity>,
    initialPage: Int,
    currencySymbol: String,
    onUpdateName: (Int, String) -> Unit,
    onUpdateQuantity: (Int, String) -> Unit,
    onUpdatePrice: (Int, String) -> Unit,
    onUpdateUnit: (Int, String) -> Unit,
    onUpdateCategory: (Int, String) -> Unit,
    onUpdateExpiryDate: (Int, LocalDate?) -> Unit,
    onUpdateBarcode: (Int, String) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onUpdateMatchType: (Int, ReceiptMatchType, Long?) -> Unit,
    onMarkReviewed: (Int) -> Unit,
    onConfirm: () -> Unit,
    onBackToSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
        pageCount = { items.size }
    )
    val scope = rememberCoroutineScope()

    // Mark current page as reviewed
    LaunchedEffect(pagerState.currentPage) {
        onMarkReviewed(pagerState.currentPage)
    }

    // Override back to return to summary instead of showing discard dialog
    BackHandler {
        onBackToSummary()
    }

    val reviewedCount = items.count { it.isReviewed }
    val activeItems = items.filter { it.matchType != ReceiptMatchType.SKIP }
    val progressTarget = if (items.isNotEmpty()) reviewedCount.toFloat() / items.size else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressTarget, label = "progress")

    Column(modifier = modifier.fillMaxSize()) {
        // Progress bar + counter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$reviewedCount/${items.size} reviewed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Page indicator: text for >10 items, dots-style text otherwise
                Text(
                    "${pagerState.currentPage + 1} of ${items.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Pager with chevron navigation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page in items.indices) {
                    PagerItemCard(
                        item = items[page],
                        units = units,
                        categories = categories,
                        index = page,
                        currencySymbol = currencySymbol,
                        onNameChange = { onUpdateName(page, it) },
                        onQuantityChange = { onUpdateQuantity(page, it) },
                        onPriceChange = { onUpdatePrice(page, it) },
                        onUnitChange = { onUpdateUnit(page, it) },
                        onCategoryChange = { onUpdateCategory(page, it) },
                        onExpiryDateChange = { onUpdateExpiryDate(page, it) },
                        onBarcodeChange = { onUpdateBarcode(page, it) },
                        onRemove = { onRemoveItem(page) },
                        onMatchTypeChange = { type, id -> onUpdateMatchType(page, type, id) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Left chevron
            if (items.size > 1 && pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right chevron
            if (items.size > 1 && pagerState.currentPage < items.size - 1) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = activeItems.isNotEmpty()
            ) {
                Text("Confirm (${activeItems.size} items)")
            }
            TextButton(
                onClick = onBackToSummary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to summary")
            }
        }
    }
}

// ── Pager Item Card ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagerItemCard(
    item: EditableReceiptItem,
    units: List<UnitEntity>,
    categories: List<CategoryEntity>,
    index: Int,
    currencySymbol: String,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onExpiryDateChange: (LocalDate?) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMatchTypeChange: (ReceiptMatchType, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSkipped = item.matchType == ReceiptMatchType.SKIP
    val containerColor = when {
        isSkipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        item.matchType == ReceiptMatchType.UPDATE_EXISTING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> null
    }

    var showDatePicker by remember(item.name, item.expiryDate) { mutableStateOf(false) }
    var showBarcodeSheet by remember(item.name) { mutableStateOf(false) }

    AppCard(
        modifier = modifier
            .padding(horizontal = 40.dp, vertical = 8.dp)
            .then(if (isSkipped) Modifier.alpha(0.5f) else Modifier),
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Match badge + menu (badge is tappable to open menu)
            var showActionMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MatchBadge(
                    item = item,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showActionMenu = true }
                )
                ActionMenu(
                    item = item,
                    expanded = showActionMenu,
                    onExpandedChange = { showActionMenu = it },
                    onMatchTypeChange = onMatchTypeChange,
                    onRemove = onRemove
                )
            }

            if (!isSkipped) {
                // Row 2: Product name + Category
                OutlinedTextField(
                    value = item.name,
                    onValueChange = onNameChange,
                    label = { Text("Product name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Row 2b: Category dropdown
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

                // Row 3: Qty + Unit dropdown + Price
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.quantity,
                        onValueChange = onQuantityChange,
                        label = { Text("Qty") },
                        modifier = Modifier.weight(0.8f),
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
                    OutlinedTextField(
                        value = item.price,
                        onValueChange = onPriceChange,
                        label = { Text("Price") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text(currencySymbol, style = MaterialTheme.typography.bodyMedium) }
                    )
                }

                // Unit conflict hint
                if (item.unitConflict != null) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = "Warning",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                        Text(
                            "Receipt says ${item.unit} — ${item.unitConflict}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }

                // Row 4: Expiry date + Barcode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Expiry date field
                    val expiryLabel = if (item.isAiEstimatedExpiry && item.expiryDate != null) "Expected expiry" else "Expiry"
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = item.expiryDate?.let { FormatUtils.formatDate(it) } ?: "",
                            onValueChange = {},
                            label = { Text(expiryLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            readOnly = true,
                            trailingIcon = {
                                if (item.expiryDate != null) {
                                    IconButton(onClick = { onExpiryDateChange(null) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Clear, "Clear expiry", modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Icon(Icons.Filled.DateRange, "Set expiry", modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                                .alpha(0f)
                        )
                    }

                    // Barcode field
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = item.barcode,
                            onValueChange = {},
                            label = { Text("Barcode") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            readOnly = true,
                            trailingIcon = {
                                if (item.barcode.isNotBlank()) {
                                    IconButton(onClick = { onBarcodeChange("") }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Clear, "Clear barcode", modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Icon(Icons.Filled.QrCodeScanner, "Scan barcode", modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showBarcodeSheet = true }
                                .alpha(0f)
                        )
                    }
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = item.expiryDate?.let {
                it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        onExpiryDateChange(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Barcode scan bottom sheet
    if (showBarcodeSheet) {
        BarcodeScanBottomSheet(
            onBarcodeDetected = { barcode ->
                onBarcodeChange(barcode)
                showBarcodeSheet = false
            },
            onDismiss = { showBarcodeSheet = false }
        )
    }
}

// ── Match Badge ─────────────────────────────────────────────────────────

@Composable
private fun MatchBadge(item: EditableReceiptItem, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (item.matchType) {
            ReceiptMatchType.CREATE_NEW -> {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("New item", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
            }
            ReceiptMatchType.UPDATE_EXISTING -> {
                val matchedName = item.inventoryCandidates.firstOrNull { it.id == item.matchedInventoryItemId }?.name ?: "?"
                val matchedQty = item.inventoryCandidates.firstOrNull { it.id == item.matchedInventoryItemId }?.currentQuantity ?: 0.0
                val qtyStr = if (matchedQty == matchedQty.toLong().toDouble()) matchedQty.toLong().toString() else String.format("%.1f", matchedQty)
                Icon(Icons.Filled.Inventory2, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                Text(
                    "Update \"$matchedName\" ($qtyStr)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ReceiptMatchType.SKIP -> {
                Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text("Skipped", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            }
        }

        if (item.matchedShoppingId != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ShoppingCart, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Action Menu ─────────────────────────────────────────────────────────

@Composable
private fun ActionMenu(
    item: EditableReceiptItem,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onMatchTypeChange: (ReceiptMatchType, Long?) -> Unit,
    onRemove: () -> Unit
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.MoreVert, "Options", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Create new") },
                onClick = { onMatchTypeChange(ReceiptMatchType.CREATE_NEW, null); onExpandedChange(false) },
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
                        onClick = { onMatchTypeChange(ReceiptMatchType.UPDATE_EXISTING, candidate.id); onExpandedChange(false) },
                        leadingIcon = { Icon(Icons.Filled.Inventory2, null, Modifier.size(18.dp)) }
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("Skip") },
                onClick = { onMatchTypeChange(ReceiptMatchType.SKIP, null); onExpandedChange(false) },
                leadingIcon = { Icon(Icons.Filled.Block, null, Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                onClick = { onRemove(); onExpandedChange(false) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

// ── Barcode Scan Bottom Sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun BarcodeScanBottomSheet(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Scan Barcode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (cameraPermissionState.status.isGranted) {
                BarcodeCameraPreview(
                    onBarcodeDetected = onBarcodeDetected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Camera access needed to scan barcodes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }

            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
