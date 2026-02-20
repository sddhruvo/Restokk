package com.inventory.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.inventory.app.ui.screens.barcode.BarcodeScannerScreen
import com.inventory.app.ui.screens.categories.CategoryFormScreen
import com.inventory.app.ui.screens.categories.CategoryListScreen
import com.inventory.app.ui.screens.categories.SubcategoryFormScreen
import com.inventory.app.ui.screens.categories.SubcategoryListScreen
import com.inventory.app.ui.screens.dashboard.DashboardScreen
import com.inventory.app.ui.screens.items.ItemDetailScreen
import com.inventory.app.ui.screens.items.ItemFormScreen
import com.inventory.app.ui.screens.items.ItemListScreen
import com.inventory.app.ui.screens.locations.LocationDetailScreen
import com.inventory.app.ui.screens.locations.LocationFormScreen
import com.inventory.app.ui.screens.locations.LocationListScreen
import com.inventory.app.ui.screens.more.MoreScreen
import com.inventory.app.ui.screens.reports.ExpiringReportScreen
import com.inventory.app.ui.screens.reports.InventoryReportScreen
import com.inventory.app.ui.screens.reports.LowStockReportScreen
import com.inventory.app.ui.screens.reports.ReportsScreen
import com.inventory.app.ui.screens.reports.SpendingReportScreen
import com.inventory.app.ui.screens.reports.UsageReportScreen
import com.inventory.app.ui.screens.pantryhealth.PantryHealthScreen
import com.inventory.app.ui.screens.kitchen.KitchenMapScreen
import com.inventory.app.ui.screens.recognition.FridgeScanScreen
import com.inventory.app.ui.screens.recognition.ReceiptScanScreen
import com.inventory.app.ui.screens.purchases.PurchaseHistoryScreen
import com.inventory.app.ui.screens.search.GlobalSearchScreen
import com.inventory.app.ui.screens.settings.ExportImportScreen
import com.inventory.app.ui.screens.settings.SettingsScreen
import com.inventory.app.ui.screens.cook.CookScreen
import com.inventory.app.ui.screens.cook.SavedRecipesScreen
import com.inventory.app.ui.screens.onboarding.OnboardingScreen
import com.inventory.app.ui.screens.shopping.ShoppingListScreen

// Bottom nav tab transitions (subtle fade)
private fun tabEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(300))

private fun tabExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(300))

// Detail/form screen transitions (slide + fade)
private fun detailEnterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(300)) +
            fadeIn(animationSpec = tween(300))

private fun detailExitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) +
            fadeOut(animationSpec = tween(300))

private fun detailPopEnterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) +
            fadeIn(animationSpec = tween(300))

private fun detailPopExitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(300)) +
            fadeOut(animationSpec = tween(300))

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Dashboard.route,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding
        composable(
            Screen.Onboarding.route,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            OnboardingScreen(
                onComplete = { postRoute ->
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                    postRoute?.let { route ->
                        navController.navigate(route)
                    }
                }
            )
        }

        // Bottom nav destinations
        composable(
            Screen.Dashboard.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
            DashboardScreen(navController = navController, windowWidthSizeClass = windowWidthSizeClass)
        }

        composable(
            Screen.ItemList.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull()
            ItemListScreen(navController = navController, initialCategoryId = categoryId)
        }

        composable(
            Screen.BarcodeScan.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            BarcodeScannerScreen(navController = navController)
        }

        composable(
            Screen.ShoppingList.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
            ShoppingListScreen(navController = navController)
        }

        composable(
            Screen.More.route,
            enterTransition = { tabEnterTransition() },
            exitTransition = { tabExitTransition() },
            popEnterTransition = { tabEnterTransition() },
            popExitTransition = { tabExitTransition() }
        ) {
            MoreScreen(navController = navController)
        }

        // Item screens
        composable(
            route = "items/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            ItemDetailScreen(navController = navController)
        }

        composable(
            route = "items/form?itemId={itemId}&barcode={barcode}&name={name}&brand={brand}&quantity={quantity}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("barcode") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("brand") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("quantity") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull()
            val barcode = backStackEntry.arguments?.getString("barcode")
            val name = backStackEntry.arguments?.getString("name")
            val brand = backStackEntry.arguments?.getString("brand")
            val quantity = backStackEntry.arguments?.getString("quantity")
            ItemFormScreen(
                navController = navController,
                itemId = itemId,
                barcode = barcode,
                name = name,
                brand = brand,
                quantity = quantity
            )
        }

        // Category screens
        composable(
            Screen.CategoryList.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            CategoryListScreen(navController = navController)
        }

        composable(
            route = "categories/form?categoryId={categoryId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull()
            CategoryFormScreen(navController = navController, categoryId = categoryId)
        }

        composable(
            route = "categories/{categoryId}/subcategories",
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType }),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            SubcategoryListScreen(navController = navController, categoryId = categoryId)
        }

        composable(
            route = "subcategories/form?subcategoryId={subcategoryId}&categoryId={categoryId}",
            arguments = listOf(
                navArgument("subcategoryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) { backStackEntry ->
            val subcategoryId = backStackEntry.arguments?.getString("subcategoryId")?.toLongOrNull()
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull() ?: 0L
            SubcategoryFormScreen(
                navController = navController,
                subcategoryId = subcategoryId,
                categoryId = categoryId
            )
        }

        // Location screens
        composable(
            Screen.LocationList.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            LocationListScreen(navController = navController)
        }

        composable(
            route = "locations/form?locationId={locationId}",
            arguments = listOf(
                navArgument("locationId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId")?.toLongOrNull()
            LocationFormScreen(navController = navController, locationId = locationId)
        }

        composable(
            route = "locations/{locationId}",
            arguments = listOf(navArgument("locationId") { type = NavType.LongType }),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            LocationDetailScreen(navController = navController)
        }

        // Report screens
        composable(
            Screen.Reports.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            ReportsScreen(navController = navController)
        }
        composable(
            Screen.ExpiringReport.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            ExpiringReportScreen(navController = navController)
        }
        composable(
            Screen.LowStockReport.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            LowStockReportScreen(navController = navController)
        }
        composable(
            Screen.SpendingReport.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            SpendingReportScreen(navController = navController)
        }
        composable(
            Screen.UsageReport.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            UsageReportScreen(navController = navController)
        }
        composable(
            Screen.InventoryReport.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            InventoryReportScreen(navController = navController)
        }

        // Purchase History
        composable(
            Screen.PurchaseHistory.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            PurchaseHistoryScreen(navController = navController)
        }

        // Pantry Health
        composable(
            Screen.PantryHealth.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            PantryHealthScreen(navController = navController)
        }

        // Receipt Scanning
        composable(
            Screen.ReceiptScan.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            ReceiptScanScreen(navController = navController)
        }

        // Kitchen Map (visual zone view)
        composable(
            Screen.KitchenMap.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            KitchenMapScreen(navController = navController)
        }

        // What Can I Cook? (AI Meal Suggestions)
        composable(
            Screen.Cook.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            CookScreen(navController = navController)
        }

        // My Saved Recipes
        composable(
            Screen.SavedRecipes.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            SavedRecipesScreen(navController = navController)
        }

        // Kitchen Scan (Fridge/Pantry Photo Recognition)
        composable(
            Screen.FridgeScan.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            FridgeScanScreen(navController = navController)
        }

        // Search
        composable(
            Screen.GlobalSearch.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            GlobalSearchScreen(navController = navController)
        }

        // Settings screens
        composable(
            Screen.Settings.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            SettingsScreen(navController = navController)
        }
        composable(
            Screen.ExportImport.route,
            enterTransition = { detailEnterTransition() },
            exitTransition = { detailExitTransition() },
            popEnterTransition = { detailPopEnterTransition() },
            popExitTransition = { detailPopExitTransition() }
        ) {
            ExportImportScreen(navController = navController)
        }
    }
}
