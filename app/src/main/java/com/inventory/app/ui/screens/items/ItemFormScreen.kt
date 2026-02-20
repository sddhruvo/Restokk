package com.inventory.app.ui.screens.items

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AutoCompleteTextField
import com.inventory.app.ui.components.DatePickerField
import com.inventory.app.ui.components.AnimatedSaveButton
import com.inventory.app.ui.components.DropdownField
import com.inventory.app.ui.components.FormProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    navController: NavController,
    itemId: Long? = null,
    barcode: String? = null,
    name: String? = null,
    brand: String? = null,
    quantity: String? = null,
    viewModel: ItemFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Camera launcher for expiry date scanning
    val expiryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.scanExpiryDate(it) }
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.expiryScanError) {
        uiState.expiryScanError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    LaunchedEffect(itemId) {
        itemId?.let { viewModel.loadItem(it) }
    }

    LaunchedEffect(barcode, name, brand, quantity) {
        if (itemId == null) {
            viewModel.prefill(barcode, name, brand, quantity)
        }
    }

    // Navigate back when saved AND no pending matches to resolve
    var hasNavigated by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isSaved, uiState.pendingMatches) {
        if (uiState.isSaved && uiState.pendingMatches.isEmpty() && !hasNavigated) {
            hasNavigated = true
            navController.navigateUp()
        }
    }

    // Confirmation dialog for medium-confidence matches
    val currentPendingMatch = uiState.pendingMatches.firstOrNull()
    if (currentPendingMatch != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMatch(currentPendingMatch) },
            title = { Text("Shopping List Match Found") },
            text = {
                Text("We found \"${currentPendingMatch.shoppingItemName}\" on your shopping list that may match \"${uiState.name}\". Would you like to mark it as purchased?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmMatch(currentPendingMatch) }) {
                    Text("Mark as Purchased")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMatch(currentPendingMatch) }) {
                    Text("No Thanks")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId != null) "Edit Item" else "Add Item") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) showDiscardDialog = true
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val scrollState = rememberScrollState()
        val scrollProgress = if (scrollState.maxValue > 0) {
            scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        } else 0f
        val sectionNames = listOf("Basic Info", "Category", "Quantity", "Expiry", "Purchase")
        val currentSection = sectionNames[(scrollProgress * (sectionNames.size - 1)).toInt().coerceIn(0, sectionNames.lastIndex)]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FormProgressIndicator(
                progress = scrollProgress,
                sectionName = currentSection,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Text(
                "* Required field. All other fields are optional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Basic Info Section
            SectionHeader("Basic Information")

            AutoCompleteTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                suggestions = uiState.nameSuggestions,
                onSuggestionSelected = { viewModel.selectSuggestion(it) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.barcode,
                    onValueChange = { viewModel.updateBarcode(it) },
                    label = { Text("Barcode") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.brand,
                    onValueChange = { viewModel.updateBrand(it) },
                    label = { Text("Brand") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.smartDefaultsApplied) {
                AssistChip(
                    onClick = {},
                    label = { Text("Smart defaults applied based on item name") },
                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Categorization Section
            SectionHeader("Categorization")

            DropdownField(
                label = "Category",
                options = uiState.categories,
                selectedOption = uiState.categories.find { it.id == uiState.selectedCategoryId },
                onOptionSelected = { viewModel.selectCategory(it?.id) },
                optionLabel = { it.name },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.subcategories.isNotEmpty()) {
                DropdownField(
                    label = "Subcategory",
                    options = uiState.subcategories,
                    selectedOption = uiState.subcategories.find { it.id == uiState.selectedSubcategoryId },
                    onOptionSelected = { viewModel.selectSubcategory(it?.id) },
                    optionLabel = { it.name },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DropdownField(
                label = "Storage Location",
                options = uiState.locations,
                selectedOption = uiState.locations.find { it.id == uiState.selectedLocationId },
                onOptionSelected = { viewModel.selectLocation(it?.id) },
                optionLabel = { it.name },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Quantity Section
            SectionHeader("Quantity")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.quantity,
                    onValueChange = { viewModel.updateQuantity(it) },
                    label = { Text("Quantity") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.quantityError != null,
                    supportingText = uiState.quantityError?.let { { Text(it) } }
                )
                DropdownField(
                    label = "Unit",
                    options = uiState.units,
                    selectedOption = uiState.units.find { it.id == uiState.selectedUnitId },
                    onOptionSelected = { viewModel.selectUnit(it?.id) },
                    optionLabel = { "${it.name} (${it.abbreviation})" },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.minQuantity,
                    onValueChange = { viewModel.updateMinQuantity(it) },
                    label = { Text("Min Qty") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.minQuantityError != null,
                    supportingText = { Text(uiState.minQuantityError ?: "Alert when below") }
                )
                OutlinedTextField(
                    value = uiState.maxQuantity,
                    onValueChange = { viewModel.updateMaxQuantity(it) },
                    label = { Text("Max Qty") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.maxQuantityError != null,
                    supportingText = { Text(uiState.maxQuantityError ?: "Storage capacity") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Expiry Section
            SectionHeader("Expiry Tracking")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                DatePickerField(
                    value = uiState.expiryDate,
                    onDateSelected = { viewModel.updateExpiryDate(it) },
                    label = { Text("Expiry Date") },
                    modifier = Modifier.weight(1f),
                    isError = uiState.expiryDateError != null,
                    supportingText = uiState.expiryDateError?.let { { Text(it) } }
                )
                // AI expiry scan button
                if (uiState.isScanningExpiry) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(top = 16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = { expiryPhotoLauncher.launch(null) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Scan expiry date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                OutlinedTextField(
                    value = uiState.expiryWarningDays,
                    onValueChange = { viewModel.updateExpiryWarningDays(it) },
                    label = { Text("Warning Days") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Days before expiry to warn") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePickerField(
                    value = uiState.openedDate,
                    onDateSelected = { viewModel.updateOpenedDate(it) },
                    label = { Text("Opened Date") },
                    modifier = Modifier.weight(1f),
                    isError = uiState.openedDateError != null,
                    supportingText = uiState.openedDateError?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = uiState.daysAfterOpening,
                    onValueChange = { viewModel.updateDaysAfterOpening(it) },
                    label = { Text("Use Within") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Days after opening") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Purchase Section
            SectionHeader("Purchase Info")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePickerField(
                    value = uiState.purchaseDate,
                    onDateSelected = { viewModel.updatePurchaseDate(it) },
                    label = { Text("Purchase Date") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.purchasePrice,
                    onValueChange = { viewModel.updatePurchasePrice(it) },
                    label = { Text("Total Price") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(uiState.currencySymbol) },
                    isError = uiState.priceError != null,
                    supportingText = uiState.priceError?.let { { Text(it) } }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Other
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Favorite")
                Switch(
                    checked = uiState.isFavorite,
                    onCheckedChange = { viewModel.updateFavorite(it) }
                )
            }

            AnimatedSaveButton(
                text = if (itemId != null) "Update Item" else "Create Item",
                onClick = { viewModel.save() },
                isLoading = uiState.isSaving,
                isSaved = uiState.isSaved
            )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}
