package com.inventory.app.ui.screens.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inventory.app.domain.model.Priority
import com.inventory.app.ui.components.AutoCompleteTextField
import com.inventory.app.ui.components.DropdownField

data class SheetRequest(
    val itemId: Long? = null,
    val shoppingItemId: Long? = null
)

val LocalShowAddShoppingSheet = compositionLocalOf<(Long?, Long?) -> Unit> { { _, _ -> } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingItemSheet(
    request: SheetRequest,
    onDismiss: () -> Unit,
    viewModel: AddShoppingItemViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Reset + load when request changes
    LaunchedEffect(request) {
        viewModel.reset()
        request.itemId?.let { viewModel.loadFromItem(it) }
        request.shoppingItemId?.let { viewModel.loadShoppingItem(it) }
    }

    // Auto-dismiss on save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onDismiss()
    }

    // Waste warning dialog renders above the sheet
    if (uiState.showWasteWarning) {
        WasteWarningDialog(
            matches = uiState.wasteWarningMatches,
            onDismiss = { viewModel.dismissWasteWarning() },
            onAddAnyway = { viewModel.proceedWithSave() }
        )
    }

    val isEditMode = uiState.isEditMode
    val title = if (isEditMode) "Edit Shopping Item" else "Add to Shopping List"
    val buttonText = if (isEditMode) "Save Changes" else "Add to Shopping List"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title + close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            // Item name field
            if (request.itemId != null || (isEditMode && uiState.itemId != null)) {
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

            // Quantity + Unit row
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

            // Priority
            DropdownField(
                label = "Priority",
                options = Priority.entries.toList(),
                selectedOption = uiState.priority,
                onOptionSelected = { it?.let { viewModel.updatePriority(it) } },
                optionLabel = { it.label },
                modifier = Modifier.fillMaxWidth(),
                allowNone = false
            )

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Save button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = request.itemId != null || uiState.customName.isNotBlank() || uiState.itemId != null
            ) {
                Text(buttonText)
            }
        }
    }
}
