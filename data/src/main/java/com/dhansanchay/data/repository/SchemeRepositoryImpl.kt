package com.dhansanchay.data.repository

import android.util.Log
import com.dhansanchay.data.prefs.SessionConstants
import com.dhansanchay.data.prefs.SessionManager
import com.dhansanchay.data.source.local.SchemeLocalDataSource
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.mapper.SchemeMapper.mapToDomain // Assuming a single model mapper
import com.dhansanchay.data.source.mapper.SchemeMapper.toDomainModelList
import com.dhansanchay.data.source.mapper.SchemeMetaMapper.mapToDomain // For SchemeMetaModel
// Assuming you might have a SchemeDetailApiResponse to SchemeEntity mapper
import com.dhansanchay.data.source.mapper.SchemeMetaMapper.mapToData // Example if SchemeDetailApiResponse maps to SchemeMetaEntity
import com.dhansanchay.data.source.remote.SchemeRemoteDataSource
import com.dhansanchay.data.source.remote.model.SchemeDetailApiResponse // Used by remote source
import com.dhansanchay.domain.model.SchemeMetaModel
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.utils.NetworkResult
// import com.yourpackage.DefaultDispatcher // For Hilt
// import com.yourpackage.IoDispatcher // For Hilt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SchemeRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val remoteDataSource: SchemeRemoteDataSource,
    private val localDataSource: SchemeLocalDataSource,
    // Example: Inject dispatchers for better testability
    // @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    // @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : SchemeRepository {

    // Use injected dispatchers if available, otherwise fallback to Dispatchers.Default/IO
    // For simplicity in this example, directly using Dispatchers, but injection is preferred.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val tag = SchemeRepositoryImpl::class.java.simpleName

    override fun getSchemeListObservable(isForceRefresh: Boolean): Flow<NetworkResult<List<SchemeModel>>> =
        flow {
            Log.d(tag, "getSchemeListObservable: Emitting loading state. ForceRefresh: $isForceRefresh")
            emit(NetworkResult.Loading)

            var remoteFetchAttempted = false
            var remoteFetchError: NetworkResult.Error? = null
            var shouldFetchFromRemote = isForceRefresh

            if (!shouldFetchFromRemote) {
                val isLocalDataEmpty = withContext(ioDispatcher) {
                    localDataSource.isSchemeListEmpty()
                }
                if (isLocalDataEmpty) {
                    Log.d(tag, "getSchemeListObservable: Local data is empty, will fetch from remote.")
                    shouldFetchFromRemote = true
                } else {
                    val isLocalDataStale = withContext(ioDispatcher) { // Assuming staleness check might involve I/O
                        checkLocalDataStaleness()
                    }
                    if (isLocalDataStale) {
                        Log.d(tag, "getSchemeListObservable: Local data is stale, will fetch from remote.")
                        shouldFetchFromRemote = true
                    }
                }
            }

            if (shouldFetchFromRemote) {
                remoteFetchAttempted = true
                Log.d(tag, "getSchemeListObservable: Attempting to refresh data from remote.")
                when (val remoteOutput = remoteDataSource.fetchSchemeList()) {
                    is NetworkResult.Success -> {
                        val remoteApiData = remoteOutput.data
                        // Assuming remoteApiData is List<SchemeApiResponse>
                        // smartUpdateSchemes should handle mapping SchemeApiResponse to local SchemeEntity
                        withContext(ioDispatcher) {
                            localDataSource.smartUpdateSchemes(remoteApiData)
                            updateLastRefreshTimestamp() // Update timestamp on successful sync
                        }
                        Log.d(tag, "Remote fetch success. Updated local cache with ${remoteApiData.size} items.")
                    }
                    is NetworkResult.Error -> {
                        Log.w(tag, "getSchemeListObservable: Remote fetch error: ${remoteOutput.message}")
                        remoteFetchError = remoteOutput // Store error to potentially emit later
                    }
                    is NetworkResult.Loading -> { /* Handled by initial emit */ }
                }
            } else {
                Log.d(tag, "getSchemeListObservable: Not fetching from remote. Will rely on local data.")
            }

            // Emit data from local data source and observe changes
            localDataSource.observeSchemeList() // Flow<List<SchemeEntity>>
                .map<List<SchemeEntity>, NetworkResult<List<SchemeModel>>> { entityList -> // Assuming SchemeEntity maps to SchemeModel
                    Log.d(tag, "getSchemeListObservable: Local data changed, mapping ${entityList.size} entities.")
                    if (entityList.isEmpty()) {
                        if (remoteFetchAttempted && remoteFetchError != null) {
                            Log.w(tag, "Local data empty and remote fetch failed. Emitting remote error.")
                            remoteFetchError!! // Remote error takes precedence
                        } else if (remoteFetchAttempted && remoteFetchError == null) {
                            // Remote fetch attempted, succeeded, but resulted in no data (or cleared local)
                            Log.d(tag, "Local data empty after successful remote sync (which might have returned no data).")
                            NetworkResult.Success(emptyList())
                        } else {
                            // No remote fetch attempted, local is just empty
                            Log.d(tag, "Local data empty, no remote fetch attempted or failed fetch with no specific error to show yet.")
                            NetworkResult.Success(emptyList())
                        }
                    } else {
                        NetworkResult.Success(entityList.toDomainModelList()) // Map List<SchemeEntity> to List<SchemeModel>
                    }
                }
                .catch { e ->
                    Log.e(tag, "getSchemeListObservable: Error collecting from local data source", e)
                    val errorToEmit = if (remoteFetchAttempted && remoteFetchError != null) {
                        remoteFetchError!!
                    } else {
                        NetworkResult.Error("Error reading from local database: ${e.localizedMessage}",-2, e)
                    }
                    emit(errorToEmit)
                }
                .collect { result ->
                    emit(result)
                }
        }.flowOn(defaultDispatcher) // Use defaultDispatcher if mapping is CPU intensive, else ioDispatcher for flow operations

    override suspend fun getSchemeListOnce(): NetworkResult<List<SchemeModel>> {
        Log.d(tag, "getSchemeListOnce: Fetching scheme list once.")
        // Attempt remote fetch first
        when (val remoteOutput = remoteDataSource.fetchSchemeList()) {
            is NetworkResult.Success -> {
                val remoteApiData = remoteOutput.data
                Log.d(tag, "getSchemeListOnce: Remote fetch success with ${remoteApiData.size} items.")
                withContext(ioDispatcher) {
                    // Decide on update strategy: deleteAll + insertAll or smartUpdate
                    localDataSource.deleteAllSchemes() // Simpler for one-shot
                    localDataSource.insertSchemes(remoteApiData) // Assuming insertSchemes takes List<SchemeApiResponse>
                    updateLastRefreshTimestamp()
                }
                // After updating, read from local as the source of truth
                return try {
                    val localSchemes = withContext(ioDispatcher) { localDataSource.getSchemeListSuspend() } // Needs suspend version
                    NetworkResult.Success(localSchemes.toDomainModelList())
                } catch (e: Exception) {
                    Log.e(tag, "getSchemeListOnce: Error fetching from local after remote success", e)
                    NetworkResult.Error("Failed to load schemes from local cache: ${e.localizedMessage}", -2, e)
                }
            }
            is NetworkResult.Error -> {
                Log.w(tag, "getSchemeListOnce: Error fetching from remote: ${remoteOutput.message}. Attempting local.")
                // Fallback to local cache
                return try {
                    val localSchemes = withContext(ioDispatcher) { localDataSource.getSchemeListSuspend() }
                    if (localSchemes.isNotEmpty()) {
                        NetworkResult.Success(localSchemes.toDomainModelList())
                    } else {
                        Log.w(tag, "getSchemeListOnce: Local cache is empty after remote fetch failed.")
                        // Propagate the original remote error if local is empty
                        remoteOutput
                    }
                } catch (e: Exception) {
                    Log.e(tag, "getSchemeListOnce: Error fetching from local after remote failure", e)
                    // If local also fails, propagate remote error or a combined one
                    NetworkResult.Error("Failed to load schemes from remote or local: ${remoteOutput.message} & ${e.localizedMessage}",-2, e)
                }
            }
            is NetworkResult.Loading -> {
                // This state isn't typically returned from a suspend function like this
                // unless the remoteDataSource itself can return it.
                // For a one-shot, usually you'd just suspend until success/error.
                Log.d(tag, "getSchemeListOnce: Remote source is loading (unexpected for suspend function).")
                // Propagate if it can happen, or handle as error
                return NetworkResult.Error("Remote data source is in an unexpected loading state.")
            }
        }
    }

    override fun getSchemeDetailNavLatest(schemeCode: Int, forceRefresh: Boolean): Flow<NetworkResult<SchemeMetaModel>> = flow {
        emit(NetworkResult.Loading)
        Log.d(tag, "getSchemeDetailNavLatest called. SchemeCode: $schemeCode, ForceRefresh: $forceRefresh")

        var remoteFetchError: NetworkResult.Error? = null
        var shouldFetchFromRemote = forceRefresh
        var localDataInitiallyExisted = false

        val initialLocalEntity = withContext(ioDispatcher) { localDataSource.getSchemeMeta(schemeCode) } // Assuming returns SchemeMetaEntity?
        localDataInitiallyExisted = initialLocalEntity != null

        if (!shouldFetchFromRemote && initialLocalEntity == null) {
            Log.d(tag, "Local data for scheme $schemeCode is null, will fetch from remote.")
            shouldFetchFromRemote = true
        }
        // TODO: Add staleness check for individual items if needed

        if (shouldFetchFromRemote) {
            Log.d(tag, "Attempting to refresh scheme $schemeCode from remote.")
            when (val remoteOutput = remoteDataSource.fetchSchemeDetail(schemeCode)) { // Returns SchemeDetailApiResponse
                is NetworkResult.Success -> {
                    val schemeDetailResponse = remoteOutput.data
                    // Assuming SchemeDetailApiResponse maps to SchemeMetaEntity
                    // And insertSchemeMeta takes SchemeMetaEntity or handles the mapping
                    withContext(ioDispatcher) { localDataSource.insertSchemeMeta(schemeDetailResponse) } // Example mapping
                    // TODO: update timestamp for this specific item if needed
                }
                is NetworkResult.Error -> {
                    Log.w(tag, "Error fetching scheme $schemeCode from remote: ${remoteOutput.message}")
                    remoteFetchError = remoteOutput
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Attempt to load from local dataSource after potential refresh
        // This makes local DB the single source of truth for observation
        val finalLocalEntity = withContext(ioDispatcher) { localDataSource.getSchemeMeta(schemeCode) }

        if (finalLocalEntity != null) {
            emit(NetworkResult.Success(finalLocalEntity.mapToDomain())) // mapToDomain for SchemeMetaEntity -> SchemeMetaModel
            if (shouldFetchFromRemote && remoteFetchError != null && !localDataInitiallyExisted) {
                // If we forced a fetch because local was empty, and fetch failed,
                // the success above might be misleading if we didn't get new data.
                // This case is tricky. The current logic will show old data if fetch failed but old data existed.
                // If old data didn't exist and fetch failed, `finalLocalEntity` would be null.
                Log.w(tag, "Served local data for $schemeCode, but a forced remote refresh failed: ${remoteFetchError.message}")
            }
        } else {
            if (remoteFetchError != null) {
                // If local data is still null and a remote fetch error occurred
                emit(remoteFetchError)
            } else if (shouldFetchFromRemote) {
                // Remote fetch was attempted (either forced or because local was empty),
                // it didn't error, but data is still null (e.g., API returned success with null data).
                emit(NetworkResult.Error("Scheme detail not found for code: $schemeCode after remote check."))
            }
            else {
                // Not forced, initial local was null, and no remote fetch was made or it was successful with no data
                emit(NetworkResult.Error("Scheme detail not found for code: $schemeCode."))
            }
        }
    }.catch { e ->
        Log.e(tag, "Error in getSchemeDetailNavLatest flow for $schemeCode", e)
        emit(NetworkResult.Error("Failed to load scheme detail: ${e.localizedMessage}", -2, e))
    }.flowOn(defaultDispatcher) // For mapping, ioDispatcher for localDataSource calls.

    /**
     * Fetches historical data or a specific representation of a scheme as SchemeModel.
     * The exact source (local/remote) and entity for SchemeModel details needs to be defined.
     * This implementation assumes a similar pattern to getSchemeDetailNavLatest but for SchemeModel.
     */
    override suspend fun getSchemeDetailNavHistory(schemeCode: Int, forceRefresh: Boolean): Flow<NetworkResult<SchemeModel>> = flow {
        emit(NetworkResult.Loading)
        Log.d(tag, "getSchemeDetailNavHistory called. SchemeCode: $schemeCode, ForceRefresh: $forceRefresh")

        var remoteFetchError: NetworkResult.Error? = null
        var shouldFetchFromRemote = forceRefresh

        // Assuming localDataSource has a method to get a single SchemeEntity (or similar for SchemeModel)
        // For demonstration, let's assume it might come from a different local source or a specific query.
        // val initialLocalSchemeEntity = withContext(ioDispatcher) { localDataSource.getSchemeEntityByCode(schemeCode) }
        // For simplicity, let's assume SchemeDetailApiResponse can also be mapped to a SchemeEntity for history
        val initialLocalSchemeEntity = withContext(ioDispatcher) { localDataSource.getSchemeEntityForHistory(schemeCode) }  // Needs this method in localDataSource

        if (!shouldFetchFromRemote && initialLocalSchemeEntity == null) {
            Log.d(tag, "Local history data for scheme $schemeCode is null, will fetch from remote.")
            shouldFetchFromRemote = true
        }
        // TODO: Add staleness check if applicable for historical data

        if (shouldFetchFromRemote) {
            Log.d(tag, "Attempting to refresh history for scheme $schemeCode from remote.")
            // Assuming remoteDataSource.fetchSchemeDetail can be used, or a new method is needed
            when (val remoteOutput = remoteDataSource.fetchSchemeDetail(schemeCode)) { // Returns SchemeDetailApiResponse
                is NetworkResult.Success -> {
                    val schemeDetailResponse = remoteOutput.data
                    // Map SchemeDetailApiResponse to a SchemeEntity suitable for history and insert/update
                    // withContext(ioDispatcher) { localDataSource.insertSchemeEntityForHistory(schemeDetailResponse.toSchemeEntityForHistory()) }
                    Log.d(tag, "Remote fetch for history of $schemeCode successful. Data processed (if applicable).")
                    // Note: If history is purely remote and not cached, this step might be different.
                }
                is NetworkResult.Error -> {
                    Log.w(tag, "Error fetching history for scheme $schemeCode from remote: ${remoteOutput.message}")
                    remoteFetchError = remoteOutput
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Attempt to load from local dataSource after potential refresh
        val finalLocalSchemeEntity = withContext(ioDispatcher) { localDataSource.getSchemeEntityForHistory(schemeCode) }

        if (finalLocalSchemeEntity != null) {
            emit(NetworkResult.Success(finalLocalSchemeEntity.mapToDomain())) // Map SchemeEntity -> SchemeModel
        } else {
            if (remoteFetchError != null) {
                emit(remoteFetchError)
            } else if (shouldFetchFromRemote) {
                emit(NetworkResult.Error("Scheme history not found for code: $schemeCode after remote check."))
            } else {
                emit(NetworkResult.Error("Scheme history not found for code: $schemeCode."))
            }
        }
    }.catch { e ->
        Log.e(tag, "Error in getSchemeDetailNavHistory flow for $schemeCode", e)
        emit(NetworkResult.Error("Failed to load scheme history: ${e.localizedMessage}", -2, e))
    }.flowOn(defaultDispatcher)


    private suspend fun checkLocalDataStaleness(): Boolean = withContext(ioDispatcher) {
        val lastUpdateTime = sessionManager.getLong(SessionConstants.SESSION_LAST_SYNC_DATE, 0L)
        if (lastUpdateTime == 0L) return@withContext true // No sync time, consider stale for first fetch
        val currentTime = System.currentTimeMillis()
        val staleThreshold = TimeUnit.HOURS.toMillis(3) // 3 hours
        val isStale = (currentTime - lastUpdateTime > staleThreshold)
        Log.d(tag, "checkLocalDataStaleness: lastUpdate=$lastUpdateTime, current=$currentTime, stale=$isStale")
        isStale
    }

    private suspend fun updateLastRefreshTimestamp() = withContext(ioDispatcher) {
        val timestamp = System.currentTimeMillis()
        sessionManager.set(SessionConstants.SESSION_LAST_SYNC_DATE, timestamp)
        Log.d(tag, "Updated last refresh timestamp to: $timestamp")
    }
}

// Ensure your LocalDataSource interface and implementation have these (or similar) methods:
// interface SchemeLocalDataSource {
//     suspend fun isSchemeListEmpty(): Boolean
//     fun observeSchemeList(): Flow<List<SchemeEntity>> // Assuming SchemeEntity
//     suspend fun smartUpdateSchemes(schemes: List<SchemeApiResponse>) // Or List<SchemeEntity> if mapped before
//     suspend fun deleteAllSchemes()
//     suspend fun insertSchemes(schemes: List<SchemeApiResponse>) // Or List<SchemeEntity>
//     suspend fun getSchemeListSuspend(): List<SchemeEntity>
//
//     suspend fun getSchemeMetaEntity(schemeCode: Int): SchemeMetaEntity? // Assuming SchemeMetaEntity
//     suspend fun insertSchemeMeta(schemeMetaEntity: SchemeMetaEntity)    // Assuming SchemeMetaEntity
// Or: suspend fun insertSchemeMeta(schemeDetail: SchemeDetailApiResponse) if mapping is internal
//
//     suspend fun getSchemeEntityForHistory(schemeCode: Int): SchemeEntity? // For getSchemeDetailNavHistory
// Or: suspend fun insertSchemeEntityForHistory(schemeEntity: SchemeEntity) if you cache history
// }

// Ensure your Mappers are correctly defined:
// object SchemeMapper {
//    fun List<SchemeEntity>.toDomainModelList(): List<SchemeModel> = map { it.toDomainModel() }
//    fun SchemeEntity.toDomainModel(): SchemeModel = SchemeModel(...)
// }
// object SchemeMetaMapper {
//    fun SchemeMetaEntity.mapToDomain(): SchemeMetaModel = SchemeMetaModel(...)
//    fun SchemeDetailApiResponse.toEntity(): SchemeMetaEntity = SchemeMetaEntity(...) // Example
// }