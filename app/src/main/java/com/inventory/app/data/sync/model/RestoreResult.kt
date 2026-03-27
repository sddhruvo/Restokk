package com.inventory.app.data.sync.model

/**
 * Result of a restore operation — counts of entities restored and any warnings.
 */
data class RestoreResult(
    val itemsRestored: Int = 0,
    val shoppingRestored: Int = 0,
    val recipesRestored: Int = 0,
    val categoriesRestored: Int = 0,
    val locationsRestored: Int = 0,
    val unitsRestored: Int = 0,
    val settingsRestored: Int = 0,
    val warnings: List<String> = emptyList()
)
