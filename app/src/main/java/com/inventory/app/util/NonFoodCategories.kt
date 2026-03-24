package com.inventory.app.util

object NonFoodCategories {
    private val names = setOf(
        "household & cleaning", "personal care", "health & medicine", "paper & wrap"
    )

    fun isNonFood(categoryName: String): Boolean =
        categoryName.lowercase().trim() in names
}
