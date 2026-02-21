package com.inventory.app.ui.screens.shopping

import android.content.Intent
import com.inventory.app.ui.components.CelebrationType
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Category
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.data.local.entity.relations.ShoppingListItemWithDetails
import com.inventory.app.domain.model.ConsumptionPrediction
import com.inventory.app.domain.model.Priority
import com.inventory.app.ui.components.AllDoneCelebration
import com.inventory.app.ui.components.CategoryStat
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkStrikethrough
import com.inventory.app.ui.components.InkWashSwipeBackground
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.ExpiryOrange
import com.inventory.app.ui.theme.ExpiryRed
import com.inventory.app.ui.theme.StockYellow
import com.inventory.app.util.CategoryVisuals
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    navController: NavController,
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showShoppingSheet = LocalShowAddShoppingSheet.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showClearConfirmation by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val sortBy = uiState.sortBy
    var isOverflowExpanded by rememberSaveable { mutableStateOf(false) }
    var hasEverTappedOverflow by rememberSaveable { mutableStateOf(false) }
    var hasShownPeekHint by rememberSaveable { mutableStateOf(false) }
    var showSwipeHintText by rememberSaveable { mutableStateOf(true) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear Purchased Items") },
            text = { Text("Are you sure you want to remove all ${uiState.purchasedItems.size} purchased item(s) from the list?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearPurchased()
                    showClearConfirmation = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch quick-add dialog
    if (uiState.showBatchAddDialog) {
        BatchAddDialog(
            onDismiss = { viewModel.hideBatchAdd() },
            onAdd = { text -> viewModel.batchAddItems(text) }
        )
    }

    // Undo delete snackbar
    LaunchedEffect(uiState.recentlyDeletedItem) {
        val deleted = uiState.recentlyDeletedItem ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Item deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete()
        } else {
            viewModel.clearRecentlyDeleted()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    BackHandler(enabled = isOverflowExpanded) {
        isOverflowExpanded = false
    }

    var quickAddText by rememberSaveable { mutableStateOf("") }
    var isPurchasedCollapsed by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val quickAddFocusRequester = remember { FocusRequester() }

    fun cycleSortBy(current: String): String = when (current) {
        "priority" -> "name"; "name" -> "quantity"; else -> "priority"
    }
    fun sortLabel(key: String): String = when (key) {
        "name" -> "A-Z"; "quantity" -> "Qty"; else -> "Priority"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shopping List")
                        val totalCount = uiState.activeItems.size + uiState.purchasedItems.size
                        val purchasedCount = uiState.purchasedItems.size
                        val showRing = purchasedCount > 0 && totalCount > 0
                        AnimatedVisibility(visible = showRing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp)
                            ) {
                                ShoppingProgressRing(
                                    purchasedCount = purchasedCount,
                                    totalCount = totalCount
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Left icons — hide when overflow is expanded
                    AnimatedVisibility(
                        visible = !isOverflowExpanded,
                        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(tween(100))
                    ) {
                        Row {
                            IconButton(onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) searchQuery = ""
                            }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = if (isSearchVisible) "Hide search" else "Search",
                                    tint = if (isSearchVisible) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.toggleCategoryGrouping() }) {
                                Icon(
                                    if (uiState.isGroupedByCategory) Icons.Filled.ViewList else Icons.Outlined.Category,
                                    contentDescription = if (uiState.isGroupedByCategory) "Flat list" else "Group by category"
                                )
                            }
                            if (uiState.activeItems.isNotEmpty()) {
                                IconButton(onClick = {
                                    val text = uiState.activeItems.joinToString("\n") { item ->
                                        val name = item.item?.name ?: item.shoppingItem.customName ?: "Unknown"
                                        val qty = item.shoppingItem.quantity.formatQty()
                                        val unit = item.unit?.abbreviation ?: ""
                                        if (unit.isNotEmpty()) "$name — $qty $unit" else "$name — $qty"
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Shopping List:\n$text")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Shopping List"))
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share list")
                                }
                            }
                        }
                    }
                    // Morphing overflow bar
                    MorphingOverflowBar(
                        isExpanded = isOverflowExpanded,
                        hasEverTapped = hasEverTappedOverflow,
                        onToggle = {
                            hasEverTappedOverflow = true
                            isOverflowExpanded = !isOverflowExpanded
                        },
                        currentSortLabel = sortLabel(sortBy),
                        onCycleSort = { viewModel.updateSortBy(cycleSortBy(sortBy)) },
                        onRestock = {
                            viewModel.generateFromLowStock()
                            isOverflowExpanded = false
                        },
                        purchasedCount = uiState.purchasedItems.size,
                        onClearPurchased = {
                            showClearConfirmation = true
                            isOverflowExpanded = false
                        }
                    )
                }
            )
        },
        bottomBar = {
            QuickAddBar(
                text = quickAddText,
                onTextChange = { newText ->
                    quickAddText = newText
                    viewModel.updateQuickAddQuery(newText)
                },
                onSubmit = {
                    viewModel.quickAddItem(quickAddText)
                    quickAddText = ""
                    focusManager.clearFocus()
                },
                onFullFormClick = {
                    showShoppingSheet(null, null)
                },
                suggestions = uiState.quickAddSuggestions,
                onSuggestionClick = { suggestion ->
                    viewModel.quickAddSuggestionItem(suggestion)
                    quickAddText = ""
                    focusManager.clearFocus()
                },
                onDismissSuggestions = { viewModel.clearQuickAddSuggestions() },
                focusRequester = quickAddFocusRequester
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.activeItems.isEmpty() && uiState.purchasedItems.isEmpty() -> {
                ShoppingListEmptyState(
                    buyAgainItems = uiState.buyAgainItems,
                    predictions = uiState.predictions,
                    onBuyAgainAdd = { viewModel.addBuyAgainItemToList(it) },
                    onPredictionAdd = { viewModel.addPredictionToShoppingList(it) },
                    onQuickAddFocus = {
                        try { quickAddFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onGenerateFromLowStock = { viewModel.generateFromLowStock() },
                    onBatchAdd = { viewModel.showBatchAdd() },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                fun matchesSearch(item: ShoppingListItemWithDetails): Boolean {
                    if (searchQuery.isBlank()) return true
                    val name = item.item?.name ?: item.shoppingItem.customName ?: ""
                    return name.contains(searchQuery, ignoreCase = true)
                }
                fun sortItems(items: List<ShoppingListItemWithDetails>): List<ShoppingListItemWithDetails> {
                    return when (sortBy) {
                        "name" -> items.sortedBy { (it.item?.name ?: it.shoppingItem.customName ?: "").lowercase() }
                        "quantity" -> items.sortedByDescending { it.shoppingItem.quantity }
                        else -> items.sortedByDescending { it.shoppingItem.priority }
                    }
                }
                val filteredActive = sortItems(uiState.activeItems.filter { matchesSearch(it) })
                val filteredPurchased = sortItems(uiState.purchasedItems.filter { matchesSearch(it) })

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LaunchedEffect(Unit) {
                            searchFocusRequester.requestFocus()
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search shopping list...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .focusRequester(searchFocusRequester),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        searchQuery = ""
                                    } else {
                                        isSearchVisible = false
                                    }
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {

                    // "All Done!" celebration with trip summary when active list becomes empty
                    if (uiState.celebrationType != null) {
                        item(key = "celebration") {
                            // Compute category breakdown from purchased items
                            val categoryBreakdown = remember(uiState.purchasedItems, uiState.categories, uiState.inferredCategories) {
                                val counts = mutableMapOf<String, Int>()
                                for (item in uiState.purchasedItems) {
                                    val catName = item.item?.categoryId?.let { uiState.categories[it] }
                                        ?: uiState.inferredCategories[item.shoppingItem.id]
                                        ?: continue  // skip uncategorized
                                    counts[catName] = (counts[catName] ?: 0) + 1
                                }
                                counts.entries
                                    .sortedByDescending { it.value }
                                    .take(5)
                                    .map { (name, count) ->
                                        CategoryStat(name, count, CategoryVisuals.get(name).color)
                                    }
                            }
                            val tripStats = remember(uiState.purchasedItems, uiState.itemPrices) {
                                var total = 0.0
                                var priced = 0
                                for (item in uiState.purchasedItems) {
                                    val cost = uiState.itemPrices[item.shoppingItem.id]
                                    if (cost != null) {
                                        total += cost
                                        priced++
                                    }
                                }
                                Pair(total, priced)
                            }
                            AllDoneCelebration(
                                celebrationType = uiState.celebrationType ?: CelebrationType.entries.random(),
                                purchasedCount = uiState.purchasedItems.size,
                                estimatedTotal = tripStats.first,
                                currencySymbol = uiState.currencySymbol,
                                pricedItemCount = tripStats.second,
                                categoryBreakdown = categoryBreakdown,
                                onClearAndFinish = { viewModel.clearAndFinish() }
                            )
                        }
                    }

                    // Prediction chip ribbon
                    if (uiState.predictions.isNotEmpty()) {
                        item {
                            PredictionChipRibbon(
                                predictions = uiState.predictions,
                                onAdd = { viewModel.addPredictionToShoppingList(it) }
                            )
                        }
                    }

                    // Buy Again chip ribbon
                    if (uiState.buyAgainItems.isNotEmpty()) {
                        item {
                            BuyAgainRibbon(
                                items = uiState.buyAgainItems,
                                onAdd = { viewModel.addBuyAgainItemToList(it) }
                            )
                        }
                    }

                    // Estimated total card with budget indicator
                    if (uiState.estimatedTotal > 0) {
                        item {
                            val budget = uiState.shoppingBudget
                            val hasBudget = budget > 0
                            val budgetRatio = if (hasBudget) (uiState.estimatedTotal / budget).toFloat().coerceIn(0f, 1f) else 0f
                            val overBudget = hasBudget && uiState.estimatedTotal > budget
                            val cardColor = when {
                                overBudget -> MaterialTheme.colorScheme.errorContainer
                                hasBudget && budgetRatio > 0.8f -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                            val contentColor = when {
                                overBudget -> MaterialTheme.colorScheme.onErrorContainer
                                hasBudget && budgetRatio > 0.8f -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                            AppCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                containerColor = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (overBudget) "Over Budget!" else "Estimated Total",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = contentColor
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${uiState.currencySymbol}${"%.2f".format(uiState.estimatedTotal)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor
                                            )
                                            if (hasBudget) {
                                                Text(
                                                    "of ${uiState.currencySymbol}${"%.2f".format(budget)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = contentColor.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                    if (hasBudget) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val barColor = when {
                                            overBudget -> MaterialTheme.colorScheme.error
                                            budgetRatio > 0.8f -> StockYellow
                                            else -> Color(0xFF4CAF50)
                                        }
                                        LinearProgressIndicator(
                                            progress = { budgetRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = barColor,
                                            trackColor = barColor.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredActive.isNotEmpty()) {
                        val firstActiveItemId = filteredActive.firstOrNull()?.shoppingItem?.id

                        // Swipe hint text (one-time, auto-dismisses after 5s)
                        item(key = "swipe_hint") {
                            // Start dismiss timer when hint is visible
                            LaunchedEffect(showSwipeHintText) {
                                if (showSwipeHintText) {
                                    delay(5000)
                                    showSwipeHintText = false
                                }
                            }
                            AnimatedVisibility(
                                visible = showSwipeHintText,
                                exit = fadeOut(tween(500)) + shrinkVertically(tween(300))
                            ) {
                                Text(
                                    "Swipe left to delete \u00B7 right to mark done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (uiState.isGroupedByCategory) {
                            // Group by category (supports both inventory-linked and inferred categories)
                            val grouped = filteredActive.groupBy { item ->
                                item.item?.categoryId?.let { uiState.categories[it] }
                                    ?: uiState.inferredCategories[item.shoppingItem.id]
                                    ?: "Uncategorized"
                            }
                            val sortedGroups = grouped.entries.sortedBy { (catName, _) ->
                                if (catName == "Uncategorized") "zzz" else catName
                            }
                            for ((categoryName, groupItems) in sortedGroups) {
                                val catVisual = CategoryVisuals.get(categoryName)
                                item(key = "cat_header_${categoryName}") {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            catVisual.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = catVisual.color
                                        )
                                        Text(
                                            "$categoryName (${groupItems.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = catVisual.color
                                        )
                                    }
                                }
                                val borderColor = catVisual.color
                                items(groupItems, key = { "active_${it.shoppingItem.id}" }) { item ->
                                    SwipeableShoppingRow(
                                        item = item,
                                        onToggle = { viewModel.togglePurchased(item.shoppingItem.id) },
                                        onDelete = { viewModel.deleteItemWithUndo(item.shoppingItem.id) },
                                        onEdit = { showShoppingSheet(null, item.shoppingItem.id) },
                                        onQuantityChange = { delta -> viewModel.updateQuantity(item.shoppingItem.id, delta) },
                                        estimatedCost = uiState.itemPrices[item.shoppingItem.id],
                                        currencySymbol = uiState.currencySymbol,
                                        storePriceInfo = item.shoppingItem.itemId?.let { uiState.storePriceInfo[it] },
                                        categoryColor = borderColor,
                                        showPeekHint = !hasShownPeekHint && item.shoppingItem.id == firstActiveItemId,
                                        onPeekComplete = { hasShownPeekHint = true }
                                    )
                                }
                            }
                        } else {
                            // Flat list
                            item {
                                Text(
                                    "To Buy (${filteredActive.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(filteredActive, key = { "active_${it.shoppingItem.id}" }) { item ->
                                val catName = item.item?.categoryId?.let { uiState.categories[it] }
                                    ?: uiState.inferredCategories[item.shoppingItem.id]
                                val catColor = if (catName != null) CategoryVisuals.get(catName).color else null
                                SwipeableShoppingRow(
                                    item = item,
                                    onToggle = { viewModel.togglePurchased(item.shoppingItem.id) },
                                    onDelete = { viewModel.deleteItemWithUndo(item.shoppingItem.id) },
                                    onEdit = { showShoppingSheet(null, item.shoppingItem.id) },
                                    onQuantityChange = { delta -> viewModel.updateQuantity(item.shoppingItem.id, delta) },
                                    estimatedCost = uiState.itemPrices[item.shoppingItem.id],
                                    currencySymbol = uiState.currencySymbol,
                                    storePriceInfo = item.shoppingItem.itemId?.let { uiState.storePriceInfo[it] },
                                    categoryColor = catColor,
                                    showPeekHint = !hasShownPeekHint && item.shoppingItem.id == firstActiveItemId,
                                    onPeekComplete = { hasShownPeekHint = true }
                                )
                            }
                        }
                    }

                    if (filteredPurchased.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isPurchasedCollapsed = !isPurchasedCollapsed }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Recently Purchased (${filteredPurchased.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (isPurchasedCollapsed) Icons.Filled.KeyboardArrowDown
                                    else Icons.Filled.KeyboardArrowUp,
                                    contentDescription = if (isPurchasedCollapsed) "Expand" else "Collapse",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (!isPurchasedCollapsed) {
                            items(filteredPurchased, key = { "purchased_${it.shoppingItem.id}" }) { item ->
                                SwipeableShoppingRow(
                                    item = item,
                                    onToggle = { viewModel.togglePurchased(item.shoppingItem.id) },
                                    onDelete = { viewModel.deleteItemWithUndo(item.shoppingItem.id) }
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorphingOverflowBar(
    isExpanded: Boolean,
    hasEverTapped: Boolean,
    onToggle: () -> Unit,
    currentSortLabel: String,
    onCycleSort: () -> Unit,
    onRestock: () -> Unit,
    purchasedCount: Int,
    onClearPurchased: () -> Unit
) {
    // Pill stagger states
    var showPill1 by remember { mutableStateOf(false) }
    var showPill2 by remember { mutableStateOf(false) }
    var showPill3 by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            showPill1 = true
            delay(50)
            showPill2 = true
            delay(50)
            showPill3 = true
        } else {
            showPill1 = false
            showPill2 = false
            showPill3 = false
        }
    }

    // Background color animation
    val bgColor by animateColorAsState(
        targetValue = if (isExpanded)
            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        else Color.Transparent,
        animationSpec = tween(250),
        label = "overflowBg"
    )

    // Discovery glow
    val showGlow = !hasEverTapped && !isExpanded
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(24.dp))
            .padding(horizontal = if (isExpanded) 4.dp else 0.dp)
    ) {
        // Sort pill
        AnimatedVisibility(
            visible = showPill1,
            enter = expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(150)),
            exit = shrinkHorizontally(animationSpec = tween(150)) + fadeOut(tween(150))
        ) {
            Row {
                OverflowPill(
                    icon = Icons.Filled.Sort,
                    label = currentSortLabel,
                    onClick = onCycleSort,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(4.dp))
            }
        }

        // Restock pill
        AnimatedVisibility(
            visible = showPill2,
            enter = expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(150)),
            exit = shrinkHorizontally(animationSpec = tween(150)) + fadeOut(tween(150))
        ) {
            Row {
                OverflowPill(
                    icon = Icons.Filled.AutoAwesome,
                    label = "Restock",
                    onClick = onRestock,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.width(4.dp))
            }
        }

        // Clear pill — only when purchased items exist
        AnimatedVisibility(
            visible = showPill3 && purchasedCount > 0,
            enter = expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(150)),
            exit = shrinkHorizontally(animationSpec = tween(150)) + fadeOut(tween(150))
        ) {
            Row {
                OverflowPill(
                    icon = Icons.Filled.ClearAll,
                    label = "Clear($purchasedCount)",
                    onClick = onClearPurchased,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(4.dp))
            }
        }

        // Toggle button: ⋮ ↔ ×
        Box(contentAlignment = Alignment.Center) {
            if (showGlow) {
                val glowColor = MaterialTheme.colorScheme.primaryContainer
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(glowScale)
                        .alpha(glowAlpha)
                        .background(glowColor, CircleShape)
                )
            }
            IconButton(onClick = onToggle) {
                Crossfade(
                    targetState = isExpanded,
                    animationSpec = tween(200),
                    label = "overflowIcon"
                ) { expanded ->
                    Icon(
                        if (expanded) Icons.Filled.Close else Icons.Filled.MoreVert,
                        contentDescription = if (expanded) "Close menu" else "More options"
                    )
                }
            }
        }
    }
}

@Composable
private fun OverflowPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun BatchAddDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Items") },
        text = {
            Column {
                Text(
                    "One item per line. Optionally prefix with quantity:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "e.g. \"2 Milk\", \"Eggs\", \"3 Bread\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    placeholder = { Text("Milk\n2 Eggs\n3 Bread") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableShoppingRow(
    item: ShoppingListItemWithDetails,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onQuantityChange: ((Double) -> Unit)? = null,
    estimatedCost: Double? = null,
    currencySymbol: String = "",
    storePriceInfo: StorePriceInfo? = null,
    categoryColor: Color? = null,
    showPeekHint: Boolean = false,
    onPeekComplete: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val peekOffsetPx = remember { Animatable(0f) }
    val isPeekingLeft = peekOffsetPx.value < -1f
    val isPeekingRight = peekOffsetPx.value > 1f

    // Peek animation: slides row right (green cart), then left (red delete), then settles
    LaunchedEffect(showPeekHint) {
        if (showPeekHint) {
            delay(800) // Let list settle first
            val targetPx = with(density) { 40.dp.toPx() }
            // Phase 1: Peek right → reveal green cart strip on left
            peekOffsetPx.animateTo(targetPx, tween(300, easing = FastOutSlowInEasing))
            delay(350)
            peekOffsetPx.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 200f))
            delay(150)
            // Phase 2: Peek left → reveal red delete strip on right
            peekOffsetPx.animateTo(-targetPx, tween(300, easing = FastOutSlowInEasing))
            delay(350)
            peekOffsetPx.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 200f))
            onPeekComplete()
        }
    }

    // Cancel peek if user starts swiping
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled && peekOffsetPx.value != 0f) {
            peekOffsetPx.snapTo(0f)
        }
    }

    // Reset dismiss state when item reappears after undo (ghost swipe fix)
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    val isPurchased = item.shoppingItem.isPurchased
    val purchasedAlpha = if (isPurchased) Modifier.alpha(0.6f) else Modifier

    Box(modifier = Modifier.graphicsLayer { clip = true }) {
        // Peek background: ink wash strip (Paper & Ink style)
        if (isPeekingRight || isPeekingLeft) {
            // Convert peek offset px to a synthetic progress fraction
            val peekFraction = (kotlin.math.abs(peekOffsetPx.value) / with(density) { 200.dp.toPx() })
                .coerceIn(0f, 1f)
            val peekDirection = if (isPeekingRight) {
                SwipeToDismissBoxValue.StartToEnd
            } else {
                SwipeToDismissBoxValue.EndToStart
            }
            InkWashSwipeBackground(
                progress = peekFraction,
                direction = peekDirection,
                modifier = Modifier.matchParentSize()
            )
        }

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.offset { IntOffset(peekOffsetPx.value.roundToInt(), 0) },
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                InkWashSwipeBackground(
                    progress = dismissState.progress,
                    direction = dismissState.targetValue
                )
            },
            content = {
                val borderMod = if (categoryColor != null) {
                    Modifier.drawBehind {
                        drawRect(
                            color = categoryColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                        )
                    }
                } else Modifier
                ShoppingListRow(
                    item = item,
                    onToggle = onToggle,
                    modifier = borderMod.then(purchasedAlpha),
                    onEdit = onEdit,
                    onQuantityChange = onQuantityChange,
                    estimatedCost = estimatedCost,
                    currencySymbol = currencySymbol,
                    showQuantityButtons = true,
                    storePriceInfo = storePriceInfo,
                    categoryColor = categoryColor
                )
            }
        )
    }
}

@Composable
private fun ShoppingListRow(
    item: ShoppingListItemWithDetails,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onQuantityChange: ((Double) -> Unit)? = null,
    estimatedCost: Double? = null,
    currencySymbol: String = "",
    showQuantityButtons: Boolean = false,
    storePriceInfo: StorePriceInfo? = null,
    categoryColor: Color? = null
) {
    val haptic = LocalHapticFeedback.current
    val itemName = item.item?.name ?: item.shoppingItem.customName ?: "Unknown"
    val isPurchased = item.shoppingItem.isPurchased
    val priority = Priority.fromValue(item.shoppingItem.priority)

    // Ink strikethrough animation state
    var isAnimatingInk by remember { mutableStateOf(false) }
    val inkProgress = remember { Animatable(0f) }

    LaunchedEffect(isAnimatingInk) {
        if (isAnimatingInk) {
            inkProgress.snapTo(0f)
            inkProgress.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            delay(50) // Brief pause at full stroke
            onToggle()
            isAnimatingInk = false
        }
    }

    // Animated checkbox scale
    val checkScale by animateFloatAsState(
        targetValue = if (isPurchased) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )

    val inkColor = categoryColor ?: MaterialTheme.colorScheme.primary

    ListItem(
        headlineContent = {
            Box {
                Text(
                    text = itemName,
                    textDecoration = if (isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isPurchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isAnimatingInk) {
                    InkStrikethrough(
                        progress = inkProgress.value,
                        color = inkColor,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        },
        supportingContent = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val qty = item.shoppingItem.quantity
                    val unit = item.unit?.abbreviation ?: ""
                    // Only show quantity in supporting text if stepper buttons aren't visible
                    if (!showQuantityButtons || isPurchased) {
                        Text("${qty.formatQty()} $unit")
                    } else if (unit.isNotEmpty()) {
                        // When stepper is visible, only show unit (qty is in the stepper)
                        Text(unit)
                    }
                    if (estimatedCost != null && !isPurchased) {
                        Text(
                            "~$currencySymbol${"%.2f".format(estimatedCost)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (priority != Priority.NORMAL && !isPurchased) {
                        Text(
                            priority.label,
                            color = if (priority == Priority.URGENT) ExpiryRed else ExpiryOrange,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                val notes = item.shoppingItem.notes
                if (!notes.isNullOrBlank()) {
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (storePriceInfo != null && !isPurchased) {
                    val storeText = if (storePriceInfo.storeName != null) {
                        "Last: $currencySymbol${"%.2f".format(storePriceInfo.unitPrice)} at ${storePriceInfo.storeName}"
                    } else {
                        "Last: $currencySymbol${"%.2f".format(storePriceInfo.unitPrice)}"
                    }
                    Text(
                        storeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        },
        leadingContent = {
            Box(modifier = Modifier.scale(checkScale)) {
                Checkbox(
                    checked = isPurchased || isAnimatingInk,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isPurchased && !isAnimatingInk) {
                            // Animate ink stroke, then toggle after animation completes
                            isAnimatingInk = true
                        } else {
                            // Un-purchase is instant
                            onToggle()
                        }
                    }
                )
            }
        },
        trailingContent = if (showQuantityButtons && !isPurchased && onQuantityChange != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onQuantityChange(-1.0) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
                    }
                    AnimatedQuantityDisplay(
                        quantity = item.shoppingItem.quantity,
                        categoryColor = inkColor
                    )
                    IconButton(onClick = { onQuantityChange(1.0) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(20.dp))
                    }
                }
            }
        } else null,
        modifier = modifier.then(if (onEdit != null && !isPurchased) Modifier.clickable { onEdit() } else Modifier)
    )
}

@Composable
private fun PredictionChipRibbon(
    predictions: List<ConsumptionPrediction>,
    onAdd: (ConsumptionPrediction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.TrendingDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "Running Low Soon",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(predictions, key = { it.itemId }) { prediction ->
                PredictionChip(prediction = prediction, onClick = { onAdd(prediction) })
            }
        }
    }
}

@Composable
private fun BuyAgainRibbon(
    items: List<BuyAgainItem>,
    onAdd: (BuyAgainItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                "Buy Again",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.itemId }) { item ->
                BuyAgainChip(item = item, onClick = { onAdd(item) })
            }
        }
    }
}

@Composable
private fun BuyAgainChip(
    item: BuyAgainItem,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                item.itemName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                "${item.purchaseCount}×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PredictionChip(
    prediction: ConsumptionPrediction,
    onClick: () -> Unit
) {
    val urgencyColor = when {
        prediction.daysRemaining <= 1 -> ExpiryRed
        prediction.daysRemaining <= 3 -> ExpiryOrange
        else -> MaterialTheme.colorScheme.tertiary
    }
    val containerColor = when {
        prediction.daysRemaining <= 1 -> MaterialTheme.colorScheme.errorContainer
        prediction.daysRemaining <= 3 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    }

    AppCard(
        onClick = onClick,
        modifier = Modifier,
        containerColor = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.AddCircle,
                contentDescription = "Add",
                modifier = Modifier.size(16.dp),
                tint = urgencyColor
            )
            Text(
                prediction.itemName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                "~${prediction.daysRemaining}d",
                style = MaterialTheme.typography.labelSmall,
                color = urgencyColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PredictionListContent(
    predictions: List<ConsumptionPrediction>,
    onAdd: (ConsumptionPrediction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Running Low Soon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Based on your purchase history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(predictions, key = { it.itemId }) { prediction ->
            PredictionListItem(prediction = prediction, onAdd = { onAdd(prediction) })
        }
    }
}

@Composable
private fun PredictionListItem(
    prediction: ConsumptionPrediction,
    onAdd: () -> Unit
) {
    val urgencyColor = when {
        prediction.daysRemaining <= 1 -> ExpiryRed
        prediction.daysRemaining <= 3 -> ExpiryOrange
        else -> MaterialTheme.colorScheme.tertiary
    }

    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    prediction.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when {
                            prediction.daysRemaining <= 0 -> "Likely out"
                            prediction.daysRemaining == 1 -> "~1 day left"
                            else -> "~${prediction.daysRemaining} days left"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = urgencyColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Qty: ${prediction.suggestedQuantity.formatQty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = "Add to shopping list",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingListEmptyState(
    buyAgainItems: List<BuyAgainItem>,
    predictions: List<ConsumptionPrediction>,
    onBuyAgainAdd: (BuyAgainItem) -> Unit,
    onPredictionAdd: (ConsumptionPrediction) -> Unit,
    onQuickAddFocus: () -> Unit,
    onGenerateFromLowStock: () -> Unit,
    onBatchAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Paper & Ink: Staggered entrance state ---
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Icon: "Land" entrance — scale 0.3→1.0 with BouncySpring
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "icon_scale"
    )
    val iconOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else -30f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "icon_offset"
    )

    // Icon idle: "Floating" — translateY ±4dp, 3000ms
    val infiniteTransition = rememberInfiniteTransition(label = "empty_idle")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Tagline: rotating text
    val taglines = remember { listOf(
        "What do we need today?",
        "Ready when you are",
        "Let's plan your next trip",
        "Your cart awaits"
    ) }
    var taglineIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            taglineIndex = (taglineIndex + 1) % taglines.size
        }
    }

    // Staggered appearance for sections
    var showTagline by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showBuyAgain by remember { mutableStateOf(false) }
    var showPredictions by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150)  // Child appear delay
        showTagline = true
        delay(200)  // Section delay
        showActions = true
        delay(200)
        if (buyAgainItems.isNotEmpty()) showBuyAgain = true
        delay(200)
        if (predictions.isNotEmpty()) showPredictions = true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Icon + Tagline ---
        item(key = "empty_hero") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            translationY = (iconOffsetY + floatingOffset) * density
                        },
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rotating tagline with crossfade
                AnimatedVisibility(
                    visible = showTagline,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(dampingRatio = 1f, stiffness = 200f)
                    )
                ) {
                    AnimatedContent(
                        targetState = taglineIndex,
                        transitionSpec = {
                            (fadeIn(tween(400)) + slideInVertically { -it / 4 })
                                .togetherWith(fadeOut(tween(300)) + slideOutVertically { it / 4 })
                        },
                        label = "tagline"
                    ) { index ->
                        Text(
                            text = taglines[index],
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- Buy Again: Hero section (if data exists) ---
        if (buyAgainItems.isNotEmpty()) {
            item(key = "empty_buyagain") {
                AnimatedVisibility(
                    visible = showBuyAgain,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(dampingRatio = 1f, stiffness = 200f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "You usually buy...",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            buyAgainItems.take(5).forEachIndexed { index, item ->
                                // Stagger cascade: 70ms per item
                                var chipVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    delay(70L * index)
                                    chipVisible = true
                                }
                                AnimatedVisibility(
                                    visible = chipVisible,
                                    enter = fadeIn(tween(200)) + expandHorizontally(
                                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
                                    )
                                ) {
                                    BuyAgainChip(item = item, onClick = { onBuyAgainAdd(item) })
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Running Low Soon: compact prediction chips ---
        if (predictions.isNotEmpty()) {
            item(key = "empty_predictions") {
                AnimatedVisibility(
                    visible = showPredictions,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(dampingRatio = 1f, stiffness = 200f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "Running low soon",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            predictions.take(4).forEachIndexed { index, prediction ->
                                var chipVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    delay(70L * index)
                                    chipVisible = true
                                }
                                AnimatedVisibility(
                                    visible = chipVisible,
                                    enter = fadeIn(tween(200)) + expandHorizontally(
                                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
                                    )
                                ) {
                                    PredictionChip(prediction = prediction, onClick = { onPredictionAdd(prediction) })
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Action row: subtle text buttons ---
        item(key = "empty_actions") {
            AnimatedVisibility(
                visible = showActions,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(dampingRatio = 1f, stiffness = 200f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Thin divider line — pen stroke feel
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .padding(bottom = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onQuickAddFocus) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quick Add", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            "\u00B7",
                            color = MaterialTheme.colorScheme.outlineVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = onGenerateFromLowStock) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restock", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            "\u00B7",
                            color = MaterialTheme.colorScheme.outlineVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = onBatchAdd) {
                            Icon(
                                Icons.Filled.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste List", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFullFormClick: () -> Unit,
    suggestions: List<QuickAddSuggestion>,
    onSuggestionClick: (QuickAddSuggestion) -> Unit,
    onDismissSuggestions: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        // Suggestions dropdown (appears above the bar)
        AnimatedVisibility(
            visible = suggestions.isNotEmpty() && isFocused,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    suggestions.forEach { suggestion ->
                        val categoryColor = suggestion.categoryName?.let {
                            CategoryVisuals.get(it).color
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Category color dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = categoryColor ?: MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                            )
                            // Name + context column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    suggestion.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (suggestion.contextLabel.isNotBlank()) {
                                    Text(
                                        suggestion.contextLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Add icon
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add ${suggestion.name}",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // The quick-add bar itself
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = {
                        Text(
                            "Add items\u2026  2 Milk, Eggs, 3 Bread",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (text.isNotBlank()) onSubmit() }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = {
                                onTextChange("")
                                onDismissSuggestions()
                            }, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                // Send button (visible when text is entered)
                AnimatedVisibility(visible = text.isNotBlank()) {
                    IconButton(
                        onClick = onSubmit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Full form button (expand icon) — always visible
                IconButton(
                    onClick = onFullFormClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Full add form",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingProgressRing(
    purchasedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) purchasedCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ring_progress"
    )

    val isComplete = purchasedCount == totalCount && totalCount > 0
    val bounceScale = remember { Animatable(1f) }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            bounceScale.animateTo(
                1.15f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            bounceScale.animateTo(
                1.0f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            bounceScale.snapTo(1f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val completeColor = if (isComplete) MaterialTheme.colorScheme.tertiary else primaryColor

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.scale(bounceScale.value)
    ) {
        Box(modifier = Modifier.size(20.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.5.dp.toPx()
                val padding = strokeWidth / 2
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    ),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = completeColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    ),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$purchasedCount/$totalCount",
            style = MaterialTheme.typography.labelSmall,
            color = if (isComplete) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Animated quantity display with "Pen Scratch & Rewrite" effect.
 * Single tap: ink scratch draws across old number, then new number writes in with spring.
 * Rapid taps (<300ms): skip scratch, spring-bounce the new number in immediately.
 */
@Composable
private fun AnimatedQuantityDisplay(
    quantity: Double,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var displayedQuantity by remember { mutableDoubleStateOf(quantity) }
    var changeDirection by remember { mutableIntStateOf(1) }
    var lastChangeTime by remember { mutableLongStateOf(0L) }
    var showScratch by remember { mutableStateOf(false) }
    val scratchProgress = remember { Animatable(0f) }

    LaunchedEffect(quantity) {
        // Clean up any interrupted scratch from a cancelled previous launch
        showScratch = false

        if (quantity == displayedQuantity) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val isRapid = (now - lastChangeTime) < 300L
        changeDirection = if (quantity > displayedQuantity) 1 else -1
        lastChangeTime = now

        // Haptics — fire immediately, don't wait for animation
        if (quantity % 5.0 == 0.0 && quantity > 0.0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        if (!isRapid) {
            // Full animation: scratch across old number, then transition
            showScratch = true
            scratchProgress.snapTo(0f)
            scratchProgress.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
            showScratch = false
            delay(30) // Brief pause before rewrite
        }

        displayedQuantity = quantity
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = displayedQuantity,
            transitionSpec = {
                val dir = changeDirection
                val enterOffset = { fullHeight: Int -> (fullHeight / 2) * -dir }
                val exitOffset = { fullHeight: Int -> (fullHeight / 2) * dir }

                (slideInVertically(
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
                    initialOffsetY = enterOffset
                ) + fadeIn(tween(100)))
                    .togetherWith(
                        slideOutVertically(
                            animationSpec = tween(120),
                            targetOffsetY = exitOffset
                        ) + fadeOut(tween(120))
                    )
                    .using(
                        androidx.compose.animation.SizeTransform(clip = false)
                    )
            },
            label = "quantityRoll"
        ) { targetQuantity ->
            Text(
                targetQuantity.formatQty(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Ink scratch overlay — draws on top of the still-visible old number
        if (showScratch) {
            MiniInkScratch(
                progress = scratchProgress.value,
                color = categoryColor,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * A miniature ink scratch — simplified sibling of InkStrikethrough.
 * 3-segment wobble bezier, single layer, one droplet. Fits over a small number.
 */
@Composable
private fun MiniInkScratch(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val wobbleSeed = remember { (Math.random() * 10).toFloat() }

    Canvas(modifier = modifier) {
        if (progress <= 0f) return@Canvas

        val w = size.width
        val h = size.height
        val centerY = h / 2f

        // 3-segment wobble path (simpler than full InkStrikethrough's 6)
        val path = Path().apply {
            val segments = 3
            val segWidth = w / segments
            moveTo(0f, centerY)
            for (i in 1..segments) {
                val endX = segWidth * i
                val endY = centerY + sin((i + wobbleSeed) * 1.5).toFloat() * (h * 0.08f)
                val ctrlX = segWidth * (i - 0.5f)
                val ctrlY = centerY + sin((i + wobbleSeed) * 2.3 + PI / 3).toFloat() * (h * 0.12f)
                quadraticBezierTo(ctrlX, ctrlY, endX, endY)
            }
        }

        // Single layer — the number is small, two layers won't read
        clipRect(right = w * progress) {
            drawPath(
                path = path,
                color = color.copy(alpha = 0.7f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // One tiny ink droplet at the end
        if (progress > 0.85f) {
            val dropletProgress = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
            val endX = w * progress
            val endY = centerY + sin((3 + wobbleSeed) * 1.5).toFloat() * (h * 0.08f)
            val angle = Math.toRadians(20.0)
            val dist = 6.dp.toPx() * dropletProgress

            drawCircle(
                color = color.copy(alpha = (1f - dropletProgress * 0.5f) * 0.7f),
                radius = 1.5.dp.toPx() * (1f - dropletProgress * 0.3f),
                center = Offset(
                    endX + (cos(angle) * dist).toFloat(),
                    endY + (sin(angle) * dist).toFloat()
                )
            )
        }
    }
}
