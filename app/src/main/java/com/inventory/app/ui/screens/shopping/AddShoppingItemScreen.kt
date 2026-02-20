package com.inventory.app.ui.screens.shopping

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.domain.model.Priority
import com.inventory.app.ui.components.AutoCompleteTextField
import com.inventory.app.ui.components.AnimatedSaveButton
import com.inventory.app.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingItemScreen(
    navController: NavController,
    itemId: Long? = null,
    shoppingItemId: Long? = null,
    viewModel: AddShoppingItemViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val hasChanges = uiState.customName.isNotBlank() || uiState.notes.isNotBlank() ||
        uiState.quantity != "1" || uiState.priority != Priority.NORMAL
    val isEditMode = uiState.isEditMode

    BackHandler(enabled = hasChanges) {
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

    // Waste-avoidance warning dialog
    if (uiState.showWasteWarning) {
        WasteWarningDialog(
            matches = uiState.wasteWarningMatches,
            onDismiss = { viewModel.dismissWasteWarning() },
            onAddAnyway = { viewModel.proceedWithSave() }
        )
    }

    LaunchedEffect(itemId) {
        itemId?.let { viewModel.loadFromItem(it) }
    }

    LaunchedEffect(shoppingItemId) {
        shoppingItemId?.let { viewModel.loadShoppingItem(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) navController.popBackStack()
    }

    val title = if (isEditMode) "Edit Shopping Item" else "Add to Shopping List"
    val buttonText = if (isEditMode) "Save Changes" else "Add to Shopping List"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showDiscardDialog = true
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Item selection or custom name
            if (itemId != null || (isEditMode && uiState.itemId != null)) {
                OutlinedTextField(
                    value = uiState.itemName,
                    onValueChange = {},
                    label = { Text("Item") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
            } else {
                AutoCompleteTextField(
                    value = uiState.customName,
                    onValueChange = { viewModel.updateCustomName(it) },
                    suggestions = uiState.nameSuggestions,
                    onSuggestionSelected = { viewModel.selectSuggestion(it) },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.quantity,
                    onValueChange = { viewModel.updateQuantity(it) },
                    label = { Text("Quantity") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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

            DropdownField(
                label = "Priority",
                options = Priority.entries.toList(),
                selectedOption = uiState.priority,
                onOptionSelected = { it?.let { viewModel.updatePriority(it) } },
                optionLabel = { it.label },
                modifier = Modifier.fillMaxWidth(),
                allowNone = false
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            AnimatedSaveButton(
                text = buttonText,
                onClick = { viewModel.save() },
                isSaved = uiState.isSaved
            )
        }
    }
}

@Composable
internal fun WasteWarningDialog(
    matches: List<WasteWarningMatch>,
    onDismiss: () -> Unit,
    onAddAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Already in Inventory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "You may already have this item:",
                    style = MaterialTheme.typography.bodyMedium
                )
                matches.forEach { match ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            "${match.itemName} — ${match.quantity}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        val details = listOfNotNull(
                            match.locationName?.let { "in $it" },
                            match.expiryInfo
                        )
                        if (details.isNotEmpty()) {
                            Text(
                                details.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddAnyway) {
                Text("Add Anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
