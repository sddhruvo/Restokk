package com.inventory.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryVisual(
    val icon: ImageVector,
    val color: Color
)

object CategoryVisuals {

    private val mapping = mapOf(
        "dairy & eggs" to CategoryVisual(Icons.Filled.EggAlt, Color(0xFF42A5F5)),           // Blue
        "meat & poultry" to CategoryVisual(Icons.Filled.LocalDining, Color(0xFFEF5350)),     // Red
        "seafood" to CategoryVisual(Icons.Filled.SetMeal, Color(0xFF26C6DA)),                // Cyan
        "fruits" to CategoryVisual(Icons.Filled.Eco, Color(0xFF66BB6A)),                     // Green
        "vegetables" to CategoryVisual(Icons.Filled.Grass, Color(0xFF4CAF50)),               // Dark Green
        "bread & bakery" to CategoryVisual(Icons.Filled.BakeryDining, Color(0xFFFFB74D)),    // Amber
        "grains & pasta" to CategoryVisual(Icons.Filled.RiceBowl, Color(0xFFD4A056)),        // Wheat
        "canned goods" to CategoryVisual(Icons.Filled.Inventory2, Color(0xFF78909C)),        // Blue Grey
        "condiments & sauces" to CategoryVisual(Icons.Filled.Liquor, Color(0xFFFF7043)),     // Deep Orange
        "spices & seasonings" to CategoryVisual(Icons.Filled.LocalDining, Color(0xFFFFA726)),// Orange
        "snacks" to CategoryVisual(Icons.Filled.Cookie, Color(0xFFFF8A65)),                  // Light Orange
        "beverages" to CategoryVisual(Icons.Filled.LocalCafe, Color(0xFF8D6E63)),            // Brown
        "frozen foods" to CategoryVisual(Icons.Filled.AcUnit, Color(0xFF4DD0E1)),            // Light Cyan
        "baking supplies" to CategoryVisual(Icons.Filled.BreakfastDining, Color(0xFFFFCC02)),// Yellow
        "oils & vinegars" to CategoryVisual(Icons.Filled.WaterDrop, Color(0xFFAED581)),      // Light Green
        "international foods" to CategoryVisual(Icons.Filled.Public, Color(0xFFBA68C8)),     // Purple
        "baby food" to CategoryVisual(Icons.Filled.ChildCare, Color(0xFFF48FB1)),            // Pink
        "pet food" to CategoryVisual(Icons.Filled.Pets, Color(0xFFA1887F)),                  // Light Brown
        "other" to CategoryVisual(Icons.Filled.Category, Color(0xFF90A4AE)),                 // Grey
    )

    private val default = CategoryVisual(Icons.Filled.Category, Color(0xFF90A4AE))

    fun get(categoryName: String): CategoryVisual {
        return mapping[categoryName.lowercase().trim()] ?: default
    }

    /** Returns Black or White for best contrast against [background] using perceived luminance. */
    fun contrastColor(background: Color): Color {
        val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
        return if (luminance > 0.5f) Color.Black else Color.White
    }
}
