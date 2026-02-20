package com.inventory.app.domain.tips

data class Tip(
    val id: String,
    val message: String,
    val category: TipCategory,
    val priority: Int = 0,
    val actionLabel: String? = null,
    val actionRoute: String? = null
)
