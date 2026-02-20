package com.inventory.app.widget

import android.content.Context
import com.inventory.app.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

data class WidgetData(
    val shoppingCount: Int,
    val expiringCount: Int,
    val totalItems: Int,
    val shoppingItems: List<ShoppingItemPreview>
)

data class ShoppingItemPreview(
    val name: String,
    val quantity: Double,
    val unitAbbreviation: String?
)

suspend fun loadWidgetData(context: Context): WidgetData {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java
    )
    val itemDao = entryPoint.itemDao()
    val shoppingListDao = entryPoint.shoppingListDao()

    val today = LocalDate.now()
    val future = today.plusDays(7)
    val todayEpoch = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val futureEpoch = future.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val shoppingCount = shoppingListDao.getActiveCount().first()
    val expiringCount = itemDao.getExpiringSoonCount(todayEpoch, futureEpoch).first()
    val totalItems = itemDao.getTotalItemCount().first()

    val shoppingItems = shoppingListDao.getActiveItems().first()
        .take(5)
        .map { detail ->
            val name = detail.item?.name ?: detail.shoppingItem.customName ?: "Unknown"
            ShoppingItemPreview(
                name = name,
                quantity = detail.shoppingItem.quantity,
                unitAbbreviation = detail.unit?.abbreviation
            )
        }

    return WidgetData(
        shoppingCount = shoppingCount,
        expiringCount = expiringCount,
        totalItems = totalItems,
        shoppingItems = shoppingItems
    )
}
