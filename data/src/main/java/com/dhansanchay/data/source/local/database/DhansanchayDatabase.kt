package com.dhansanchay.data.source.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dhansanchay.data.source.local.dao.MutualFundDao
import com.dhansanchay.data.source.local.entity.SchemeEntity


@Database(
    entities = [
        SchemeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DhansanchayDatabase : RoomDatabase() {
    abstract fun schemeDao(): MutualFundDao
}