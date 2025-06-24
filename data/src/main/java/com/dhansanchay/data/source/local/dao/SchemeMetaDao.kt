package com.dhansanchay.data.source.local.dao

import androidx.room.Dao
// import androidx.room.MapKey; // No longer needed for this approach
import androidx.room.Query
import androidx.room.Upsert
import com.dhansanchay.data.source.local.entity.DetailFetchStatus
import com.dhansanchay.data.source.local.entity.SchemeMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeMetaDao {

    // Helper data class for the counts query
    data class DetailFetchStatusCount(
        val detailFetchStatus: String, // Matches the column name from the query
        val count: Int                 // Matches the 'COUNT(*)' alias
    )

    // ... (upsertSchemeMeta, observeSchemeMetas, etc. - keep all other methods as they are) ...

    @Upsert
    suspend fun upsertSchemeMeta(schemes: SchemeMetaEntity)

    @Upsert
    suspend fun upsertSchemeMetas(schemes: List<SchemeMetaEntity>)

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

    @Query("SELECT * FROM schemes_meta WHERE detailFetchStatus = :status LIMIT :limit")
    suspend fun getSchemesByDetailFetchStatus(status: String, limit: Int): List<SchemeMetaEntity>

    @Query("SELECT * FROM schemes_meta WHERE detailFetchStatus != :successStatus AND detailFetchRetryCount < :maxRetryCount AND detailLastAttemptTimestamp < :olderThanTimestamp LIMIT :limit")
    suspend fun getSchemesToRetry(successStatus: String = DetailFetchStatus.SUCCESS.name, maxRetryCount: Int, olderThanTimestamp: Long, limit: Int): List<SchemeMetaEntity>

    @Query("UPDATE schemes_meta SET detailFetchStatus = :status, detailLastAttemptTimestamp = :timestamp, detailErrorMessage = :errorMessage WHERE schemeCode = :schemeCode")
    suspend fun updateDetailFetchStatus(schemeCode: Int, status: String, timestamp: Long, errorMessage: String?)

    @Query("UPDATE schemes_meta SET detailFetchRetryCount = detailFetchRetryCount + 1, detailLastAttemptTimestamp = :timestamp WHERE schemeCode = :schemeCode")
    suspend fun incrementDetailRetryCount(schemeCode: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schemes_meta SET detailFetchStatus = :processingStatus, detailLastAttemptTimestamp = :timestamp WHERE schemeCode = :schemeCode")
    suspend fun markSchemeAsProcessing(schemeCode: Int, processingStatus: String = DetailFetchStatus.PROCESSING.name, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schemes_meta SET detailFetchStatus = :pendingStatus, detailFetchRetryCount = 0, detailErrorMessage = NULL")
    suspend fun resetAllToPending(pendingStatus: String = DetailFetchStatus.PENDING.name)

    // MODIFIED METHOD: Returns List<DetailFetchStatusCount>
    @Query("SELECT detailFetchStatus, COUNT(*) as count FROM schemes_meta GROUP BY detailFetchStatus")
    suspend fun getSchemeDetailFetchStatusCountsList(): List<DetailFetchStatusCount> // Changed return type
}