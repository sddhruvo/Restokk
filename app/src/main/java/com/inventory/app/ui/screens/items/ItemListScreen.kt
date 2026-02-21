package com.inventory.app.ui.screens.items

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.components.AnimatedEmptyState
import com.inventory.app.ui.components.AnimatedFab
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.ExpiryOrange
import com.inventory.app.ui.theme.ExpiryRed
import com.inventory.app.ui.theme.StockGreen
import com.inventory.app.ui.theme.StockYellow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val sortOptions = listOf(
    "updated" to "Recently Updated",
    "name" to "Name (A-Z)",
    "expiry" to "Expiry Date",
    "quantity" to "Quantity",
    "created" to "Date Added"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemListScreen(
    navController: NavController,
    initialCategoryId: Long? = null,
    viewModel: ItemListViewModel = hiltViewModel()
) {
    LaunchedEffect(initialCategoryId) {
        initialCategoryId?.let { viewModel.selectCategory(it) }
    }
    val showShoppingSheet = com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchActive by rememberSaveable { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Items") },
            text = { Text("Are you sure you want to delete ${uiState.selectedIds.size} selected item(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    val deletedIds = uiState.selectedIds.toList()
                    val count = deletedIds.size
                    viewModel.deleteSelected()
                    showDeleteConfirmation = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "$count item(s) deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            deletedIds.forEach { viewModel.restoreItem(it) }
                        }
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.selectionMode) {
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = uiState.selectedIds.size,
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith
                                        slideOutVertically { it } + fadeOut()
                            },
                            label = "selectionCount"
                        ) { count ->
                            Text("$count selected")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = uiState.selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete selected",
                                tint = if (uiState.selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Items") },
                    actions = {
                        IconButton(onClick = { searchActive = !searchActive }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            sortOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            fontWeight = if (uiState.sortBy == key) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateSort(key)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.viewMode == ViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Toggle view"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedFab(
                onClick = { navController.navigate(Screen.ItemForm.createRoute()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add Item") },
                visible = !uiState.selectionMode
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar with animation
            AnimatedVisibility(
                visible = !uiState.selectionMode && (searchActive || uiState.searchQuery.isNotEmpty()),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearch(it) },
                    onSearch = { _ -> searchActive = false },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search items...") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearch("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {}
            }

            // Filter chips
            AnimatedVisibility(
                visible = !uiState.selectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = {
                                viewModel.selectCategory(
                                    if (uiState.selectedCategoryId == category.id) null else category.id
                                )
                            },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.items.isEmpty() -> AnimatedEmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = "No Items",
                        message = "Add your first inventory item"
                    )
                    else -> {
                        Crossfade(
                            targetState = uiState.viewMode,
                            animationSpec = tween(300),
                            label = "viewModeTransition"
                        ) { viewMode ->
                            when (viewMode) {
                                ViewMode.GRID -> {
                                    val (activeItems, pausedItems) = uiState.items.partition { !it.isPaused }
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(activeItems, key = { it.id }) { item ->
                                            ItemGridCard(
                                                item = item,
                                                unitAbbr = item.unitId?.let { uiState.unitMap[it] },
                                                isSelected = item.id in uiState.selectedIds,
                                                selectionMode = uiState.selectionMode,
                                                onClick = {
                                                    if (uiState.selectionMode) {
                                                        viewModel.toggleSelection(item.id)
                                                    } else {
                                                        navController.navigate(Screen.ItemDetail.createRoute(item.id))
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!uiState.selectionMode) {
                                                        viewModel.enterSelectionMode(item.id)
                                                    }
                                                },
                                                onFavorite = { viewModel.toggleFavorite(item.id) },
                                                onAddToShopping = { showShoppingSheet(item.id, null) },
                                                modifier = Modifier.animateItemPlacement()
                                            )
                                        }
                                        if (pausedItems.isNotEmpty()) {
                                            item(span = { GridItemSpan(maxLineSpan) }, key = "paused_header") {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp, bottom = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                                    Text(
                                                        "Paused (${pausedItems.size})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            items(pausedItems, key = { it.id }) { item ->
                                                ItemGridCard(
                                                    item = item,
                                                    unitAbbr = item.unitId?.let { uiState.unitMap[it] },
                                                    isSelected = item.id in uiState.selectedIds,
                                                    selectionMode = uiState.selectionMode,
                                                    onClick = {
                                                        if (uiState.selectionMode) {
                                                            viewModel.toggleSelection(item.id)
                                                        } else {
                                                            navController.navigate(Screen.ItemDetail.createRoute(item.id))
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!uiState.selectionMode) {
                                                            viewModel.enterSelectionMode(item.id)
                                                        }
                                                    },
                                                    onFavorite = { viewModel.toggleFavorite(item.id) },
                                                    onAddToShopping = { showShoppingSheet(item.id, null) },
                                                    modifier = Modifier.animateItemPlacement().graphicsLayer { alpha = 0.6f }
                                                )
                                            }
                                        }
                                    }
                                }
                                ViewMode.LIST -> {
                                    val (activeItems, pausedItems) = uiState.items.partition { !it.isPaused }
                                    LazyColumn(
                                        contentPadding = PaddingValues(bottom = 80.dp)
                                    ) {
                                        items(activeItems, key = { it.id }) { item ->
                                            ItemListRow(
                                                item = item,
                                                unitAbbr = item.unitId?.let { uiState.unitMap[it] },
                                                isSelected = item.id in uiState.selectedIds,
                                                selectionMode = uiState.selectionMode,
                                                onClick = {
                                                    if (uiState.selectionMode) {
                                                        viewModel.toggleSelection(item.id)
                                                    } else {
                                                        navController.navigate(Screen.ItemDetail.createRoute(item.id))
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!uiState.selectionMode) {
                                                        viewModel.enterSelectionMode(item.id)
                                                    }
                                                },
                                                onFavorite = { viewModel.toggleFavorite(item.id) },
                                                onAddToShopping = { showShoppingSheet(item.id, null) },
                                                modifier = Modifier.animateItemPlacement()
                                            )
                                        }
                                        if (pausedItems.isNotEmpty()) {
                                            item(key = "paused_header") {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                                    Text(
                                                        "Paused (${pausedItems.size})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            items(pausedItems, key = { it.id }) { item ->
                                                ItemListRow(
                                                    item = item,
                                                    unitAbbr = item.unitId?.let { uiState.unitMap[it] },
                                                    isSelected = item.id in uiState.selectedIds,
                                                    selectionMode = uiState.selectionMode,
                                                    onClick = {
                                                        if (uiState.selectionMode) {
                                                            viewModel.toggleSelection(item.id)
                                                        } else {
                                                            navController.navigate(Screen.ItemDetail.createRoute(item.id))
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!uiState.selectionMode) {
                                                            viewModel.enterSelectionMode(item.id)
                                                        }
                                                    },
                                                    onFavorite = { viewModel.toggleFavorite(item.id) },
                                                    onAddToShopping = { showShoppingSheet(item.id, null) },
                                                    modifier = Modifier.animateItemPlacement().graphicsLayer { alpha = 0.6f }
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ItemGridCard(
    item: ItemEntity,
    unitAbbr: String? = null,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToShopping: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "selectionScale"
    )

    // Effective min: manual minQuantity if set, otherwise smart (peak) min
    val effectiveMin = if (item.minQuantity > 0) item.minQuantity else item.smartMinQuantity
    val hasEffectiveMin = effectiveMin > 0
    val stockRatio = if (hasEffectiveMin) (item.quantity / effectiveMin).toFloat().coerceIn(0f, 1f) else 1f
    val stockColor = when {
        !hasEffectiveMin -> null
        item.quantity <= 0 -> ExpiryRed
        stockRatio < 0.3f -> ExpiryRed
        stockRatio < 0.6f -> StockYellow
        else -> StockGreen
    }

    val statusColor = when {
        item.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }?.let { it < 0 } == true -> ExpiryRed
        item.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }?.let { it <= item.expiryWarningDays } == true -> ExpiryOrange
        stockColor == ExpiryRed -> ExpiryRed
        stockColor == StockYellow -> StockYellow
        else -> null
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (statusColor != null) Modifier.border(
                    width = 1.5.dp,
                    color = statusColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else null
    ) {
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 120.dp)
                .padding(14.dp)
        ) {
            // Top row: selection check or name + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (selectionMode) {
                    Icon(
                        if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.brand?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!selectionMode) {
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(16.dp),
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stock level indicator (when effective min is available)
            if (hasEffectiveMin) {
                val stockLabel = when {
                    item.quantity <= 0 -> "Out of stock"
                    stockRatio < 0.3f -> "Low"
                    stockRatio < 0.6f -> "Getting low"
                    else -> "OK"
                }
                val qtyText = buildString {
                    append(item.quantity.formatQty())
                    unitAbbr?.let { append(" $it") }
                    append(" / ${effectiveMin.formatQty()}")
                    unitAbbr?.let { append(" $it") }
                    append(" · $stockLabel")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = qtyText,
                        style = MaterialTheme.typography.labelSmall,
                        color = stockColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!selectionMode) {
                        IconButton(
                            onClick = onAddToShopping,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.AddShoppingCart,
                                contentDescription = "Add to shopping list",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { stockRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = stockColor ?: StockGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                // No min quantity — simple qty display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append(item.quantity.formatQty())
                            unitAbbr?.let { append(" $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (!selectionMode) {
                        IconButton(
                            onClick = onAddToShopping,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.AddShoppingCart,
                                contentDescription = "Add to shopping list",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Expiry chips (stock status is now shown by the progress bar)
            val expiryChips = mutableListOf<Pair<String, Color>>()
            val expiry = item.expiryDate
            if (expiry != null) {
                val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                if (daysUntil < 0) {
                    expiryChips.add("Expired" to ExpiryRed)
                } else if (daysUntil <= item.expiryWarningDays) {
                    expiryChips.add("${daysUntil}d left" to ExpiryOrange)
                }
            }
            // Only show "Out of stock" chip — low stock is handled by progress bar
            if (hasEffectiveMin && item.quantity <= 0) {
                expiryChips.add("Out of stock" to ExpiryRed)
            }

            if (expiryChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    expiryChips.forEach { (text, color) ->
                        StatusChip(text = text, color = color)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemListRow(
    item: ItemEntity,
    unitAbbr: String? = null,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToShopping: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else Color.Transparent,
        animationSpec = tween(200),
        label = "rowBgColor"
    )

    val effectiveMin = if (item.minQuantity > 0) item.minQuantity else item.smartMinQuantity
    val hasEffectiveMin = effectiveMin > 0
    val stockRatio = if (hasEffectiveMin) (item.quantity / effectiveMin).toFloat().coerceIn(0f, 1f) else 1f
    val stockColor = when {
        !hasEffectiveMin -> null
        item.quantity <= 0 -> ExpiryRed
        stockRatio < 0.3f -> ExpiryRed
        stockRatio < 0.6f -> StockYellow
        else -> StockGreen
    }

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.brand?.let {
                    Text(
                        text = " · $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        supportingContent = {
            Column {
                // Quantity line
                val qtyText = buildString {
                    append(item.quantity.formatQty())
                    unitAbbr?.let { append(" $it") }
                    if (hasEffectiveMin) {
                        append(" / ${effectiveMin.formatQty()}")
                        unitAbbr?.let { append(" $it") }
                        val stockLabel = when {
                            item.quantity <= 0 -> "Out of stock"
                            stockRatio < 0.3f -> "Low"
                            stockRatio < 0.6f -> "Getting low"
                            else -> "OK"
                        }
                        append(" · $stockLabel")
                    }
                }
                Text(
                    text = qtyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = stockColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stock progress bar
                if (hasEffectiveMin) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { stockRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = stockColor ?: StockGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Expiry chips
                val expiryChips = mutableListOf<Pair<String, Color>>()
                val expiry = item.expiryDate
                if (expiry != null) {
                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                    if (daysUntil < 0) {
                        expiryChips.add("Expired" to ExpiryRed)
                    } else if (daysUntil <= item.expiryWarningDays) {
                        expiryChips.add("${daysUntil}d left" to ExpiryOrange)
                    }
                }
                if (hasEffectiveMin && item.quantity <= 0) {
                    expiryChips.add("Out of stock" to ExpiryRed)
                }
                if (expiryChips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        expiryChips.forEach { (text, color) ->
                            StatusChip(text = text, color = color)
                        }
                    }
                }
            }
        },
        leadingContent = if (selectionMode) {
            {
                Icon(
                    if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = {
            if (!selectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddToShopping, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.AddShoppingCart,
                            contentDescription = "Add to shopping list",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onFavorite, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(16.dp),
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(bgColor)
    )
}

@Composable
private fun StatusChip(
    text: String,
    color: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
