package com.inventory.app.ui.screens.categories

import com.inventory.app.ui.components.ThemedTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import com.inventory.app.ui.components.ThemedAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.PageHeader
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.ui.components.SaveAction
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.RegisterNavigationGuard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryFormScreen(
    navController: NavController,
    subcategoryId: Long? = null,
    categoryId: Long,
    viewModel: SubcategoryFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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

    LaunchedEffect(subcategoryId, categoryId) {
        viewModel.init(subcategoryId, categoryId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) navController.popBackStack()
    }

    val formTitle = if (subcategoryId != null) "Edit Subcategory" else "Add Subcategory"

    PageScaffold(
        onBack = {
            if (isDirty) showDiscardDialog = true
            else navController.popBackStack()
        },
        actions = {
            SaveAction(
                visible = true,
                onClick = { viewModel.save() }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
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

        }
    }
}
