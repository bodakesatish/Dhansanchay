package com.dhansanchay.data.source.local

import android.util.Log
//import androidx.room.MapKey // Make sure this import is present if SchemeMetaDao uses it
import com.dhansanchay.data.source.local.dao.MutualFundDao
import com.dhansanchay.data.source.local.dao.SchemeMetaDao
import com.dhansanchay.data.source.local.entity.DetailFetchStatus
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.local.entity.SchemeMetaEntity
import com.dhansanchay.data.source.mapper.SchemeMapper.toEntityModelList
import com.dhansanchay.data.source.mapper.SchemeMapper.toSchemeMetaEntityList
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
    suspend fun getSchemeListSuspend(): List<SchemeEntity>
    suspend fun deleteAllSchemes()
    suspend fun smartUpdateSchemes(data: List<SchemeApiResponse>)
    suspend fun isSchemeListEmpty(): Boolean
    suspend fun insertSchemeMeta(schemeMetaResponse: SchemeDetailApiResponse)
    fun observeSchemeMeta(schemeCode: Int): Flow<SchemeMetaEntity?>
    suspend fun getSchemeMeta(schemeCode: Int): SchemeMetaEntity?
    suspend fun getSchemeEntityForHistory(schemeCode: Int): SchemeEntity?
    suspend fun insertSchemeForHistory(schemeDetailResponse: SchemeDetailApiResponse)
    suspend fun getPendingSchemeDetails(limit: Int): List<SchemeMetaEntity>
    suspend fun getSchemesToRetry(limit: Int, maxRetryCount: Int, olderThanTimestamp: Long): List<SchemeMetaEntity>
    suspend fun updateSchemeDetailFetchStatus(schemeCode: Int, status: DetailFetchStatus, errorMessage: String? = null)
    suspend fun incrementSchemeDetailRetryCount(schemeCode: Int)
    suspend fun markSchemeAsProcessing(schemeCode: Int)
    suspend fun resetAllSchemeDetailStatusToPending()
    suspend fun getSchemeDetailFetchCounts(): Map<DetailFetchStatus, Int>
}

@Singleton
class SchemeLocalDataSourceImpl @Inject constructor(
    private val mutualFundDao: MutualFundDao,
    private val schemeMetaDao: SchemeMetaDao,
    // @IoDispatcher private val ioDispatcher: CoroutineDispatcher // Example of injected dispatcher
) : SchemeLocalDataSource {

    private val tag = this.javaClass.simpleName
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Fallback if not injected

    // ... (other method implementations like getPendingSchemeDetails, insertSchemes, etc.) ...

    override suspend fun getPendingSchemeDetails(limit: Int): List<SchemeMetaEntity> {
        return withContext(ioDispatcher) {
            Log.d(tag, "Getting pending scheme details with limit: $limit")
            schemeMetaDao.getSchemesByDetailFetchStatus(DetailFetchStatus.PENDING.name, limit)
        }
    }

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
            mutualFundDao.getSchemesList()
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
            val schemeMetas = schemeEntities.toSchemeMetaEntityList()
            if (schemeEntities.isEmpty() && data.isNotEmpty()) {
                Log.w(tag, "smartUpdateSchemes: API data was present but mapped to empty list. Clearing local schemes.")
                mutualFundDao.deleteAllSchemes()
                return@withContext
            }

            if (data.isEmpty()) {
                Log.d(tag, "smartUpdateSchemes: Remote data is empty, clearing all local schemes.")
                val timeTaken = measureTimeMillis {
                    mutualFundDao.deleteAllSchemes()
                }
                Log.d(tag, "deleteAllSchemes took $timeTaken ms")
                return@withContext
            }

            val timeTaken = measureTimeMillis {
                Log.d(tag, "Upserting ${schemeEntities.size} schemes via smartUpdateSchemes")
                mutualFundDao.upsertSchemes(schemeEntities)
            }
            Log.d(tag, "Upserting schemes took $timeTaken ms")
            val timeTakenMeta = measureTimeMillis {
                Log.d(tag, "Upserting ${schemeMetas.size} timeTakenMeta")
                schemeMetaDao.upsertSchemeMetas(schemeMetas)
            }
            Log.d(tag, "Upserting schemes meta took $timeTakenMeta ms")

        }
    }

    override suspend fun isSchemeListEmpty(): Boolean {
        return withContext(ioDispatcher) {
            val count = mutualFundDao.getSchemesList().size
            Log.d(tag, "Checking if scheme list is empty. Count: $count")
            count == 0
        }
    }

    override suspend fun insertSchemeMeta(schemeMetaResponse: SchemeDetailApiResponse) {
        withContext(ioDispatcher) {
            Log.d(tag, "Inserting/updating scheme meta for code: ${schemeMetaResponse.meta.schemeCode}")
            schemeMetaDao.upsertSchemeMeta(schemeMetaResponse.mapToData())
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

    override suspend fun getSchemeEntityForHistory(schemeCode: Int): SchemeEntity? {
        return withContext(ioDispatcher) {
            Log.d(tag, "Getting scheme entity for history, code: $schemeCode")
            mutualFundDao.getSchemeByCode(schemeCode)
        }
    }

    override suspend fun insertSchemeForHistory(schemeDetailResponse: SchemeDetailApiResponse) {
        withContext(ioDispatcher) {
            Log.d(tag, "Inserting scheme for history, code: ${schemeDetailResponse.meta.schemeCode}")
            val schemeMetaEntity = schemeDetailResponse.mapToData()
            Log.w(tag, "insertSchemeForHistory: mapToData produced ${schemeMetaEntity?.javaClass?.simpleName}. Ensure this is the correct entity type and DAO for history.")
            // Correct logic depends on how 'history' relates to SchemeEntity vs SchemeMetaEntity
        }
    }

    override suspend fun getSchemesToRetry(limit: Int, maxRetryCount: Int, olderThanTimestamp: Long): List<SchemeMetaEntity> {
        return withContext(ioDispatcher) {
            Log.d(tag, "Getting schemes to retry with limit: $limit, maxRetry: $maxRetryCount, olderThan: $olderThanTimestamp")
            // Assuming DetailFetchStatus.SUCCESS.name is the correct default for successStatus
            // The DAO method `getSchemesToRetry` default for `successStatus` is DetailFetchStatus.SUCCESS.name.
            // It filters for schemes where detailFetchStatus is NOT successStatus.
            // So if you're looking for ERROR states to retry, this query is correct.
            schemeMetaDao.getSchemesToRetry(
                maxRetryCount = maxRetryCount,
                olderThanTimestamp = olderThanTimestamp,
                limit = limit
                // successStatus = DetailFetchStatus.ERROR.name // This would mean "retry if NOT ERROR" which is likely not what you want.
                // Keep default successStatus or explicitly pass DetailFetchStatus.SUCCESS.name
                // if you only want to retry statuses that are not SUCCESS.
            )
        }
    }

    override suspend fun updateSchemeDetailFetchStatus(schemeCode: Int, status: DetailFetchStatus, errorMessage: String?) {
        withContext(ioDispatcher) {
            Log.d(tag, "Updating scheme detail fetch status for code: $schemeCode to $status")
            // Corrected to use the DAO method `updateDetailFetchStatus`
            schemeMetaDao.updateDetailFetchStatus(
                schemeCode = schemeCode,
                status = status.name,
                timestamp = System.currentTimeMillis(),
                errorMessage = errorMessage
            )
        }
    }

    override suspend fun incrementSchemeDetailRetryCount(schemeCode: Int) {
        withContext(ioDispatcher) {
            Log.d(tag, "Incrementing scheme detail retry count for code: $schemeCode")
            // Corrected to use the DAO method `incrementDetailRetryCount`
            // The timestamp parameter has a default in the DAO, so it's optional here.
            schemeMetaDao.incrementDetailRetryCount(schemeCode = schemeCode)
        }
    }

    override suspend fun markSchemeAsProcessing(schemeCode: Int) {
        return withContext(ioDispatcher) {
            Log.d(tag, "Marking scheme as processing: $schemeCode")
            // Corrected to use the DAO method `markSchemeAsProcessing`
            // Parameters `processingStatus` and `timestamp` have defaults in the DAO.
            schemeMetaDao.markSchemeAsProcessing(schemeCode = schemeCode)
        }
    }

    override suspend fun resetAllSchemeDetailStatusToPending() {
        return withContext(ioDispatcher) {
            Log.d(tag, "Resetting all scheme detail statuses to PENDING")
            // Corrected to use the DAO method `resetAllToPending`
            // Parameter `pendingStatus` has a default in the DAO.
            schemeMetaDao.resetAllToPending()
        }
    }

    override suspend fun getSchemeDetailFetchCounts(): Map<DetailFetchStatus, Int> {
        return withContext(ioDispatcher) {
            Log.d(tag, "Getting scheme detail fetch status counts list from DAO.")
            val statusCountsList: List<SchemeMetaDao.DetailFetchStatusCount> =
                schemeMetaDao.getSchemeDetailFetchStatusCountsList()

            Log.d(tag, "Raw list from DAO: $statusCountsList")

            val transformedMap = statusCountsList.mapNotNull { statusCount ->
                try {
                    val statusEnum = DetailFetchStatus.valueOf(statusCount.detailFetchStatus)
                    statusEnum to statusCount.count
                } catch (e: IllegalArgumentException) {
                    Log.e(tag, "Unknown DetailFetchStatus name from DAO: ${statusCount.detailFetchStatus}", e)
                    null
                }
            }.toMap()

            Log.d(tag, "Transformed counts map: $transformedMap")
            transformedMap
        }
    }
}