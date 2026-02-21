package com.inventory.app.ui.screens.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.inventory.app.ui.components.AppCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.DailyChartEntry
import com.inventory.app.ui.components.SpendingLineChart
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.components.ConfirmDialog
import com.inventory.app.ui.components.DropdownField
import com.inventory.app.ui.components.ExpandableSection
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.util.FormatUtils
import com.inventory.app.ui.theme.ExpiryOrange
import com.inventory.app.ui.theme.ExpiryRed
import com.inventory.app.ui.theme.StockGreen
import com.inventory.app.ui.theme.StockYellow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    navController: NavController,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showShoppingSheet = com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUsageDialog by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.item?.item?.name ?: "Item") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.item?.let { details ->
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (details.item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (details.item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(Screen.ItemForm.createRoute(itemId = details.item.id))
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
            uiState.item == null -> LoadingState()
            else -> {
                val details = uiState.item ?: return@Scaffold
                val item = details.item

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val effectiveMin = if (item.minQuantity > 0) item.minQuantity else item.smartMinQuantity

                    // Status badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.isPaused) StatusChip("Paused", MaterialTheme.colorScheme.outline)
                        item.expiryDate?.let { expiry ->
                            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                            when {
                                daysUntil < 0 -> StatusChip("Expired", ExpiryRed)
                                daysUntil <= item.expiryWarningDays -> StatusChip("Expiring Soon", ExpiryOrange)
                            }
                        }
                        if (item.quantity <= 0) StatusChip("Out of Stock", ExpiryRed)
                        else if (effectiveMin > 0 && item.quantity < effectiveMin) StatusChip("Low Stock", StockYellow)
                    }

                    // Quantity section with +/- controls
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Quantity", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${item.quantity.formatQty()} ${details.unit?.abbreviation ?: ""}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (effectiveMin > 0) {
                                    Text(
                                        "Target: ${effectiveMin.formatQty()}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = {
                                    viewModel.adjustQuantity(-1.0)
                                    scope.launch { snackbarHostState.showSnackbar("Quantity decreased") }
                                }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                IconButton(onClick = {
                                    viewModel.adjustQuantity(1.0)
                                    scope.launch { snackbarHostState.showSnackbar("Quantity increased") }
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }

                    // Key facts grid
                    val hasCategoryOrLocation = details.category != null || details.storageLocation != null
                    val hasExpiryOrPrice = item.expiryDate != null || item.purchasePrice != null

                    if (hasCategoryOrLocation || hasExpiryOrPrice) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (hasCategoryOrLocation) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        details.category?.let {
                                            InfoMiniCard(
                                                label = "Category",
                                                value = it.name,
                                                icon = Icons.Filled.Category,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        details.storageLocation?.let {
                                            InfoMiniCard(
                                                label = "Location",
                                                value = it.name,
                                                icon = Icons.Filled.Place,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                if (hasExpiryOrPrice) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item.expiryDate?.let { expiry ->
                                            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                                            val expiryColor = when {
                                                daysUntil < 0 -> ExpiryRed
                                                daysUntil <= item.expiryWarningDays -> ExpiryOrange
                                                else -> StockGreen
                                            }
                                            val expiryText = when {
                                                daysUntil < 0 -> "$expiry (expired)"
                                                daysUntil == 0L -> "Today"
                                                daysUntil == 1L -> "Tomorrow"
                                                else -> "$expiry ($daysUntil days)"
                                            }
                                            InfoMiniCard(
                                                label = "Expiry",
                                                value = expiryText,
                                                icon = Icons.Filled.Schedule,
                                                valueColor = expiryColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        item.purchasePrice?.let {
                                            InfoMiniCard(
                                                label = "Price",
                                                value = "${uiState.currencySymbol}${String.format("%.2f", it)}",
                                                icon = Icons.Filled.Payments,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    item.notes?.let { notes ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    // More details (expandable, only if secondary details exist)
                    val hasSecondaryDetails = item.brand != null || item.barcode != null ||
                        item.openedDate != null || item.daysAfterOpening != null ||
                        details.subcategory != null || item.purchaseDate != null

                    if (hasSecondaryDetails) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                ExpandableSection(title = "More Details", initiallyExpanded = false) {
                                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                        item.brand?.let { DetailRow("Brand", it) }
                                        item.barcode?.let { DetailRow("Barcode", it) }
                                        details.subcategory?.let { DetailRow("Subcategory", it.name) }
                                        item.purchaseDate?.let { DetailRow("Purchase Date", it.toString()) }
                                        item.openedDate?.let { DetailRow("Opened", it.toString()) }
                                        item.daysAfterOpening?.let { DetailRow("Good After Opening", "$it days") }
                                    }
                                }
                            }
                        }
                    }

                    // Quick actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { showUsageDialog = true },
                            label = { Text("Record Usage") },
                            leadingIcon = { Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp)) }
                        )
                        AssistChip(
                            onClick = { showPurchaseDialog = true },
                            label = { Text("Add Purchase") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp)) }
                        )
                        AssistChip(
                            onClick = { showShoppingSheet(item.id, null) },
                            label = { Text("Shopping") },
                            leadingIcon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Add to shopping", modifier = Modifier.size(18.dp)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                viewModel.togglePause()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (item.isPaused) "Alerts resumed" else "Alerts paused — no expiry or low stock warnings"
                                    )
                                }
                            },
                            label = { Text(if (item.isPaused) "Resume Alerts" else "Pause Alerts") },
                            leadingIcon = {
                                Icon(
                                    if (item.isPaused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
                                    contentDescription = if (item.isPaused) "Resume" else "Pause",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }

                    // History sections — conditional
                    val hasAnyHistory = uiState.usageLogs.isNotEmpty() || uiState.purchaseHistory.isNotEmpty()

                    if (hasAnyHistory) {
                        // Usage history
                        if (uiState.usageLogs.isNotEmpty()) {
                            Text("Usage History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    uiState.usageLogs.take(10).forEach { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    "${log.usageType.replaceFirstChar { it.uppercase() }} - ${log.quantity.formatQty()}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                log.notes?.let {
                                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(log.usageDate.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }

                        // Unit price trend chart
                        if (uiState.priceTrendData.size >= 2) {
                            Text(
                                "Unit Price Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                SpendingLineChart(
                                    entries = uiState.priceTrendData.map { point ->
                                        DailyChartEntry(
                                            label = FormatUtils.formatMonthDay(point.date),
                                            value = point.unitPrice.toFloat()
                                        )
                                    },
                                    currencySymbol = uiState.currencySymbol,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // Purchase history
                        if (uiState.purchaseHistory.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Purchase History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = {
                                    navController.navigate(Screen.PurchaseHistory.createRoute(details.item.id))
                                }) {
                                    Text("View All")
                                }
                            }
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    uiState.purchaseHistory.take(5).forEach { purchase ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        "Qty: ${purchase.quantity.formatQty()}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    purchase.totalPrice?.let {
                                                        Text(
                                                            "${uiState.currencySymbol}${String.format("%.2f", it)}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                purchase.unitPrice?.let {
                                                    Text(
                                                        "${uiState.currencySymbol}${String.format("%.2f", it)}/unit",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                purchase.expiryDate?.let { expiry ->
                                                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                                                    val expiryColor = when {
                                                        daysUntil < 0 -> MaterialTheme.colorScheme.error
                                                        daysUntil <= 7 -> ExpiryOrange
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                    Text(
                                                        "Expires: $expiry",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = expiryColor
                                                    )
                                                }
                                                purchase.notes?.let {
                                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(
                                                purchase.purchaseDate.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    } else {
                        // Single subtle hint for fresh items
                        Text(
                            "Stats will appear as you track usage and purchases",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Item",
            message = "Are you sure you want to delete this item?",
            onConfirm = {
                viewModel.deleteItem()
                showDeleteDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Item deleted")
                    navController.navigateUp()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showUsageDialog) {
        RecordUsageDialog(
            onDismiss = { showUsageDialog = false },
            onConfirm = { qty, type, notes ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.logUsage(qty, type, notes)
                showUsageDialog = false
                scope.launch { snackbarHostState.showSnackbar("Usage recorded") }
            }
        )
    }

    if (showPurchaseDialog) {
        AddPurchaseDialog(
            onDismiss = { showPurchaseDialog = false },
            onConfirm = { qty, price, store, notes ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addPurchase(qty, price, store, notes)
                showPurchaseDialog = false
                scope.launch { snackbarHostState.showSnackbar("Purchase recorded") }
            },
            currencySymbol = uiState.currencySymbol
        )
    }
}

@Composable
private fun RecordUsageDialog(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, usageType: String, notes: String?) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    val usageTypes = listOf("consumed", "wasted", "expired", "gifted")
    var selectedType by remember { mutableStateOf("consumed") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Usage") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                DropdownField(
                    label = "Usage Type",
                    options = usageTypes,
                    selectedOption = selectedType,
                    onOptionSelected = { it?.let { selectedType = it } },
                    optionLabel = { it.replaceFirstChar { c -> c.uppercase() } },
                    modifier = Modifier.fillMaxWidth(),
                    allowNone = false
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: return@TextButton
                    if (qty <= 0) return@TextButton
                    onConfirm(qty, selectedType, notes.ifBlank { null })
                }
            ) { Text("Record") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddPurchaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, totalPrice: Double?, storeName: String?, notes: String?) -> Unit,
    currencySymbol: String = ""
) {
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Purchase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Total Price") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text(currencySymbol) }
                    )
                }
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text("Store (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: return@TextButton
                    if (qty <= 0) return@TextButton
                    val pr = price.toDoubleOrNull()
                    if (price.isNotBlank() && (pr == null || pr < 0)) return@TextButton
                    onConfirm(qty, pr, store.ifBlank { null }, notes.ifBlank { null })
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InfoMiniCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    AppCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    AppCard(
        containerColor = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

