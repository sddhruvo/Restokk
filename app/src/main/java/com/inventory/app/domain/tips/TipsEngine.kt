package com.inventory.app.domain.tips

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipsEngine @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards TipProvider>
) {
    suspend fun getTips(category: TipCategory? = null, limit: Int = 5): List<Tip> {
        return providers
            .filter { category == null || it.category == category }
            .flatMap { it.generateTips() }
            .distinctBy { it.id }
            .sortedByDescending { it.priority }
            .take(limit)
    }
}
