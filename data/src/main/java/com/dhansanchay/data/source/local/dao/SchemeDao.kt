package com.dhansanchay.data.source.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhansanchay.data.source.local.base.BaseDao
import com.dhansanchay.data.source.local.entity.SchemeEntity

@Dao
interface SchemeDao : BaseDao<SchemeEntity> {

    @Query("SELECT * FROM ${SchemeEntity.TABLE_NAME}")
    suspend fun getSchemeList() : List<SchemeEntity>

    @Query("SELECT COUNT(*) FROM ${SchemeEntity.TABLE_NAME}")
    suspend fun getSchemeListCount() : Int

    @Query("DELETE FROM ${SchemeEntity.TABLE_NAME}")
    suspend fun deleteSchemeList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchemeList(list: List<SchemeEntity>) : List<Long>


    @Query("SELECT * FROM ${SchemeEntity.TABLE_NAME} LIMIT :pageSize OFFSET :offset")
    fun getItems(pageSize: Int, offset: Int): List<SchemeEntity>

    @Query("SELECT * FROM ${SchemeEntity.TABLE_NAME} ORDER BY ${SchemeEntity.Columns.ID} LIMIT :limit OFFSET :offset")
    fun getPaginatedSchemeList(limit: Int, offset: Int) : List<SchemeEntity>
}