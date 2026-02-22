package com.inventory.app.ui.screens.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.DashboardGreeting
import com.inventory.app.ui.components.EmptyState
import com.inventory.app.ui.components.ShimmerStatCard
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.CardBlue
import com.inventory.app.ui.theme.CardGold
import com.inventory.app.ui.theme.CardGreen
import com.inventory.app.ui.theme.CardOrange
import com.inventory.app.ui.theme.CardPurple
import com.inventory.app.ui.theme.ExpiryOrange
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.theme.ExpiryRed
import com.inventory.app.ui.theme.StockGreen
import com.inventory.app.ui.theme.StockYellow
import com.inventory.app.ui.theme.scoreToColor
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.inventory.app.R
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val DashboardCardShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val uiState by viewModel.uiState.collectAsState()
    val showShoppingSheet = com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Google Sign-In launcher for beta dialog
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.onBetaGoogleSignIn(token)
                }
            } catch (e: ApiException) {
                viewModel.onBetaSignInError(e.message ?: "Google sign-in failed")
            }
        }
    }

    val gridColumns = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Expanded -> 4
        WindowWidthSizeClass.Medium -> 3
        else -> 2
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar =  {
            TopAppBar(
                title = { Text("Restokk") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.GlobalSearch.route) }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Beta sign-in dialog — appears every launch until user signs in
        if (uiState.showBetaWelcome) {
            AlertDialog(
                onDismissRequest = {
                    if (!uiState.betaSignInLoading) viewModel.dismissBetaWelcomeForSession()
                },
                icon = {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = CardGold,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        "Welcome to the Restokk Beta!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Thank you for being an early tester! Please sign in with Google so we can identify beta testers and reach you if needed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Your data stays on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.betaSignInLoading) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Signing in...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        uiState.betaSignInError?.let { error ->
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(context.getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .build()
                                val client = GoogleSignIn.getClient(context, gso)
                                googleSignInLauncher.launch(client.signInIntent)
                            } catch (_: Exception) { }
                        },
                        enabled = !uiState.betaSignInLoading
                    ) {
                        Text("Sign in with Google")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissBetaWelcomeForSession() },
                        enabled = !uiState.betaSignInLoading
                    ) {
                        Text("Maybe Later")
                    }
                }
            )
        }

        // Only animate on first entry; skip on back-navigation to preserve scroll
        var hasAnimated by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!hasAnimated) {
                kotlinx.coroutines.delay(6 * 50L + 300L)
                hasAnimated = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Greeting
            AnimateOnce(index = 0, hasAnimated = hasAnimated) {
                DashboardGreeting(
                    totalItems = uiState.totalItems,
                    expiringSoon = uiState.expiringSoon
                )
            }

            // Hero card — contextual insight based on state
            if (!uiState.isLoading) {
                if (uiState.totalItems == 0) {
                    KitchenScanHeroCard { navController.navigate(Screen.KitchenMap.route) }
                } else if (uiState.expiringSoon > 0) {
                    InsightCard(
                        icon = Icons.Filled.Timer,
                        iconTint = CardOrange,
                        text = "${uiState.expiringSoon} item${if (uiState.expiringSoon != 1) "s" else ""} expiring soon — check them",
                        actionLabel = "View",
                        onClick = { navController.navigate(Screen.ExpiringReport.route) }
                    )
                } else if (uiState.shoppingActive > 0) {
                    InsightCard(
                        icon = Icons.Filled.ShoppingCart,
                        iconTint = CardPurple,
                        text = "Shopping list: ${uiState.shoppingActive} item${if (uiState.shoppingActive != 1) "s" else ""} to buy",
                        actionLabel = "Shop",
                        onClick = { navController.navigate(Screen.ShoppingList.route) }
                    )
                } else if (uiState.savedRecipeCount > 0) {
                    InsightCard(
                        icon = Icons.Filled.MenuBook,
                        iconTint = CardGreen,
                        text = "You have ${uiState.savedRecipeCount} saved recipe${if (uiState.savedRecipeCount != 1) "s" else ""} — time to cook?",
                        actionLabel = "Cook",
                        onClick = { navController.navigate(Screen.Cook.route) }
                    )
                }
            }

            // Stats cards
            AnimateOnce(index = 1, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    if (uiState.isLoading) {
                        AdaptiveGrid(
                            columns = gridColumns,
                            spacing = 12.dp,
                            items = listOf<@Composable (Modifier) -> Unit>(
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) }
                            )
                        )
                    } else {
                        AdaptiveGrid(
                            columns = gridColumns,
                            spacing = 12.dp,
                            items = listOf<@Composable (Modifier) -> Unit>(
                                { mod -> HealthScoreCard(mod, uiState.homeScore, uiState.homeScoreLabel) { navController.navigate(Screen.PantryHealth.route) } },
                                { mod -> StatCard(mod, "Total Items", "${uiState.totalItems}", Icons.Filled.Inventory2, CardBlue) { navController.navigate(Screen.ItemList.createRoute()) } },
                                { mod -> StatCard(mod, "Expiring Soon", "${uiState.expiringSoon}", Icons.Filled.Warning, CardOrange) { navController.navigate(Screen.ExpiringReport.route) } },
                                { mod -> StatCard(mod, "Low Stock", "${uiState.lowStock}", Icons.Filled.TrendingDown, CardGreen) { navController.navigate(Screen.LowStockReport.route) } },
                                { mod -> StatCard(mod, "Total Value", "${uiState.currencySymbol}${String.format("%.2f", uiState.totalValue)}", Icons.Filled.AttachMoney, CardGreen) { navController.navigate(Screen.InventoryReport.route) } }
                            )
                        )
                    }
                }
            }

            // Quick actions
            AnimateOnce(index = 2, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    AdaptiveGrid(
                    columns = gridColumns,
                    spacing = 12.dp,
                    items = listOf<@Composable (Modifier) -> Unit>(
                        { mod -> QuickActionCard(mod, "Cook", Icons.Filled.Restaurant, CardOrange,
                            subtitle = if (uiState.savedRecipeCount > 0) "${uiState.savedRecipeCount} saved" else null
                        ) { navController.navigate(Screen.Cook.route) } },
                        { mod -> QuickActionCard(mod, "Kitchen", Icons.Filled.Kitchen, CardGold,
                            subtitle = if (uiState.lastScanItemCount > 0) "${uiState.lastScanItemCount} mapped" else null
                        ) { navController.navigate(Screen.KitchenMap.route) } },
                        { mod -> QuickActionCard(mod, "Reports", Icons.Filled.Assessment, CardBlue) { navController.navigate(Screen.Reports.route) } },
                        { mod -> QuickActionCard(mod, "Scan", Icons.Filled.QrCodeScanner, CardGold) { navController.navigate(Screen.BarcodeScan.route) } },
                        { mod -> QuickActionCard(mod, "Receipt", Icons.Filled.Receipt, CardGreen) { navController.navigate(Screen.ReceiptScan.route) } },
                        { mod ->
                            val totalShopping = uiState.shoppingActive + uiState.shoppingPurchased
                            QuickActionCard(mod, "Shopping", Icons.Filled.ShoppingCart, CardPurple,
                                subtitle = if (totalShopping > 0) "${uiState.shoppingPurchased}/$totalShopping done" else null
                            ) { navController.navigate(Screen.ShoppingList.route) }
                        }
                    )
                )
                }
            }

            // Expiring soon list
            if (uiState.expiringItems.isNotEmpty()) {
            AnimateOnce(index = 3, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Expiring Soon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.expiringItems.size > 5) {
                                TextButton(onClick = { navController.navigate(Screen.ExpiringReport.route) }) {
                                    Text("View All (${uiState.expiringItems.size})")
                                }
                            }
                            IconButton(onClick = { navController.navigate(Screen.ItemForm.createRoute()) }) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add item",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    run {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            uiState.expiringItems.take(5).forEach { item ->
                                val daysUntil = item.item.expiryDate?.let {
                                    ChronoUnit.DAYS.between(LocalDate.now(), it)
                                }
                                val color = when {
                                    daysUntil == null -> MaterialTheme.colorScheme.onSurface
                                    daysUntil < 0 -> ExpiryRed
                                    daysUntil <= 3 -> ExpiryOrange
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                ListItem(
                                    headlineContent = { Text(item.item.name) },
                                    supportingContent = {
                                        Text(
                                            when {
                                                daysUntil == null -> ""
                                                daysUntil < 0 -> "Expired ${-daysUntil} days ago"
                                                daysUntil == 0L -> "Expires today"
                                                else -> "Expires in $daysUntil days"
                                            },
                                            color = color
                                        )
                                    },
                                    trailingContent = {
                                        Row {
                                            IconButton(
                                                onClick = { showShoppingSheet(item.item.id, null) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.AddShoppingCart,
                                                    contentDescription = "Add ${item.item.name} to shopping list",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.pauseItem(item.item.id)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "${item.item.name} paused",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.unpauseItem(item.item.id)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.PauseCircleOutline,
                                                    contentDescription = "Pause alerts for ${item.item.name}",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }

            // Low stock list
            if (uiState.lowStockItems.isNotEmpty()) {
            AnimateOnce(index = 4, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Low Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (uiState.lowStockItems.size > 5) {
                            TextButton(onClick = { navController.navigate(Screen.LowStockReport.route) }) {
                                Text("View All (${uiState.lowStockItems.size})")
                            }
                        }
                    }

                    run {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            uiState.lowStockItems.take(5).forEach { item ->
                                val effectiveMin = if (item.item.minQuantity > 0) item.item.minQuantity else item.item.smartMinQuantity
                                val ratio = if (effectiveMin > 0) {
                                    (item.item.quantity / effectiveMin).toFloat().coerceIn(0f, 1f)
                                } else 1f
                                val barColor = if (ratio < 0.3f) ExpiryRed else StockYellow
                                ListItem(
                                    headlineContent = { Text(item.item.name) },
                                    supportingContent = {
                                        Column {
                                            Text(
                                                "Qty: ${item.item.quantity.formatQty()} / ${effectiveMin.formatQty()}",
                                                color = barColor
                                            )
                                            LinearProgressIndicator(
                                                progress = { ratio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                                    .height(6.dp),
                                                color = barColor,
                                                trackColor = barColor.copy(alpha = 0.2f)
                                            )
                                        }
                                    },
                                    trailingContent = {
                                        Row {
                                            IconButton(
                                                onClick = { showShoppingSheet(item.item.id, null) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.AddShoppingCart,
                                                    contentDescription = "Add ${item.item.name} to shopping list",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.pauseItem(item.item.id)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "${item.item.name} paused",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.unpauseItem(item.item.id)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.PauseCircleOutline,
                                                    contentDescription = "Pause alerts for ${item.item.name}",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }

            // Items by category summary
            if (uiState.itemsByCategory.isNotEmpty()) {
                AnimateOnce(index = 5, hasAnimated = hasAnimated) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Items by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val maxCategoryCount = uiState.itemsByCategory.maxOfOrNull { it.count } ?: 1
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.itemsByCategory.forEach { data ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate(Screen.ItemList.createRoute(categoryId = data.id))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(data.label, style = MaterialTheme.typography.bodyMedium)
                                            Text("${data.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        val fraction = if (maxCategoryCount > 0) data.count.toFloat() / maxCategoryCount else 0f
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Items by location summary
            if (uiState.itemsByLocation.isNotEmpty()) {
                AnimateOnce(index = 6, hasAnimated = hasAnimated) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Items by Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val maxLocationCount = uiState.itemsByLocation.maxOfOrNull { it.count } ?: 1
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.itemsByLocation.forEach { data ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate(Screen.LocationDetail.createRoute(data.id))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(data.label, style = MaterialTheme.typography.bodyMedium)
                                            Text("${data.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        val fraction = if (maxLocationCount > 0) data.count.toFloat() / maxLocationCount else 0f
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.tertiary,
                                            trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Adaptive Grid ──────────────────────────────────────────────────────

@Composable
private fun AdaptiveGrid(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    items: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowItems.forEach { child ->
                    child(Modifier.weight(1f))
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── Stat Card ──────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    AppCard(onClick = onClick, modifier = modifier, shape = DashboardCardShape) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = iconTint)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Pantry Health Score Card ───────────────────────────────────────────

@Composable
private fun HealthScoreCard(
    modifier: Modifier = Modifier,
    score: Int,
    label: String,
    onClick: () -> Unit = {}
) {
    val scoreColor = scoreToColor(score)
    AppCard(onClick = onClick, modifier = modifier, shape = DashboardCardShape) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Favorite, contentDescription = "Home Score", modifier = Modifier.size(24.dp), tint = scoreColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = scoreColor)
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text("Home $label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tap for details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

// ─── Kitchen Scan Hero Card (empty inventory) ──────────────────────────

@Composable
private fun KitchenScanHeroCard(onClick: () -> Unit) {
    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = DashboardCardShape
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = CardOrange
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    "Map Your Kitchen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Take photos of your fridge, pantry, and shelves. AI identifies every item and builds your inventory in minutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(onClick = onClick) {
                Text("Start Kitchen Tour")
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Insight Card (contextual hero) ──────────────────────────────────────

@Composable
private fun InsightCard(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = DashboardCardShape) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = iconTint)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onClick) {
                Text(actionLabel)
            }
        }
    }
}

// ─── Quick Action Card (with scale-on-tap) ──────────────────────────────

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "quickActionScale"
    )
    AppCard(
        onClick = onClick,
        modifier = modifier
            .height(88.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = DashboardCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(28.dp), tint = iconTint)
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── AnimateOnce (skip animation on back-navigation) ────────────────────

@Composable
private fun AnimateOnce(
    index: Int,
    hasAnimated: Boolean,
    content: @Composable () -> Unit
) {
    if (hasAnimated) {
        content()
    } else {
        StaggeredAnimatedItem(index = index, slideOffsetDivisor = 6) {
            content()
        }
    }
}
