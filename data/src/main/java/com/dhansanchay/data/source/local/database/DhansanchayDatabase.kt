package com.dhansanchay.data.source.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dhansanchay.data.source.local.dao.MutualFundDao
import com.dhansanchay.data.source.local.dao.SchemeMetaDao
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.local.entity.SchemeMetaEntity


@Database(
    entities = [
        SchemeEntity::class,
        SchemeMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DhansanchayDatabase : RoomDatabase() {
    abstract fun schemeDao(): MutualFundDao
    abstract fun schemeMetaDao(): SchemeMetaDao
}