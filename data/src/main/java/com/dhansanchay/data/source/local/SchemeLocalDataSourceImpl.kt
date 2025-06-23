package com.dhansanchay.data.source.local

import android.util.Log
import com.dhansanchay.data.source.local.dao.MutualFundDao
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.mapper.SchemeMapper.toEntityModelList
import com.dhansanchay.data.source.remote.model.SchemeApiResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

interface SchemeLocalDataSource {
    suspend fun insertSchemes(data: List<SchemeApiResponse>) // Input is API response type
    fun observeSchemeList(): Flow<List<SchemeEntity>> // Output is Entity type, observable
    fun getSchemeList(): List<SchemeEntity>
    suspend fun deleteAllSchemes()
    // Add other methods like getSchemeByCode, insertScheme, etc. if needed
    suspend fun smartUpdateSchemes(data: List<SchemeApiResponse>)
    fun isSchemeListEmpty(): Boolean
}

@Singleton
class SchemeLocalDataSourceImpl @Inject constructor(
    private val dao: MutualFundDao,
) : SchemeLocalDataSource {

    private val tag = this.javaClass.simpleName
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun insertSchemes(data: List<SchemeApiResponse>) {
        withContext(ioDispatcher) {
            Log.d(tag, "Inserting ${data.size} schemes into local DB")
            // Ensure you have a mapper from SchemeApiResponse to SchemeEntity
            dao.upsertSchemes(data.toEntityModelList()) // Assuming individual toEntity() or a list mapper
        }
    }

    // Returns a Flow, Room handles threading for Flow observation
    override fun observeSchemeList(): Flow<List<SchemeEntity>> { //Flow<List<SchemeEntity>> {
        Log.d(tag, "Getting scheme list flow from local DB")
        return dao.observeAllSchemes()
    }

    // Returns a Flow, Room handles threading for Flow observation
    override fun getSchemeList(): List<SchemeEntity> { //Flow<List<SchemeEntity>> {
        Log.d(tag, "Getting scheme list flow from local DB")
        return dao.getSchemesList()
    }

    override suspend fun deleteAllSchemes() {
        withContext(ioDispatcher) {
            Log.d(tag, "Deleting all schemes from local DB")
            dao.deleteAllSchemes()
        }
    }

    override suspend fun smartUpdateSchemes(data: List<SchemeApiResponse>) {
        withContext(ioDispatcher) {
            val schemeEntities = data.toEntityModelList() // Map API response to local entities
            val timeTaken = measureTimeMillis {
                Log.d(tag, "Upserting all schemes from local DB")
                dao.upsertSchemes(schemeEntities)
            }
//            val timeTaken = measureTimeMillis {
//                if (schemeEntities.isNotEmpty()) {
//                    val newSchemeIds =
//                        schemeEntities.map { it.schemeCode } // Assuming SchemeEntity has 'id' as String PK
//                    Log.d(
//                        tag,
//                        "Smart updating DB: ${schemeEntities.size} new/updated, deleting others not in ${newSchemeIds.size} IDs."
//                    )
//                    // Using the DAO's @Transaction method
//                    dao.smartUpdateTransaction(schemeEntities, newSchemeIds)
//                } else {
//                    Log.d(
//                        tag,
//                        "Smart updating DB: Remote data is empty, clearing all local schemes."
//                    )
//                    dao.deleteAllSchemes()
//                }
//            }
            Log.d(tag, "Upserting all schemes took $timeTaken ms")
        }
    }

    override fun isSchemeListEmpty(): Boolean {
        return dao.getSchemesListSuspend() == 0
    }

}