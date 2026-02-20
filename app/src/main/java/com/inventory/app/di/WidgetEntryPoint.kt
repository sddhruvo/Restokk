package com.inventory.app.di

import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.local.dao.ShoppingListDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun itemDao(): ItemDao
    fun shoppingListDao(): ShoppingListDao
}
