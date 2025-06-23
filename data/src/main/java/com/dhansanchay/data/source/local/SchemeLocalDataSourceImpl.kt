package com.dhansanchay.data.source.local

import android.util.Log
import com.dhansanchay.data.source.local.dao.MutualFundDao
import com.dhansanchay.data.source.local.dao.SchemeMetaDao
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.local.entity.SchemeMetaEntity
import com.dhansanchay.data.source.mapper.SchemeMapper.toEntityModelList
import com.dhansanchay.data.source.mapper.SchemeMetaMapper.mapToData
import com.dhansanchay.data.source.remote.model.SchemeApiResponse
import com.dhansanchay.data.source.remote.model.SchemeDetailApiResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

interface SchemeLocalDataSource {
    suspend fun insertSchemes(data: List<SchemeApiResponse>)
    fun observeSchemeList(): Flow<List<SchemeEntity>>
    suspend fun getSchemeListSuspend(): List<SchemeEntity> // Renamed and made suspend
    suspend fun deleteAllSchemes()
    suspend fun smartUpdateSchemes(data: List<SchemeApiResponse>)
    suspend fun isSchemeListEmpty(): Boolean // Made suspend
    suspend fun insertSchemeMeta(schemeMetaResponse: SchemeDetailApiResponse) // Parameter name more descriptive
    fun observeSchemeMeta(schemeCode: Int): Flow<SchemeMetaEntity?>
    suspend fun getSchemeMeta(schemeCode: Int): SchemeMetaEntity? // This is fine, matches repository's need

    // Methods needed for getSchemeDetailNavHistory (example)
    // Adjust SchemeEntity if history uses a different entity structure
    suspend fun getSchemeEntityForHistory(schemeCode: Int): SchemeEntity?
    suspend fun insertSchemeForHistory(schemeDetailResponse: SchemeDetailApiResponse) // Or takes SchemeEntity
}

@Singleton
class SchemeLocalDataSourceImpl @Inject constructor(
    private val mutualFundDao: MutualFundDao, // Renamed for clarity from 'dao'
    private val schemeMetaDao: SchemeMetaDao,
    // @IoDispatcher private val ioDispatcher: CoroutineDispatcher // Example of injected dispatcher
) : SchemeLocalDataSource {

    private val tag = this.javaClass.simpleName
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Fallback if not injected

    override suspend fun insertSchemes(data: List<SchemeApiResponse>) {
        withContext(ioDispatcher) {
            Log.d(tag, "Inserting ${data.size} schemes into local DB via insertSchemes")
            mutualFundDao.upsertSchemes(data.toEntityModelList())
        }
    }

    override fun observeSchemeList(): Flow<List<SchemeEntity>> {
        Log.d(tag, "Observing scheme list from local DB")
        return mutualFundDao.observeAllSchemes()
    }

    override suspend fun getSchemeListSuspend(): List<SchemeEntity> {
        return withContext(ioDispatcher) {
            Log.d(tag, "Getting scheme list (suspend) from local DB")
            mutualFundDao.getSchemesList() // Assuming this is a blocking DAO call
        }
    }

    override suspend fun deleteAllSchemes() {
        withContext(ioDispatcher) {
            Log.d(tag, "Deleting all schemes from local DB")
            mutualFundDao.deleteAllSchemes()
        }
    }

    override suspend fun smartUpdateSchemes(data: List<SchemeApiResponse>) {
        withContext(ioDispatcher) {
            val schemeEntities = data.toEntityModelList()
            if (schemeEntities.isEmpty() && data.isNotEmpty()) {
                // This case suggests the remote API returned data, but mapping resulted in an empty list.
                // Could be an issue with the mapper or unexpected API data.
                Log.w(tag, "smartUpdateSchemes: API data was present but mapped to empty list. Clearing local schemes as a precaution.")
                mutualFundDao.deleteAllSchemes()
                return@withContext
            }

            if (data.isEmpty()) { // Explicitly handle if the API itself returns an empty list
                Log.d(tag, "smartUpdateSchemes: Remote data is empty, clearing all local schemes.")
                val timeTaken = measureTimeMillis {
                    mutualFundDao.deleteAllSchemes()
                }
                Log.d(tag, "deleteAllSchemes took $timeTaken ms")
                return@withContext
            }


            // To implement true smart update (with deletes), you need a @Transaction method in DAO
            // For example:
            // val timeTaken = measureTimeMillis {
            //     val newSchemeCodes = schemeEntities.map { it.schemeCode }
            //     Log.d(tag, "Smart updating DB: ${schemeEntities.size} new/updated items. Identifying items to delete.")
            //     mutualFundDao.smartUpdateTransaction(schemeEntities, newSchemeCodes)
            // }
            // Log.d(tag, "smartUpdateTransaction took $timeTaken ms")

            // Current simpler upsert:
            val timeTaken = measureTimeMillis {
                Log.d(tag, "Upserting ${schemeEntities.size} schemes via smartUpdateSchemes")
                mutualFundDao.upsertSchemes(schemeEntities)
            }
            Log.d(tag, "Upserting schemes took $timeTaken ms")
        }
    }

    override suspend fun isSchemeListEmpty(): Boolean {
        return withContext(ioDispatcher) {
            // Assuming mutualFundDao.getSchemesCount() is more efficient if it exists
            // Or use a dedicated count query.
            val count = mutualFundDao.getSchemesList().size // Example: suspend fun getSchemesCount(): Int
            Log.d(tag, "Checking if scheme list is empty. Count: $count")
            count == 0
            // Fallback if only getSchemesListSuspend is available (less efficient for just checking emptiness)
            // mutualFundDao.getSchemesList().isEmpty()
        }
    }

    override suspend fun insertSchemeMeta(schemeMetaResponse: SchemeDetailApiResponse) {
        withContext(ioDispatcher) {
            Log.d(tag, "Inserting/updating scheme meta for code: ${schemeMetaResponse.meta.schemeCode}")
            schemeMetaDao.upsertSchemeMeta(schemeMetaResponse.mapToData()) // mapToData maps to SchemeMetaEntity
        }
    }

    override fun observeSchemeMeta(schemeCode: Int): Flow<SchemeMetaEntity?> {
        Log.d(tag, "Observing scheme meta for code: $schemeCode")
        return schemeMetaDao.observeSchemeMetaByCode(schemeCode)
    }

    override suspend fun getSchemeMeta(schemeCode: Int): SchemeMetaEntity? {
        return withContext(ioDispatcher) {
            Log.d(tag, "Getting scheme meta for code: $schemeCode")
            schemeMetaDao.getSchemeMetaByCode(schemeCode)
        }
    }

    // --- Implementation for methods needed by getSchemeDetailNavHistory ---
    // These are examples and depend on your actual data structure for "history"

    override suspend fun getSchemeEntityForHistory(schemeCode: Int): SchemeEntity? {
        return withContext(ioDispatcher) {
            Log.d(tag, "Getting scheme entity for history, code: $schemeCode")
            // Example: Reuse existing DAO method if applicable, or a new one
            // This might be the same as getSchemeByCode if SchemeEntity holds all needed info
            mutualFundDao.getSchemeByCode(schemeCode) // Assuming this method exists in MutualFundDao
        }
    }

    override suspend fun insertSchemeForHistory(schemeDetailResponse: SchemeDetailApiResponse) {
        withContext(ioDispatcher) {
            Log.d(tag, "Inserting scheme for history, code: ${schemeDetailResponse.meta.schemeCode}")
            // Assuming SchemeDetailApiResponse can be mapped to SchemeEntity
            // or a specific "SchemeHistoryEntity"
            // For this example, let's assume it maps to a general SchemeEntity
            // and upsert is desired.
            val schemeEntity = schemeDetailResponse.mapToData() // Example conversion
            schemeEntity?.let {
             //   mutualFundDao.upsertSchemes(it) // Upserting a single item list
            } ?: Log.w(tag, "Could not map SchemeDetailApiResponse to SchemeEntity for history insertion.")
        }
    }
}