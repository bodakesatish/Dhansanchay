package com.dhansanchay.data.source.local.dao

import androidx.room.*
import com.dhansanchay.data.source.local.entity.SchemeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MutualFundDao {
    // --- Mutual Fund Schemes ---

    @Insert // Or your existing insert method for multiple schemes
    suspend fun insertSchemes(schemes: List<SchemeEntity>) // Assuming this takes SchemeEntity

    @Upsert
    suspend fun upsertSchemes(schemes: List<SchemeEntity>)

    @Query("DELETE FROM mutual_fund_schemes WHERE schemeCode NOT IN (:schemeIdsToKeep)") // Assuming 'id' is your primary key column name
    suspend fun deleteSchemesNotInList(schemeIdsToKeep: List<Int>) // Adjust String if your ID is different type

    @Query("SELECT * FROM mutual_fund_schemes ORDER BY schemeName ASC")
    fun observeAllSchemes(): Flow<List<SchemeEntity>>

    @Query("SELECT * FROM mutual_fund_schemes ORDER BY schemeName ASC")
    fun getSchemesList(): List<SchemeEntity>

    @Query("SELECT * FROM mutual_fund_schemes WHERE schemeCode = :schemeCode")
    suspend fun getSchemeByCode(schemeCode: Int): SchemeEntity?

    @Query("DELETE FROM mutual_fund_schemes")
    fun deleteAllSchemes()

    // You can also create a @Transaction method here if preferred,
    // though SchemeLocalDataSourceImpl can also manage the transaction.
    @Transaction
    suspend fun clearAndInsertTransaction(schemes: List<SchemeEntity>) {
        deleteAllSchemes()
        insertSchemes(schemes)
    }

    @Query("SELECT schemeCode FROM mutual_fund_schemes") // Assuming schemeCode is the primary key column
    suspend fun getAllSchemeCodes(): List<Int>

    // New method to delete specific IDs (for batching)
    @Query("DELETE FROM mutual_fund_schemes WHERE schemeCode IN (:schemeIdsToDelete)")
    suspend fun deleteSchemesByIds(schemeIdsToDelete: List<Int>)

    @Transaction
    suspend fun smartUpdateTransaction(newSchemes: List<SchemeEntity>, newSchemeIds: List<Int>) {
        if (newSchemes.isNotEmpty()) {
            // 1. Upsert all new/updated schemes
            // This will insert new schemes or replace existing ones based on their primary key.
            upsertSchemes(newSchemes)

            // 2. Determine schemes to delete
            // Get the codes of the schemes that were just upserted.
            val currentSchemeCodes = newSchemes.map { it.schemeCode }

            // Get all scheme codes currently in the database.
            val allExistingCodes = getAllSchemeCodes()

            // Find codes that exist in the database but are NOT in the list of currentSchemeCodes.
            // These are the schemes that need to be deleted.
            val codesToDelete = allExistingCodes.filterNot { it in currentSchemeCodes }

            // 3. Delete old schemes in batches if necessary
            if (codesToDelete.isNotEmpty()) {
                val batchSize = 900 // SQLite variable limit is often 999; using a safe margin.
                codesToDelete.chunked(batchSize).forEach { batchOfCodesToDelete ->
                    deleteSchemesByIds(batchOfCodesToDelete)
                }
            }
        } else {
            // If the list of newSchemes is empty, it implies all existing schemes should be removed.
            deleteAllSchemes()
        }
    }

}