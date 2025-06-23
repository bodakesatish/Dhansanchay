package com.dhansanchay.di

import com.dhansanchay.data.prefs.SessionManager
import com.dhansanchay.data.prefs.SharedPreferencesManagerImpl
import com.dhansanchay.data.repository.SchemeRepositoryImpl
import com.dhansanchay.data.source.local.SchemeLocalDataSource
import com.dhansanchay.data.source.local.SchemeLocalDataSourceImpl
import com.dhansanchay.data.source.remote.SchemeRemoteDataSource
import com.dhansanchay.data.source.remote.SchemeRemoteDataSourceImpl
import com.dhansanchay.domain.repository.SchemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    internal abstract fun provideSchemeRepository(impl: SchemeRepositoryImpl): SchemeRepository

    @Binds
    @Singleton // Optional: if SchemeRemoteDataSourceImpl is stateless and can be a singleton
    abstract fun bindSchemeRemoteDataSource(
        schemeRemoteDataSourceImpl: SchemeRemoteDataSourceImpl
    ): SchemeRemoteDataSource

    @Binds
    @Singleton // Optional: if SchemeRemoteDataSourceImpl is stateless and can be a singleton
    abstract fun bindSchemeLocalDataSource(
        schemeLocalDataSourceImpl: SchemeLocalDataSourceImpl
    ): SchemeLocalDataSource

    @Binds
    internal abstract fun provideSharedPreferencesManager(sharedPreferencesManagerImpl: SharedPreferencesManagerImpl): SessionManager

}