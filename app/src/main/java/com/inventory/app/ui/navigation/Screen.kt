package com.inventory.app.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Bottom nav destinations
    data object Dashboard : Screen("dashboard")
    data object ItemList : Screen("items?categoryId={categoryId}") {
        fun createRoute(categoryId: Long? = null): String =
            if (categoryId != null) "items?categoryId=$categoryId" else "items"
        val baseRoute = "items"
    }
    data object BarcodeScan : Screen("barcode/scan")
    data object ShoppingList : Screen("shopping")
    data object More : Screen("more")

    // Item screens
    data object ItemDetail : Screen("items/{itemId}") {
        fun createRoute(id: Long) = "items/$id"
    }
    data object ItemForm : Screen("items/form?itemId={itemId}&barcode={barcode}&name={name}&brand={brand}&quantity={quantity}") {
        fun createRoute(
            itemId: Long? = null,
            barcode: String? = null,
            name: String? = null,
            brand: String? = null,
            quantity: String? = null
        ): String {
            val params = mutableListOf<String>()
            itemId?.let { params.add("itemId=$it") }
            barcode?.let { params.add("barcode=${Uri.encode(it)}") }
            name?.let { params.add("name=${Uri.encode(it)}") }
            brand?.let { params.add("brand=${Uri.encode(it)}") }
            quantity?.let { params.add("quantity=${Uri.encode(it)}") }
            return if (params.isEmpty()) "items/form" else "items/form?${params.joinToString("&")}"
        }
    }

    // Category screens
    data object CategoryList : Screen("categories")
    data object CategoryForm : Screen("categories/form?categoryId={categoryId}") {
        fun createRoute(categoryId: Long? = null) =
            if (categoryId != null) "categories/form?categoryId=$categoryId" else "categories/form"
    }
    data object SubcategoryList : Screen("categories/{categoryId}/subcategories") {
        fun createRoute(categoryId: Long) = "categories/$categoryId/subcategories"
    }
    data object SubcategoryForm : Screen("subcategories/form?subcategoryId={subcategoryId}&categoryId={categoryId}") {
        fun createRoute(subcategoryId: Long? = null, categoryId: Long) =
            if (subcategoryId != null) "subcategories/form?subcategoryId=$subcategoryId&categoryId=$categoryId"
            else "subcategories/form?categoryId=$categoryId"
    }

    // Location screens
    data object LocationList : Screen("locations")
    data object LocationForm : Screen("locations/form?locationId={locationId}") {
        fun createRoute(locationId: Long? = null) =
            if (locationId != null) "locations/form?locationId=$locationId" else "locations/form"
    }
    data object LocationDetail : Screen("locations/{locationId}") {
        fun createRoute(locationId: Long) = "locations/$locationId"
    }

    // Shopping screens
    data object AddShoppingItem : Screen("shopping/add?itemId={itemId}") {
        fun createRoute(itemId: Long? = null) =
            if (itemId != null) "shopping/add?itemId=$itemId" else "shopping/add"
    }
    // Report screens
    data object Reports : Screen("reports")
    data object ExpiringReport : Screen("reports/expiring")
    data object LowStockReport : Screen("reports/low-stock")
    data object SpendingReport : Screen("reports/spending")
    data object UsageReport : Screen("reports/usage")
    data object InventoryReport : Screen("reports/inventory")

    // Purchase History
    data object PurchaseHistory : Screen("purchases?itemId={itemId}") {
        fun createRoute(itemId: Long? = null): String =
            if (itemId != null) "purchases?itemId=$itemId" else "purchases"
        val baseRoute = "purchases"
    }

    // Search
    data object GlobalSearch : Screen("search")

    // Receipt Scanning
    data object ReceiptScan : Screen("receipt-scan")

    // Kitchen Scan (Fridge/Pantry Photo Recognition)
    data object FridgeScan : Screen("fridge-scan")

    // Kitchen Map (visual zone view)
    data object KitchenMap : Screen("kitchen-map")

    // Pantry Health
    data object PantryHealth : Screen("pantry-health")

    // What Can I Cook? (AI Meal Suggestions)
    data object Cook : Screen("cook")

    // My Saved Recipes
    data object SavedRecipes : Screen("saved-recipes")

    // Onboarding
    data object Onboarding : Screen("onboarding")

    // Settings screens
    data object Settings : Screen("settings")
    data object ExportImport : Screen("settings/export-import")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Home", Icons.Filled.Home),
    BottomNavItem(Screen.ItemList, "Items", Icons.Filled.Inventory2),
    // Center slot is reserved for the Quick Add FAB (no nav item here)
    BottomNavItem(Screen.ShoppingList, "Shopping", Icons.Filled.ShoppingCart),
    BottomNavItem(Screen.More, "More", Icons.Filled.MoreHoriz)
)

data class QuickAddMenuItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val quickAddMenuItems = listOf(
    QuickAddMenuItem("Add Item", Icons.Filled.AddShoppingCart, Screen.AddShoppingItem.createRoute()),
    QuickAddMenuItem("Scan Barcode", Icons.Filled.QrCodeScanner, Screen.BarcodeScan.route),
    QuickAddMenuItem("Kitchen Scan", Icons.Filled.PhotoCamera, Screen.FridgeScan.route),
    QuickAddMenuItem("Scan Receipt", Icons.Filled.Receipt, Screen.ReceiptScan.route)
)
