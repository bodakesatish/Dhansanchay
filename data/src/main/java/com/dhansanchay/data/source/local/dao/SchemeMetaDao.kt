package com.dhansanchay.data.source.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dhansanchay.data.source.local.entity.SchemeMetaEntity // Your entity path
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeMetaDao {

    @Upsert // Inserts if new, updates if schemeCode already exists
    suspend fun upsertSchemeMeta(schemes: SchemeMetaEntity)

    @Query("SELECT * FROM schemes_meta ORDER BY schemeName ASC")
    fun observeSchemeMetas(): Flow<List<SchemeMetaEntity>>

    @Query("SELECT * FROM schemes_meta WHERE schemeCode = :schemeCode")
    suspend fun getSchemeMetaByCode(schemeCode: Int): SchemeMetaEntity?

    @Query("SELECT * FROM schemes_meta WHERE schemeCode = :schemeCode")
    fun observeSchemeMetaByCode(schemeCode: Int): Flow<SchemeMetaEntity?>

    @Query("DELETE FROM schemes_meta")
    suspend fun clearAllSchemeMetas()

    @Query("SELECT COUNT(*) FROM schemes_meta")
    suspend fun count(): Int
}