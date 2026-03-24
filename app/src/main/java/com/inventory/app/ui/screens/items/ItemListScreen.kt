package com.inventory.app.ui.screens.items

import com.inventory.app.ui.navigation.LocalBottomNavHeight
import com.inventory.app.ui.components.ThemedDropdownMenu
import com.inventory.app.ui.components.ThemedFilterChip
import com.inventory.app.ui.components.ThemedSnackbarHost
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
import com.inventory.app.ui.components.ThemedAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import com.inventory.app.ui.components.ThemedDivider
import com.inventory.app.ui.components.ThemedProgressBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.inventory.app.ui.components.CollapsingPageScaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import com.inventory.app.R
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.components.computeStockBar
import com.inventory.app.ui.components.EmptyStateIllustration
import com.inventory.app.ui.components.AnimatedFab
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.appColors
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
    val lowStockPct by viewModel.lowStockThreshold.collectAsState()
    val lowStockRatio = (lowStockPct.toFloatOrNull() ?: 25f) / 100f
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchActive by rememberSaveable { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        ThemedAlertDialog(
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

    CollapsingPageScaffold(
        title = if (uiState.selectionMode) "${uiState.selectedIds.size} selected" else "Items",
        onBack = if (uiState.selectionMode) ({ viewModel.exitSelectionMode() }) else null,
        actions = {
            if (uiState.selectionMode) {
                IconButton(onClick = { viewModel.selectAll() }) {
                    ThemedIcon(materialIcon = Icons.Filled.SelectAll, inkIconRes = R.drawable.ic_ink_select_all, contentDescription = "Select all")
                }
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = uiState.selectedIds.isNotEmpty()
                ) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.Delete,
                        inkIconRes = R.drawable.ic_ink_delete,
                        contentDescription = "Delete selected",
                        tint = if (uiState.selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                IconButton(onClick = { searchActive = !searchActive }) {
                    ThemedIcon(materialIcon = Icons.Filled.Search, inkIconRes = R.drawable.ic_ink_search, contentDescription = "Search")
                }
                IconButton(onClick = { showSortMenu = true }) {
                    ThemedIcon(materialIcon = Icons.Filled.Sort, inkIconRes = R.drawable.ic_ink_sort, contentDescription = "Sort")
                }
                ThemedDropdownMenu(
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
        },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedFab(
                onClick = { navController.navigate(Screen.ItemForm.createRoute()) },
                icon = { ThemedIcon(materialIcon = Icons.Filled.Add, inkIconRes = R.drawable.ic_ink_add, contentDescription = "Add Item") },
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
                                ThemedIcon(materialIcon = Icons.Filled.Clear, inkIconRes = R.drawable.ic_ink_close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingSm)
                ) {}
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.items.isEmpty() -> EmptyStateIllustration(
                        icon = Icons.Filled.Inventory2,
                        headline = "This is where everything lives",
                        body = "Every item in your kitchen, organized and tracked. Let\u2019s add your first ones.",
                        ctaLabel = "Add item",
                        onCtaClick = { navController.navigate(Screen.ItemForm.createRoute()) },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> {
                        Crossfade(
                            targetState = uiState.viewMode,
                            animationSpec = tween(PaperInkMotion.DurationMedium),
                            label = "viewModeTransition"
                        ) { viewMode ->
                            when (viewMode) {
                                ViewMode.GRID -> {
                                    val (activeItems, pausedItems) = uiState.items.partition { !it.isPaused }
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
                                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                                    ) {
                                        // Filter chips — scroll with content
                                        item(span = { GridItemSpan(maxLineSpan) }, key = "filter_chips") {
                                            Column {
                                                AnimatedVisibility(
                                                    visible = !uiState.selectionMode,
                                                    enter = expandVertically() + fadeIn(),
                                                    exit = shrinkVertically() + fadeOut()
                                                ) {
                                                    LazyRow(
                                                        contentPadding = PaddingValues(horizontal = 0.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                                                        modifier = Modifier.padding(bottom = Dimens.spacingXs)
                                                    ) {
                                                        items(uiState.categories, key = { it.id }) { category ->
                                                            ThemedFilterChip(
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
                                            }
                                        }
                                        items(activeItems, key = { it.id }) { item ->
                                            ItemGridCard(
                                                item = item,
                                                unitAbbr = item.unitId?.let { uiState.unitMap[it] },
                                                isSelected = item.id in uiState.selectedIds,
                                                selectionMode = uiState.selectionMode,
                                                lowStockRatio = lowStockRatio,
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
                                                        .padding(top = Dimens.spacingSm, bottom = Dimens.spacingXs),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                                                ) {
                                                    ThemedDivider(modifier = Modifier.weight(1f))
                                                    Text(
                                                        "Paused (${pausedItems.size})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    ThemedDivider(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            items(pausedItems, key = { "paused_${it.id}" }) { item ->
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
                                        contentPadding = PaddingValues(horizontal = Dimens.spacingLg)
                                    ) {
                                        // Filter chips — scroll with content
                                        item(key = "filter_chips") {
                                            Column {
                                                AnimatedVisibility(
                                                    visible = !uiState.selectionMode,
                                                    enter = expandVertically() + fadeIn(),
                                                    exit = shrinkVertically() + fadeOut()
                                                ) {
                                                    LazyRow(
                                                        contentPadding = PaddingValues(horizontal = 0.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                                                        modifier = Modifier.padding(bottom = Dimens.spacingXs)
                                                    ) {
                                                        items(uiState.categories, key = { it.id }) { category ->
                                                            ThemedFilterChip(
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
                                            }
                                        }
                                        items(activeItems, key = { it.id }) { item ->
                                            ItemListRow(
                                                item = item,
                                                unitAbbr = item.unitId?.let { uiState.unitMap[it] },
                                                isSelected = item.id in uiState.selectedIds,
                                                selectionMode = uiState.selectionMode,
                                                lowStockRatio = lowStockRatio,
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
                                                        .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingSm),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                                                ) {
                                                    ThemedDivider(modifier = Modifier.weight(1f))
                                                    Text(
                                                        "Paused (${pausedItems.size})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    ThemedDivider(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            items(pausedItems, key = { "paused_${it.id}" }) { item ->
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
    lowStockRatio: Float = 0.25f,
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

    val stockState = computeStockBar(item.quantity, item.minQuantity, item.smartMinQuantity, item.maxQuantity, lowStockRatio)
    val stockRatio = stockState.ratio
    val appColors = MaterialTheme.appColors
    val stockColor = appColors.stockColor(stockRatio, stockState.threshold)
    val hasEffectiveMin = stockState.ceiling > 0 && (item.minQuantity > 0 || item.smartMinQuantity > 0 || item.maxQuantity?.let { it > 0 } == true)

    val statusColor = when {
        item.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }?.let { it < 0 } == true -> appColors.statusExpired
        item.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }?.let { it <= item.expiryWarningDays } == true -> appColors.statusExpiring
        stockColor == appColors.statusExpired -> appColors.statusExpired
        stockColor == appColors.statusLowStock -> appColors.statusLowStock
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
                    shape = MaterialTheme.shapes.large
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
                        ThemedIcon(
                            materialIcon = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            inkIconRes = if (item.isFavorite) R.drawable.ic_ink_heart else R.drawable.ic_ink_heart_outline,
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
                    stockRatio <= stockState.threshold -> "Low"
                    else -> "OK"
                }
                val qtyText = buildString {
                    append(item.quantity.formatQty())
                    unitAbbr?.let { append(" $it") }
                    append(" / ${stockState.ceiling.formatQty()}")
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
                        color = stockColor
                    )
                    if (!selectionMode) {
                        IconButton(
                            onClick = onAddToShopping,
                            modifier = Modifier.size(36.dp)
                        ) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.AddShoppingCart,
                                inkIconRes = R.drawable.ic_ink_add_to_cart,
                                contentDescription = "Add to shopping list",
                                modifier = Modifier.size(Dimens.iconSizeSm),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                ThemedProgressBar(
                    progress = { stockRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = stockColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                // No min quantity — qty display with stock bar
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
                        style = MaterialTheme.typography.labelLarge,
                        color = stockColor
                    )
                    if (!selectionMode) {
                        IconButton(
                            onClick = onAddToShopping,
                            modifier = Modifier.size(36.dp)
                        ) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.AddShoppingCart,
                                inkIconRes = R.drawable.ic_ink_add_to_cart,
                                contentDescription = "Add to shopping list",
                                modifier = Modifier.size(Dimens.iconSizeSm),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                ThemedProgressBar(
                    progress = { stockRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = stockColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Expiry chips (stock status is now shown by the progress bar)
            val expiryChips = mutableListOf<Pair<String, Color>>()
            val expiry = item.expiryDate
            if (expiry != null) {
                val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                if (daysUntil < 0) {
                    expiryChips.add("Expired" to appColors.statusExpired)
                } else if (daysUntil <= item.expiryWarningDays) {
                    expiryChips.add("${daysUntil}d left" to appColors.statusExpiring)
                }
            }
            // Only show "Out of stock" chip — low stock is handled by progress bar
            if (hasEffectiveMin && item.quantity <= 0) {
                expiryChips.add("Out of stock" to appColors.statusExpired)
            }

            if (expiryChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
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
    lowStockRatio: Float = 0.25f,
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

    val stockState = computeStockBar(item.quantity, item.minQuantity, item.smartMinQuantity, item.maxQuantity, lowStockRatio)
    val stockRatio = stockState.ratio
    val appColors = MaterialTheme.appColors
    val stockColor = appColors.stockColor(stockRatio, stockState.threshold)
    val hasEffectiveMin = item.minQuantity > 0 || item.smartMinQuantity > 0 || item.maxQuantity?.let { it > 0 } == true

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
                        append(" / ${stockState.ceiling.formatQty()}")
                        unitAbbr?.let { append(" $it") }
                        val stockLabel = when {
                            item.quantity <= 0 -> "Out of stock"
                            stockRatio <= stockState.threshold -> "Low"
                            else -> "OK"
                        }
                        append(" · $stockLabel")
                    }
                }
                Text(
                    text = qtyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = stockColor
                )

                // Stock progress bar (always shown)
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                ThemedProgressBar(
                    progress = { stockRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = stockColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Expiry chips
                val expiryChips = mutableListOf<Pair<String, Color>>()
                val expiry = item.expiryDate
                if (expiry != null) {
                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                    if (daysUntil < 0) {
                        expiryChips.add("Expired" to appColors.statusExpired)
                    } else if (daysUntil <= item.expiryWarningDays) {
                        expiryChips.add("${daysUntil}d left" to appColors.statusExpiring)
                    }
                }
                if (hasEffectiveMin && item.quantity <= 0) {
                    expiryChips.add("Out of stock" to appColors.statusExpired)
                }
                if (expiryChips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Dimens.spacingXs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
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
                        ThemedIcon(
                            materialIcon = Icons.Filled.AddShoppingCart,
                            inkIconRes = R.drawable.ic_ink_add_to_cart,
                            contentDescription = "Add to shopping list",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.spacingXs))
                    IconButton(onClick = onFavorite, modifier = Modifier.size(Dimens.iconSizeMd)) {
                        ThemedIcon(
                            materialIcon = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            inkIconRes = if (item.isFavorite) R.drawable.ic_ink_heart else R.drawable.ic_ink_heart_outline,
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
