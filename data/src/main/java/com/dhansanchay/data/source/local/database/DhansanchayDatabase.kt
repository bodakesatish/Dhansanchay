package com.dhansanchay.data.source.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dhansanchay.data.source.local.convertors.DateConverter
import com.dhansanchay.data.source.local.dao.SchemeDao
import com.dhansanchay.data.source.local.entity.SchemeEntity


@Database(
    entities = [
        SchemeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class DhansanchayDatabase : RoomDatabase() {
    abstract fun schemeDao(): SchemeDao
}