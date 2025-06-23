package com.dhansanchay.data.source.local.di

import android.app.Application
import androidx.room.Room
import com.dhansanchay.data.source.local.database.DhansanchayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val DATABASE_NAME = "dsd.db"

    @Singleton
    @Provides
    fun providesDatabase( appContext: Application): DhansanchayDatabase {
        return Room.databaseBuilder(
            appContext,
            DhansanchayDatabase::class.java,
            DATABASE_NAME
        )
            // .fallbackToDestructiveMigration() // Add if you want this behavior during schema changes without migrations
            // .addMigrations(MIGRATION_1_2, ...) // Add actual migrations
            // .createFromAsset("initial_data.db") // If you have a prepopulated DB
            .build()
    }

    @Singleton
    @Provides
    fun providesSchemeDao(database: DhansanchayDatabase) = database.schemeDao()

    @Singleton
    @Provides
    fun providesSchemeMetaDao(database: DhansanchayDatabase) = database.schemeMetaDao()


}