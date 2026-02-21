package com.inventory.app.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import com.inventory.app.ui.components.AnimatedSaveButton
import com.inventory.app.ui.components.AppCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inventory.app.util.CategoryVisuals
import androidx.navigation.NavController

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

val categoryIcons = listOf(
    "category" to Icons.Filled.Category,
    "dairy" to Icons.Filled.EggAlt,
    "meat" to Icons.Filled.SetMeal,
    "produce" to Icons.Filled.Grass,
    "frozen" to Icons.Filled.AcUnit,
    "bakery" to Icons.Filled.BreakfastDining,
    "beverages" to Icons.Filled.LocalDrink,
    "snacks" to Icons.Filled.Cookie,
    "grains" to Icons.Filled.RiceBowl,
    "spices" to Icons.Filled.Spa,
    "condiments" to Icons.Filled.Blender,
    "canned" to Icons.Filled.FoodBank,
    "cleaning" to Icons.Filled.CleaningServices,
    "pets" to Icons.Filled.Pets,
    "desserts" to Icons.Filled.Cake,
    "pizza" to Icons.Filled.LocalPizza,
    "tea" to Icons.Filled.EmojiFoodBeverage,
    "water" to Icons.Filled.WaterDrop,
    "kitchen" to Icons.Filled.Kitchen,
)

fun getCategoryIcon(iconName: String?): ImageVector {
    return categoryIcons.find { it.first == iconName }?.second ?: Icons.Filled.Category
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryFormScreen(
    navController: NavController,
    categoryId: Long? = null,
    viewModel: CategoryFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(categoryId) {
        categoryId?.let { viewModel.loadCategory(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Category saved")
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.nameError, uiState.nameErrorTrigger) {
        uiState.nameError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (categoryId != null) "Edit Category" else "Add Category") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
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

            // Icon picker
            Text("Icon", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryIcons.forEach { (key, icon) ->
                        val isSelected = uiState.icon == key
                        val label = key.replaceFirstChar { it.uppercase() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.updateIcon(key) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Color picker
            Text("Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                    Icons.Filled.Category,
                                    contentDescription = name,
                                    modifier = Modifier.size(18.dp),
                                    tint = CategoryVisuals.contrastColor(swatchColor)
                                )
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = uiState.color,
                onValueChange = { viewModel.updateColor(it) },
                label = { Text("Custom color (hex)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("#6c757d") }
            )

            OutlinedTextField(
                value = uiState.sortOrder.toString(),
                onValueChange = { viewModel.updateSortOrder(it.toIntOrNull() ?: 0) },
                label = { Text("Sort Order") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

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
                Switch(
                    checked = uiState.isActive,
                    onCheckedChange = null
                )
            }

            AnimatedSaveButton(
                text = if (categoryId != null) "Update Category" else "Create Category",
                onClick = { viewModel.save() },
                isSaved = uiState.isSaved
            )
        }
    }
}
