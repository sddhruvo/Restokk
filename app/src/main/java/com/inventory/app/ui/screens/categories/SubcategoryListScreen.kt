package com.inventory.app.ui.screens.categories

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.data.local.entity.SubcategoryEntity
import com.inventory.app.ui.components.ConfirmDialog
import com.inventory.app.ui.components.EmptyState
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryListScreen(
    navController: NavController,
    categoryId: Long,
    viewModel: SubcategoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<SubcategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.categoryName?.let { "$it - Subcategories" } ?: "Subcategories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.SubcategoryForm.createRoute(categoryId = categoryId)) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Subcategory")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.subcategories.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Filled.Label,
                title = "No Subcategories",
                message = "Add subcategories to further organize items"
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(uiState.subcategories, key = { it.id }) { sub ->
                    ListItem(
                        headlineContent = { Text(sub.name) },
                        supportingContent = sub.description?.let { { Text(it) } },
                        trailingContent = {
                            androidx.compose.foundation.layout.Row {
                                IconButton(onClick = {
                                    navController.navigate(
                                        Screen.SubcategoryForm.createRoute(
                                            subcategoryId = sub.id,
                                            categoryId = categoryId
                                        )
                                    )
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { deleteTarget = sub }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    deleteTarget?.let { sub ->
        ConfirmDialog(
            title = "Delete Subcategory",
            message = "Are you sure you want to delete \"${sub.name}\"?",
            onConfirm = {
                viewModel.deleteSubcategory(sub.id)
                deleteTarget = null
                scope.launch { snackbarHostState.showSnackbar("Subcategory deleted") }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
