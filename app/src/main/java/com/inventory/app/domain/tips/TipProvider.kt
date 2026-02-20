package com.inventory.app.domain.tips

interface TipProvider {
    val category: TipCategory
    suspend fun generateTips(): List<Tip>
}
