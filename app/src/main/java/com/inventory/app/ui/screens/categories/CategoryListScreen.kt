package com.inventory.app.ui.screens.categories

import com.inventory.app.ui.components.ThemedSnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.inventory.app.ui.components.ThemedFab
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.PageHeader
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.data.local.dao.CategoryWithItemCountRow
import com.inventory.app.ui.components.ConfirmDialog
import com.inventory.app.ui.components.EmptyState
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.util.CategoryVisuals
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.appColors
import kotlinx.coroutines.launch

@Composable
fun CategoryListScreen(
    navController: NavController,
    viewModel: CategoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<CategoryWithItemCountRow?>(null) }

    PageScaffold(
        onBack = { navController.popBackStack() },
        floatingActionButton = {
            ThemedFab(
                onClick = { navController.navigate(Screen.CategoryForm.createRoute()) }
            ) {
                ThemedIcon(materialIcon = Icons.Filled.Add, inkIconRes = R.drawable.ic_ink_add, contentDescription = "Add Category")
            }
        },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.categories.isEmpty() -> EmptyState(
                icon = Icons.Filled.Category,
                title = "No Categories",
                message = "Add your first category to organize items",
                actionLabel = "Add Category",
                onAction = { navController.navigate(Screen.CategoryForm.createRoute()) }
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                item { PageHeader("Categories") }
                itemsIndexed(uiState.categories, key = { _, it -> it.id }) { index, category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        supportingContent = {
                            Text("${category.itemCount} items")
                        },
                        leadingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Reorder buttons
                                Column {
                                    IconButton(
                                        onClick = { viewModel.moveCategory(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        ThemedIcon(
                                            materialIcon = Icons.Filled.KeyboardArrowUp,
                                            inkIconRes = R.drawable.ic_ink_collapse,
                                            contentDescription = "Move up",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.moveCategory(index, index + 1) },
                                        enabled = index < uiState.categories.size - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        ThemedIcon(
                                            materialIcon = Icons.Filled.KeyboardArrowDown,
                                            inkIconRes = R.drawable.ic_ink_expand,
                                            contentDescription = "Move down",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                val defaultColor = MaterialTheme.appColors.entityDefaultColor
                                val bgColor = try {
                                    category.color?.toColorInt()?.let { Color(it) } ?: defaultColor
                                } catch (_: Exception) {
                                    defaultColor
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val visual = CategoryVisuals.get(category.name)
                                    val resolvedIcon = if (visual.icon != Icons.Filled.Category) {
                                        visual.icon // Known category — use curated icon
                                    } else {
                                        getCategoryIcon(category.icon) // Custom category — use DB icon
                                    }
                                    ThemedIcon(
                                        materialIcon = resolvedIcon,
                                        contentDescription = "Category icon",
                                        modifier = Modifier.size(24.dp),
                                        tint = CategoryVisuals.contrastColor(bgColor)
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    navController.navigate(Screen.CategoryForm.createRoute(category.id))
                                }) {
                                    ThemedIcon(materialIcon = Icons.Filled.Edit, inkIconRes = R.drawable.ic_ink_edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { deleteTarget = category }) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.Delete,
                                        inkIconRes = R.drawable.ic_ink_delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                ThemedIcon(materialIcon = Icons.Filled.ChevronRight, inkIconRes = R.drawable.ic_ink_chevron_right, contentDescription = "View subcategories")
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.SubcategoryList.createRoute(category.id))
                        }
                    )
                }
            }
        }
    }

    deleteTarget?.let { category ->
        ConfirmDialog(
            title = "Delete Category",
            message = "Are you sure you want to delete \"${category.name}\"? Items in this category will become uncategorized.",
            onConfirm = {
                val deletedId = category.id
                val deletedName = category.name
                viewModel.deleteCategory(deletedId)
                deleteTarget = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "\"$deletedName\" deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreCategory(deletedId)
                    }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
