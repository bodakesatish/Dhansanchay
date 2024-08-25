package com.dhansanchay.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhansanchay.data.source.local.base.BaseDao
import com.dhansanchay.data.source.local.entity.SchemeEntity

@Dao
interface SchemeDao : BaseDao<SchemeEntity> {

    @Query("SELECT * FROM ${SchemeEntity.TABLE_NAME}")
    suspend fun getSchemeList(): List<SchemeEntity>

    @Query("DELETE FROM ${SchemeEntity.TABLE_NAME}")
    suspend fun deleteSchemeList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchemeList(list: List<SchemeEntity>) : List<Long>

}