package com.mg.costeoapp.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Modulo Hilt para proveer dependencias de base de datos.
 * Se completara en Fase 1 cuando se cree la base de datos Room.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
