package com.inventory.app.ui.screens.locations

import com.inventory.app.ui.components.ThemedTextField
import com.inventory.app.ui.components.ThemedSnackbarHost
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.inventory.app.ui.components.ThemedAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.PageHeader
import androidx.compose.material3.SnackbarHostState
import com.inventory.app.ui.components.ThemedSwitch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import com.inventory.app.ui.theme.formSectionLabel
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.RegisterNavigationGuard
import com.inventory.app.domain.model.TemperatureZone
import com.inventory.app.ui.components.SaveAction
import com.inventory.app.ui.components.DropdownField
import com.inventory.app.util.CategoryVisuals

private val presetColors = listOf(
    "#F44336" to "Red",
    "#E91E63" to "Pink",
    "#9C27B0" to "Purple",
    "#673AB7" to "Deep Purple",
    "#3F51B5" to "Indigo",
    "#2196F3" to "Blue",
    "#03A9F4" to "Light Blue",
    "#00BCD4" to "Cyan",
    "#009688" to "Teal",
    "#4CAF50" to "Green",
    "#8BC34A" to "Light Green",
    "#CDDC39" to "Lime",
    "#FFC107" to "Amber",
    "#FF9800" to "Orange",
    "#FF5722" to "Deep Orange",
    "#795548" to "Brown",
    "#607D8B" to "Blue Grey",
    "#6c757d" to "Grey",
)

private fun parseHexColor(hex: String): Color? {
    return try {
        val cleaned = hex.removePrefix("#")
        if (cleaned.length == 6) Color(android.graphics.Color.parseColor("#$cleaned"))
        else null
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationFormScreen(
    navController: NavController,
    locationId: Long? = null,
    viewModel: LocationFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = uiState.hasBeenTouched && !uiState.isSaved

    // Guard bottom nav taps when form has unsaved changes
    RegisterNavigationGuard(
        shouldBlock = { isDirty },
        message = { "You have unsaved changes. Discard and leave?" }
    )

    BackHandler(enabled = isDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        ThemedAlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    navController.popBackStack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    LaunchedEffect(locationId) {
        locationId?.let { viewModel.loadLocation(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Location saved")
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val formTitle = if (locationId != null) "Edit Location" else "Add Location"

    PageScaffold(
        onBack = {
            if (isDirty) showDiscardDialog = true
            else navController.popBackStack()
        },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) },
        actions = {
            SaveAction(
                visible = isDirty || uiState.isSaved,
                onClick = { viewModel.save() },
                isSaved = uiState.isSaved
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PageHeader(formTitle)
            ThemedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } }
            )

            ThemedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            DropdownField(
                label = "Temperature Zone",
                options = TemperatureZone.entries.toList(),
                selectedOption = uiState.temperatureZone,
                onOptionSelected = { viewModel.updateTemperatureZone(it) },
                optionLabel = { it.label },
                modifier = Modifier.fillMaxWidth()
            )

            // Color picker
            Text("Color", style = MaterialTheme.typography.formSectionLabel)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { (hex, name) ->
                        val isSelected = uiState.color.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex) ?: Color.Gray)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                .clickable(onClickLabel = "Select $name") { viewModel.updateColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                val swatchColor = parseHexColor(hex) ?: Color.Gray
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = name,
                                    modifier = Modifier.size(18.dp),
                                    tint = CategoryVisuals.contrastColor(swatchColor)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = uiState.isActive,
                        onValueChange = { viewModel.updateIsActive(it) },
                        role = Role.Switch
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active")
                ThemedSwitch(
                    checked = uiState.isActive,
                    onCheckedChange = null
                )
            }

        }
    }
}
