package com.inventory.app.ui.screens.settings

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import com.inventory.app.ui.components.ThemedRadioButton
import com.inventory.app.ui.components.ThemedSnackbarHost
import com.inventory.app.ui.components.ThemedTextField
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.BackHandler
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Notifications
import com.inventory.app.ui.components.ThemedAlertDialog
import com.inventory.app.ui.components.ThemedButton
import androidx.compose.material3.Button
import com.inventory.app.ui.components.ThemedCircularProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.inventory.app.ui.components.ThemedSwitch
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.PageHeader
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.inventory.app.data.sync.model.BackupEligibility
import com.inventory.app.data.sync.model.BackupStatus
import com.inventory.app.data.sync.model.RestoreResult
import com.inventory.app.data.sync.model.SyncConstants
import com.inventory.app.ui.components.SaveAction
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.util.FormatUtils
import com.inventory.app.BuildConfig
import com.inventory.app.R
import com.inventory.app.domain.model.MeasurementSystem
import com.inventory.app.domain.model.RegionRegistry
import com.inventory.app.ui.components.RegionPickerContent
import com.inventory.app.ui.navigation.RegisterNavigationGuard
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.screens.onboarding.popularRegions
import com.inventory.app.ui.screens.onboarding.UserPreference
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.VisualStyle
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.previewColor
import com.inventory.app.ui.theme.sectionHeader
import com.inventory.app.ui.theme.visuals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = uiState.hasBeenTouched && !uiState.isSaved

    // Guard bottom nav taps when settings have unsaved changes
    RegisterNavigationGuard(
        shouldBlock = { isDirty },
        message = { "You have unsaved settings changes. Discard and leave?" }
    )

    BackHandler(enabled = isDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        ThemedAlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved settings changes. Are you sure you want to go back?") },
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

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                viewModel.signInWithGoogle(token)
            } else {
                viewModel.signInFailed("Sign-in failed: no authentication token received.")
            }
        } catch (e: ApiException) {
            Log.e("SettingsScreen", "Google sign-in failed: status=${e.statusCode}", e)
            if (e.statusCode != 12501) {
                viewModel.signInFailed("Google sign-in failed (code ${e.statusCode}). Please try again.")
            }
        }
    }

    // Notification permission launcher — re-prompt when user enables notifications
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Settings saved")
        }
    }

    LaunchedEffect(uiState.authError) {
        uiState.authError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearAuthError()
        }
    }

    // Restore success dialog
    uiState.restoreResult?.let { result ->
        ThemedAlertDialog(
            onDismissRequest = { viewModel.clearRestoreResult() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Restore Complete") },
            text = {
                Column {
                    if (result.itemsRestored > 0) {
                        Text("• ${result.itemsRestored} items")
                    }
                    if (result.shoppingRestored > 0) {
                        Text("• ${result.shoppingRestored} shopping list items")
                    }
                    if (result.recipesRestored > 0) {
                        Text("• ${result.recipesRestored} recipes")
                    }
                    if (result.itemsRestored == 0 && result.shoppingRestored == 0 && result.recipesRestored == 0) {
                        Text("Your data is already up to date.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearRestoreResult() }) {
                    Text("OK")
                }
            }
        )
    }

    // Restore dialog — shown after Google sign-in if cloud backup exists
    if (uiState.showRestoreDialog && uiState.restorePromptData != null) {
        val metadata = uiState.restorePromptData!!
        val backupDate = FormatUtils.formatRelativeTime(metadata.lastBackupAt)
        ThemedAlertDialog(
            onDismissRequest = { if (!uiState.restoreInProgress) viewModel.dismissRestoreDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Restore Backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    Text("We found a backup from $backupDate with ${metadata.itemCount} items and ${metadata.recipeCount} recipes.")
                    if (uiState.restoreHasLocalData) {
                        Text(
                            "This will merge with your existing data. Duplicates keep the newer version.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.restoreInProgress) {
                        ThemedCircularProgress(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally),
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            confirmButton = {
                ThemedButton(
                    onClick = { viewModel.performRestore() },
                    enabled = !uiState.restoreInProgress
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissRestoreDialog() },
                    enabled = !uiState.restoreInProgress
                ) {
                    Text("Skip")
                }
            }
        )
    }

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
        // Delete account confirmation dialog
        if (showDeleteConfirm) {
            ThemedAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                icon = {
                    ThemedIcon(
                        materialIcon = Icons.Filled.DeleteForever,
                        inkIconRes = R.drawable.ic_ink_delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("Delete Account?") },
                text = {
                    Text("This will permanently delete your account and erase all app data (inventory, shopping lists, recipes, settings). This cannot be undone.")
                },
                confirmButton = {
                    ThemedButton(
                        onClick = {
                            showDeleteConfirm = false
                            viewModel.deleteAccount {
                                navController.navigate(Screen.Onboarding.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)
        ) {
            PageHeader("Settings")

            // Account
            Text("Account", style = MaterialTheme.typography.sectionHeader)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    if (uiState.isSignedIn && !uiState.isAnonymous) {
                        // Signed in with Google
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.userPhotoUrl != null) {
                                AsyncImage(
                                    model = uiState.userPhotoUrl,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.AccountCircle,
                                    inkIconRes = R.drawable.ic_ink_account,
                                    contentDescription = "Account",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.spacingMd))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.userName ?: "Signed in",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                uiState.userEmail?.let { email ->
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.signOut() }) {
                                ThemedIcon(
                                    materialIcon = Icons.AutoMirrored.Filled.Logout,
                                    inkIconRes = R.drawable.ic_ink_logout,
                                    contentDescription = "Sign out",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.DeleteForever,
                                inkIconRes = R.drawable.ic_ink_delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(Dimens.spacingXs))
                            Text("Delete Account", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        // Not signed in or anonymous
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.AccountCircle,
                                inkIconRes = R.drawable.ic_ink_account,
                                contentDescription = "Account",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(Dimens.spacingMd))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Not signed in",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Sign in to sync across devices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (uiState.authLoading) {
                            ThemedCircularProgress(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterHorizontally),
                                strokeWidth = 2.dp
                            )
                        } else {
                            ThemedButton(
                                onClick = {
                                    try {
                                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(context.getString(R.string.default_web_client_id))
                                            .requestEmail()
                                            .build()
                                        val client = GoogleSignIn.getClient(context, gso)
                                        googleSignInLauncher.launch(client.signInIntent)
                                    } catch (e: Exception) {
                                        Log.e("SettingsScreen", "Failed to launch Google sign-in", e)
                                        viewModel.signInFailed("Could not start Google sign-in: ${e.message}")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign in with Google")
                            }
                        }
                    }
                }
            }

            // Backup & Sync
            Text("Backup & Sync", style = MaterialTheme.typography.sectionHeader)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    if (!uiState.isSignedIn || uiState.isAnonymous) {
                        // Not signed in — prompt
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimens.spacingMd))
                            Text(
                                "Sign in with Google to enable cloud backup",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Signed in — show backup status
                        if (uiState.lastBackupDate != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                                Text(
                                    "Last backed up: ${uiState.lastBackupDate}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Text(
                                "No backup yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Item count progress
                        val backedUp = uiState.lastBackupItemCount
                        val total = uiState.totalItemCount
                        Text(
                            "$backedUp of ${SyncConstants.FREE_TIER_ITEM_LIMIT} items backed up" +
                                if (total > SyncConstants.FREE_TIER_ITEM_LIMIT) " ($total total)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { backedUp.toFloat() / SyncConstants.FREE_TIER_ITEM_LIMIT.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Backup button / status
                        when (val status = uiState.backupStatus) {
                            is BackupStatus.InProgress -> {
                                ThemedCircularProgress(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterHorizontally),
                                    strokeWidth = 2.dp
                                )
                            }
                            is BackupStatus.Success -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Dimens.spacingXs))
                                    Text(
                                        "Backup complete",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            is BackupStatus.Failed -> {
                                Text(
                                    status.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            is BackupStatus.Idle -> {
                                val eligibility = uiState.backupEligibility
                                val isRateLimited = eligibility is BackupEligibility.RateLimited
                                ThemedButton(
                                    onClick = { viewModel.performBackup() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = eligibility is BackupEligibility.Eligible
                                ) {
                                    Text(
                                        if (isRateLimited) "Available in ${(eligibility as BackupEligibility.RateLimited).minutesRemaining} min"
                                        else "Back Up Now"
                                    )
                                }
                            }
                        }
                        Text(
                            "Auto backup runs daily when connected to the internet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Dimens.spacingXs)
                        )
                    }
                }
            }

            // Notifications
            Text("Notifications", style = MaterialTheme.typography.sectionHeader)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = uiState.notificationsEnabled,
                                onValueChange = { enabled ->
                                    viewModel.toggleNotificationsEnabled(enabled)
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                },
                                role = Role.Switch
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Notifications", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Smart alerts for expiry, restock & shopping",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ThemedSwitch(
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = null
                        )
                    }
                    if (uiState.notificationsEnabled) {
                        Spacer(modifier = Modifier.height(Dimens.spacingSm))
                        NotificationToggleRow(
                            label = "Expiry Alerts",
                            description = "When items are about to expire",
                            checked = uiState.notifExpiryEnabled,
                            onCheckedChange = { viewModel.toggleNotifExpiry(it) }
                        )
                        NotificationToggleRow(
                            label = "Smart Restock",
                            description = "Suggestions based on buying patterns",
                            checked = uiState.notifRestockEnabled,
                            onCheckedChange = { viewModel.toggleNotifRestock(it) }
                        )
                        NotificationToggleRow(
                            label = "Shopping Reminders",
                            description = "Reminders about your shopping list",
                            checked = uiState.notifShoppingEnabled,
                            onCheckedChange = { viewModel.toggleNotifShopping(it) }
                        )
                    }
                }
            }

            // Display
            Text("Display", style = MaterialTheme.typography.sectionHeader)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingMd)) {
                    NotificationToggleRow(
                        label = "Dashboard Highlight",
                        description = "Pulse-glow the most urgent stat card",
                        checked = uiState.dashboardHighlightEnabled,
                        onCheckedChange = { viewModel.toggleDashboardHighlight(it) }
                    )
                }
            }

            // Preferences — compact inline rows
            Text("Preferences", style = MaterialTheme.typography.sectionHeader)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingMd)) {
                    // Region row
                    RegionSettingRow(
                        regionFlag = uiState.regionFlag,
                        regionName = uiState.regionName,
                        showPicker = uiState.showRegionPicker,
                        onTogglePicker = { viewModel.toggleRegionPicker() },
                        selectedRegionCode = uiState.regionCode,
                        onRegionSelect = { viewModel.updateRegion(it) }
                    )
                    // Measurement system row
                    MeasurementSettingRow(
                        currentValue = uiState.measurementSystem,
                        regionCode = uiState.regionCode,
                        onValueChange = { viewModel.updateMeasurementSystem(it) }
                    )
                    // Date format row
                    DateFormatSettingRow(
                        currentValue = uiState.dateFormat,
                        regionCode = uiState.regionCode,
                        onValueChange = { viewModel.updateDateFormat(it) }
                    )
                    CompactSettingRow(
                        label = "Default Quantity",
                        value = uiState.defaultQuantity,
                        onValueChange = { viewModel.updateDefaultQuantity(it) },
                        keyboardType = KeyboardType.Decimal,
                        isError = uiState.defaultQuantityError != null,
                        errorText = uiState.defaultQuantityError
                    )
                    StepperSettingRow(
                        label = "Expiry Warning",
                        value = uiState.expiryWarningDays.toIntOrNull() ?: 3,
                        suffix = "days",
                        step = 1,
                        range = 1..30,
                        onValueChange = { viewModel.updateExpiryWarningDays(it.toString()) }
                    )
                    CompactSettingRow(
                        label = "Shopping Budget",
                        value = uiState.shoppingBudget,
                        onValueChange = { viewModel.updateShoppingBudget(it) },
                        keyboardType = KeyboardType.Decimal,
                        placeholder = "No limit"
                    )
                    CompactSettingRow(
                        label = "Auto-Clear Days",
                        value = uiState.autoClearDays,
                        onValueChange = { viewModel.updateAutoClearDays(it) },
                        keyboardType = KeyboardType.Number,
                        placeholder = "Never"
                    )
                    StepperSettingRow(
                        label = "Low Stock Alert",
                        value = uiState.lowStockThreshold.toIntOrNull() ?: 25,
                        suffix = "%",
                        step = 5,
                        range = 5..100,
                        onValueChange = { viewModel.updateLowStockThreshold(it.toString()) },
                        showDivider = false
                    )
                }
            }

            // Theme
            Text("Theme", style = MaterialTheme.typography.sectionHeader)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingMd), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTheme.entries.forEach { theme ->
                            ThemeCircle(
                                theme = theme,
                                isSelected = uiState.appTheme == theme,
                                onClick = { viewModel.updateAppTheme(theme) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VisualStyle.entries.forEach { style ->
                            VisualStyleChip(
                                style = style,
                                isSelected = uiState.visualStyle == style,
                                onClick = { viewModel.updateVisualStyle(style) }
                            )
                        }
                    }
                }
            }

            // What Matters Most
            Text("What Matters Most", style = MaterialTheme.typography.sectionHeader)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingMd), verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
                    Text(
                        "This shapes your dashboard and suggestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val isInk = MaterialTheme.visuals.isInk
                    UserPreference.entries.forEach { pref ->
                        val isSelected = uiState.userPreference == pref.name
                        val rowBackground = if (isInk && isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = InkTokens.fillLight)
                        else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Dimens.spacingSm))
                                .background(rowBackground)
                                .clickable { viewModel.updateUserPreference(pref.name) }
                                .padding(vertical = Dimens.spacingSm, horizontal = Dimens.spacingXs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThemedRadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateUserPreference(pref.name) }
                            )
                            Spacer(modifier = Modifier.width(Dimens.spacingSm))
                            Column {
                                Text(
                                    pref.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    pref.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // General
            AppCard(
                onClick = {
                    viewModel.resetOnboarding {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.spacingMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Replay Onboarding", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Re-watch the intro walkthrough",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ThemedIcon(
                        materialIcon = Icons.Filled.ArrowForward,
                        inkIconRes = R.drawable.ic_ink_forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // About
            Spacer(modifier = Modifier.height(Dimens.spacingLg))
            Text(
                text = "Restokk v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Developed by Shantanu Dey",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sddhruvo.github.io/Restokk/privacy-policy.html"))
                        context.startActivity(intent)
                    }
            )
            Text(
                text = "Send Feedback",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        com.inventory.app.ui.screens.more.launchFeedbackEmail(context)
                    }
            )
        }
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(start = Dimens.spacingSm, top = Dimens.spacingXs, bottom = Dimens.spacingXs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ThemedSwitch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ThemeCircle(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val circleColor = theme.previewColor
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(borderWidth, borderColor, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                val checkTint = if (circleColor.luminance() > 0.5f) Color.Black else Color.White
                ThemedIcon(
                    materialIcon = Icons.Filled.Check,
                    inkIconRes = R.drawable.ic_ink_check,
                    contentDescription = "Selected",
                    tint = checkTint,
                    modifier = Modifier.size(Dimens.iconSizeMd)
                )
            }
        }
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VisualStyleChip(
    style: VisualStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .border(borderWidth, borderColor, MaterialTheme.shapes.small)
                .clip(MaterialTheme.shapes.small)
                .background(bgColor)
                .clickable { onClick() }
                .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingSm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompactSettingRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null,
    placeholder: String = "",
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingSm, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                modifier = Modifier.width(100.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                textAlign = TextAlign.End
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (isError && errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Dimens.spacingSm)
            )
        }
        if (showDivider) {
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = Dimens.spacingSm)
            )
        }
    }
}

@Composable
private fun StepperSettingRow(
    label: String,
    value: Int,
    suffix: String,
    step: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingSm, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (value > range.first) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable(enabled = value > range.first) {
                            onValueChange((value - step).coerceAtLeast(range.first))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "\u2212",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (value > range.first) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                Text(
                    text = "$value$suffix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(52.dp),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (value < range.last) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable(enabled = value < range.last) {
                            onValueChange((value + step).coerceAtMost(range.last))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (value < range.last) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
        if (showDivider) {
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = Dimens.spacingSm)
            )
        }
    }
}

@Composable
private fun RegionSettingRow(
    regionFlag: String,
    regionName: String,
    showPicker: Boolean,
    onTogglePicker: () -> Unit,
    selectedRegionCode: String,
    onRegionSelect: (com.inventory.app.ui.screens.onboarding.RegionInfo) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.spacingSm))
                .clickable { onTogglePicker() }
                .padding(horizontal = Dimens.spacingSm, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Region",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$regionFlag $regionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ThemedIcon(
                    materialIcon = Icons.Filled.ArrowForward,
                    inkIconRes = R.drawable.ic_ink_forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = showPicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            RegionPickerContent(
                regions = popularRegions,
                selectedRegionCode = selectedRegionCode,
                onSelect = onRegionSelect,
                modifier = Modifier.padding(horizontal = Dimens.spacingSm, vertical = Dimens.spacingSm)
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = Dimens.spacingSm)
        )
    }
}

@Composable
private fun MeasurementSettingRow(
    currentValue: String,
    regionCode: String,
    onValueChange: (String) -> Unit
) {
    val resolvedAuto = RegionRegistry.findByCode(regionCode)?.measurementSystem
        ?: MeasurementSystem.METRIC
    val autoLabel = "Auto (${resolvedAuto.name.lowercase().replaceFirstChar { it.uppercase() }})"

    val options = listOf("" to autoLabel, "METRIC" to "Metric", "IMPERIAL" to "Imperial")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingSm, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Units",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                options.forEachIndexed { index, (value, label) ->
                    val isSelected = currentValue == value
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        options.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(bgColor)
                            .clickable { onValueChange(value) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                }
            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = Dimens.spacingSm)
        )
    }
}

@Composable
private fun DateFormatSettingRow(
    currentValue: String,
    regionCode: String,
    onValueChange: (String) -> Unit
) {
    val regionConfig = RegionRegistry.findByCode(regionCode)
    val autoFormat = if (regionConfig?.isMonthFirst == true) "Mar 12" else "12 Mar"
    val autoLabel = "Auto ($autoFormat)"

    val options = listOf("" to autoLabel, "MONTH_FIRST" to "Mar 12", "DAY_FIRST" to "12 Mar")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingSm, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                options.forEachIndexed { index, (value, label) ->
                    val isSelected = currentValue == value
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        options.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(bgColor)
                            .clickable { onValueChange(value) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                }
            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = Dimens.spacingSm)
        )
    }
}
