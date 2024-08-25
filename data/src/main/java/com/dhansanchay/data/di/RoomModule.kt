package com.dhansanchay.data.di

import android.app.Application
import androidx.room.Room
import com.dhansanchay.data.source.local.database.DhansanchayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RoomModule {

    val DATABASE_NAME = "dsd.db"

    @Singleton
    @Provides
    fun providesDatabase(appContext: Application): DhansanchayDatabase {
        return Room.databaseBuilder(
            appContext,
            DhansanchayDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Singleton
    @Provides
    fun providesSchemeDao(database: DhansanchayDatabase) = database.schemeDao()


}