package com.inventory.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.inventory.app.ui.components.ThemedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventory.app.domain.model.InventorySuggestion

/**
 * A name TextField that shows a dropdown of matching inventory items as the user types.
 * Selecting a suggestion fills both name and unit fields.
 *
 * Uses [DropdownMenu] so it works inside HorizontalPager without layout conflicts.
 */
@Composable
fun IngredientAutoSuggest(
    nameValue: String,
    amountValue: String,
    unitValue: String,
    suggestions: List<InventorySuggestion>,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,     // triggers debounced search in ViewModel
    onSuggestionSelected: (InventorySuggestion) -> Unit,
    onClearSuggestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Show dropdown when there are suggestions and user is typing
    dropdownExpanded = suggestions.isNotEmpty() && nameValue.length >= 2

    Column(modifier = modifier) {
        // Name field with suggestion dropdown
        Column {
            ThemedTextField(
                value = nameValue,
                onValueChange = { text ->
                    onNameChange(text)
                    onQueryChange(text)
                },
                label = { Text("Ingredient name") },
                placeholder = { Text("e.g. Ghee, Tomatoes…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = {
                    dropdownExpanded = false
                    onClearSuggestions()
                }
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Column {
                                    Text(
                                        text = suggestion.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (suggestion.unit.isNotBlank()) {
                                        Text(
                                            text = "in ${suggestion.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Amount + Unit row
        Row(modifier = Modifier.fillMaxWidth()) {
            ThemedTextField(
                value = amountValue,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                placeholder = { Text("2, ½, a pinch…") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ThemedTextField(
                value = unitValue,
                onValueChange = onUnitChange,
                label = { Text("Unit") },
                placeholder = { Text("g, cups…") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
