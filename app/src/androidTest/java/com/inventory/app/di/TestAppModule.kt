package com.inventory.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inventory.app.data.local.dao.*
import com.inventory.app.data.local.db.DatabaseCallback
import com.inventory.app.data.local.db.InventoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): InventoryDatabase {
        return Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java)
            .addCallback(DatabaseCallback(context, scope))
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed onboarding as completed so tests skip onboarding.
                    // This runs synchronously BEFORE the Activity reads the setting.
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        """INSERT OR REPLACE INTO settings (key, value, value_type, description, updated_at)
                           VALUES ('onboarding_completed', 'true', 'boolean', 'Test bypass', $now)"""
                    )
                }
            })
            .allowMainThreadQueries()
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
    @Provides fun provideSmartDefaultCacheDao(db: InventoryDatabase): SmartDefaultCacheDao = db.smartDefaultCacheDao()
    @Provides fun provideCookingLogDao(db: InventoryDatabase): CookingLogDao = db.cookingLogDao()
}
