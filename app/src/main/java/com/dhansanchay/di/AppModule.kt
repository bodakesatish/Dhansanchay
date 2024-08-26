package com.dhansanchay.di

import com.dhansanchay.data.repository.SchemeRepositoryImpl
import com.dhansanchay.data.security.prefs.SessionManager
import com.dhansanchay.data.security.prefs.SharedPreferencesManagerImpl
import com.dhansanchay.domain.repository.SchemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    internal abstract fun provideSchemeRepository(impl: SchemeRepositoryImpl): SchemeRepository


    @Binds
    internal abstract fun provideSharedPreferencesManager(sharedPreferencesManagerImpl: SharedPreferencesManagerImpl): SessionManager


}