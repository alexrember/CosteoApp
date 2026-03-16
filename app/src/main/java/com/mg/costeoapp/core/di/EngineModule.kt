package com.mg.costeoapp.core.di

import com.mg.costeoapp.core.domain.engine.NutricionEngine
import com.mg.costeoapp.core.domain.engine.NutricionEngineImpl
import com.mg.costeoapp.core.domain.engine.PricingEngine
import com.mg.costeoapp.core.domain.engine.PricingEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindPricingEngine(impl: PricingEngineImpl): PricingEngine

    @Binds
    @Singleton
    abstract fun bindNutricionEngine(impl: NutricionEngineImpl): NutricionEngine
}
