package com.inventory.app.di

import android.content.Context
import androidx.room.Room
import com.inventory.app.data.local.dao.*
import com.inventory.app.data.local.db.DatabaseCallback
import com.inventory.app.data.local.db.InventoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): InventoryDatabase {
        return Room.databaseBuilder(
            context,
            InventoryDatabase::class.java,
            "inventory.db"
        )
            .addMigrations(com.inventory.app.data.local.db.MIGRATION_1_2, com.inventory.app.data.local.db.MIGRATION_2_3, com.inventory.app.data.local.db.MIGRATION_3_4, com.inventory.app.data.local.db.MIGRATION_4_5, com.inventory.app.data.local.db.MIGRATION_5_6, com.inventory.app.data.local.db.MIGRATION_6_7)
            .addCallback(DatabaseCallback(context, scope))
            .build()
    }

    @Provides fun provideItemDao(db: InventoryDatabase): ItemDao = db.itemDao()
    @Provides fun provideCategoryDao(db: InventoryDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideSubcategoryDao(db: InventoryDatabase): SubcategoryDao = db.subcategoryDao()
    @Provides fun provideStorageLocationDao(db: InventoryDatabase): StorageLocationDao = db.storageLocationDao()
    @Provides fun provideUnitDao(db: InventoryDatabase): UnitDao = db.unitDao()
    @Provides fun provideItemImageDao(db: InventoryDatabase): ItemImageDao = db.itemImageDao()
    @Provides fun providePurchaseHistoryDao(db: InventoryDatabase): PurchaseHistoryDao = db.purchaseHistoryDao()
    @Provides fun provideStoreDao(db: InventoryDatabase): StoreDao = db.storeDao()
    @Provides fun provideUsageLogDao(db: InventoryDatabase): UsageLogDao = db.usageLogDao()
    @Provides fun provideBarcodeCacheDao(db: InventoryDatabase): BarcodeCacheDao = db.barcodeCacheDao()
    @Provides fun provideShoppingListDao(db: InventoryDatabase): ShoppingListDao = db.shoppingListDao()
    @Provides fun provideSettingsDao(db: InventoryDatabase): SettingsDao = db.settingsDao()
    @Provides fun providePantryHealthLogDao(db: InventoryDatabase): PantryHealthLogDao = db.pantryHealthLogDao()
    @Provides fun provideSavedRecipeDao(db: InventoryDatabase): SavedRecipeDao = db.savedRecipeDao()
}
