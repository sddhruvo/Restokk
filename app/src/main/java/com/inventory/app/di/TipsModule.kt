package com.inventory.app.di

import com.inventory.app.domain.tips.TipProvider
import com.inventory.app.domain.tips.providers.PantryHealthTipProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class TipsModule {

    @Binds
    @IntoSet
    abstract fun bindPantryHealthTipProvider(impl: PantryHealthTipProvider): TipProvider
}
