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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

                    // Details grid
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow("Category", details.category?.name ?: "None")
                            details.subcategory?.let { DetailRow("Subcategory", it.name) }
                            DetailRow("Location", details.storageLocation?.name ?: "None")
                            item.brand?.let { DetailRow("Brand", it) }
                            item.barcode?.let { DetailRow("Barcode", it) }
                            item.expiryDate?.let {
                                val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), it)
                                DetailRow("Expiry Date", "$it ($daysUntil days)")
                            }
                            item.purchaseDate?.let { DetailRow("Purchase Date", it.toString()) }
                            item.purchasePrice?.let { DetailRow("Total Price", "${uiState.currencySymbol}${String.format("%.2f", it)}") }
                            item.openedDate?.let { DetailRow("Opened", it.toString()) }
                            item.daysAfterOpening?.let { DetailRow("Good After Opening", "$it days") }
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
                            onClick = { navController.navigate(Screen.AddShoppingItem.createRoute(item.id)) },
                            label = { Text("Shopping") },
                            leadingIcon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Add to shopping", modifier = Modifier.size(18.dp)) }
                        )
                    }

                    // Usage history
                    Text("Usage History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (uiState.usageLogs.isEmpty()) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "No usage recorded yet. Tap 'Record Usage' above.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Purchase History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.purchaseHistory.isNotEmpty()) {
                            TextButton(onClick = {
                                navController.navigate(Screen.PurchaseHistory.createRoute(details.item.id))
                            }) {
                                Text("View All")
                            }
                        }
                    }
                    if (uiState.purchaseHistory.isEmpty()) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "No purchases recorded yet. Tap 'Add Purchase' above.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
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
                navController.navigateUp()
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
private fun StatusChip(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
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

