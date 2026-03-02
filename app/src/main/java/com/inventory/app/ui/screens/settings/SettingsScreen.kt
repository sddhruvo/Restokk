package com.inventory.app.ui.screens.settings

import com.inventory.app.ui.components.ThemedRadioButton
import com.inventory.app.ui.components.ThemedSnackbarHost
import com.inventory.app.ui.components.ThemedTextField
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Notifications
import com.inventory.app.ui.components.ThemedAlertDialog
import com.inventory.app.ui.components.ThemedButton
import androidx.compose.material3.Button
import com.inventory.app.ui.components.ThemedCircularProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.inventory.app.ui.components.ThemedSwitch
import com.inventory.app.ui.components.ThemedScaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.inventory.app.ui.components.ThemedTopAppBar
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
import com.inventory.app.ui.components.AnimatedSaveButton
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkBackButton
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.BuildConfig
import com.inventory.app.R
import com.inventory.app.ui.navigation.RegisterNavigationGuard
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.screens.onboarding.UserPreference
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.VisualStyle
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.previewColor
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
            snackbarHostState.showSnackbar("Sign-in failed: $error")
            viewModel.clearAuthError()
        }
    }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    InkBackButton(onClick = {
                        if (isDirty) showDiscardDialog = true
                        else navController.popBackStack()
                    })
                }
            )
        },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) }
    ) { padding ->
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)
        ) {
            // Account
            Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
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

            // Notifications
            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

            // Expiry Settings
            Text("Expiry Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    ThemedTextField(
                        value = uiState.expiryWarningDays,
                        onValueChange = { viewModel.updateExpiryWarningDays(it) },
                        label = { Text("Warning Days Before Expiry") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.expiryWarningDaysError != null,
                        supportingText = { Text(uiState.expiryWarningDaysError ?: "Days before expiry to show warnings") }
                    )
                }
            }

            // Display Settings
            Text("Display", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    ThemedTextField(
                        value = uiState.currencySymbol,
                        onValueChange = { viewModel.updateCurrencySymbol(it) },
                        label = { Text("Currency Symbol") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.currencyError != null,
                        supportingText = uiState.currencyError?.let { { Text(it) } }
                    )
                    ThemedTextField(
                        value = uiState.defaultQuantity,
                        onValueChange = { viewModel.updateDefaultQuantity(it) },
                        label = { Text("Default Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = uiState.defaultQuantityError != null,
                        supportingText = uiState.defaultQuantityError?.let { { Text(it) } }
                    )
                    Text("Color Palette", style = MaterialTheme.typography.bodyLarge)
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
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    Text("Visual Style", style = MaterialTheme.typography.bodyLarge)
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
            Text("What Matters Most", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
                    Text(
                        "This shapes your dashboard and suggestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    val isInk = MaterialTheme.visuals.isInk
                    UserPreference.entries.forEach { pref ->
                        val isSelected = uiState.userPreference == pref.name
                        // In P&I mode: selected row gets ink wash highlight
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

            // Shopping List
            Text("Shopping List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    ThemedTextField(
                        value = uiState.shoppingBudget,
                        onValueChange = { viewModel.updateShoppingBudget(it) },
                        label = { Text("Shopping Budget") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Leave empty for no budget limit") }
                    )
                    ThemedTextField(
                        value = uiState.autoClearDays,
                        onValueChange = { viewModel.updateAutoClearDays(it) },
                        label = { Text("Auto-Clear Purchased After (Days)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Leave empty to keep purchased items forever") }
                    )
                }
            }

            // General
            Text("General", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    Text(
                        text = "Re-watch the intro walkthrough to explore features you may have missed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ThemedButton(
                        onClick = {
                            viewModel.resetOnboarding {
                                navController.navigate(Screen.Onboarding.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replay Onboarding")
                    }
                }
            }

            // Save button
            AnimatedSaveButton(
                text = "Save Settings",
                onClick = { viewModel.save() },
                isSaved = uiState.isSaved
            )

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
