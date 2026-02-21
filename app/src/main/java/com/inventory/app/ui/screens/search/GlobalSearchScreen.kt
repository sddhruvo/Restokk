package com.inventory.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.EmptyState
import com.inventory.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController: NavController,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
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
        ) {
            SearchBar(
                query = uiState.query,
                onQueryChange = { viewModel.updateQuery(it) },
                onSearch = { query ->
                    viewModel.updateQuery(query)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                active = false,
                onActiveChange = { },
                placeholder = { Text("Search items, categories, locations...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
            ) {}

            val totalResults = uiState.items.size + uiState.categories.size + uiState.locations.size

            if (uiState.hasSearched && totalResults == 0) {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = "No Results",
                    message = "No matches found for \"${uiState.query}\""
                )
            } else {
                LazyColumn {
                    // Items section
                    if (uiState.items.isNotEmpty()) {
                        item {
                            Text(
                                "Items (${uiState.items.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(uiState.items, key = { "item-${it.id}" }) { item ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = {
                                    val parts = mutableListOf<String>()
                                    item.brand?.let { parts.add(it) }
                                    parts.add("Qty: ${item.quantity}")
                                    Text(parts.joinToString(" | "))
                                },
                                leadingContent = {
                                    Icon(Icons.Filled.Inventory2, contentDescription = "Item",
                                        tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.ItemDetail.createRoute(item.id))
                                }
                            )
                        }
                    }

                    // Categories section
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            Text(
                                "Categories (${uiState.categories.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(uiState.categories, key = { "cat-${it.id}" }) { category ->
                            ListItem(
                                headlineContent = { Text(category.name) },
                                supportingContent = { category.description?.let { Text(it) } },
                                leadingContent = {
                                    Icon(Icons.Filled.Category, contentDescription = "Category",
                                        tint = MaterialTheme.colorScheme.secondary)
                                },
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.SubcategoryList.createRoute(category.id))
                                }
                            )
                        }
                    }

                    // Locations section
                    if (uiState.locations.isNotEmpty()) {
                        item {
                            Text(
                                "Locations (${uiState.locations.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(uiState.locations, key = { "loc-${it.id}" }) { location ->
                            ListItem(
                                headlineContent = { Text(location.name) },
                                supportingContent = { location.description?.let { Text(it) } },
                                leadingContent = {
                                    Icon(Icons.Filled.Place, contentDescription = "Location",
                                        tint = MaterialTheme.colorScheme.tertiary)
                                },
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.LocationDetail.createRoute(location.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
