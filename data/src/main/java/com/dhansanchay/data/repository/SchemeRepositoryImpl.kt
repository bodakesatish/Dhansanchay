package com.dhansanchay.data.repository

import android.util.Log
import com.dhansanchay.data.prefs.SessionConstants
import com.dhansanchay.data.prefs.SessionManager
import com.dhansanchay.data.source.local.SchemeLocalDataSource
import com.dhansanchay.data.source.mapper.SchemeMapper.toDomainModelList
import com.dhansanchay.data.source.remote.SchemeRemoteDataSource
import com.dhansanchay.domain.utils.NetworkResult
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
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
) : SchemeRepository {
    private val defaultDispatcher: CoroutineDispatcher =
        Dispatchers.Default // For CPU-bound mapping
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val tag = this.javaClass.simpleName

    override fun getSchemeListObservable(isForceRefresh: Boolean): Flow<NetworkResult<List<SchemeModel>>> =
        flow {
            Log.d(tag, "getSchemeListObservable: Emitting loading state")
            emit(NetworkResult.Loading) // Emit loading state initially

            var remoteFetchAttempted = false
            var remoteFetchError : NetworkResult.Error? = null
            // --- Determine if a remote fetch is necessary ---
            var shouldFetchFromRemote = isForceRefresh
            if (!shouldFetchFromRemote) {
                // Check if local data is empty
                // Note: This requires a one-shot read before starting the observation flow,
                // or making the decision based on the first emission of the observation flow.
                // For simplicity here, let's assume you have a quick way to check emptiness
                // or you'll rely on the first emission (which can be slightly more complex to orchestrate).
                // Let's go with a one-shot check for emptiness for clarity in this example.
                val isLocalDataEmpty =
                    withContext(ioDispatcher) { // Assuming ioDispatcher for DB access
                        localDataSource.isSchemeListEmpty() // You'd need getSchemeListSuspend()
                    }
                if (isLocalDataEmpty) {
                    Log.d(
                        tag,
                        "getSchemeListObservable: Local data is empty, will fetch from remote."
                    )
                    shouldFetchFromRemote = true
                } else {
                    // Placeholder for staleness check (e.g., timestamp-based)
                    val isLocalDataStale = checkLocalDataStaleness() // Implement this method
                    if (isLocalDataStale) {
                        Log.d(
                            tag,
                            "getSchemeListObservable: Local data is stale, will fetch from remote."
                        )
                        shouldFetchFromRemote = true
                    }
                }
            }

            // --- Remote Fetch Section ---
            if (shouldFetchFromRemote) {
                remoteFetchAttempted = true
                // Try to refresh data from remote and update local cache
                // This part runs without directly affecting the immediate emission from local below,
                // but subsequent emissions from localDataSource.getSchemeList() will reflect the update.
                // For more complex scenarios (e.g., showing refresh error), this can be more involved.
                Log.d(tag, "getSchemeListObservable: Attempting to refresh data from remote")
                when (val remoteOutput = remoteDataSource.fetchSchemeList()) {
                    is NetworkResult.Success -> {
                        val remoteApiData = remoteOutput.data
                        if (remoteApiData != null) { // Check if data from remote is not null
                            // No need to check for isNotEmpty here, smartUpdateSchemes handles it
                            Log.d(
                                tag,
                                "Remote fetch success. Calling smartUpdateSchemes with ${remoteApiData.size} items."
                            )
                            localDataSource.smartUpdateSchemes(remoteApiData) // Single call for smart update
                            updateLastRefreshTimestamp()
                        } else {
                            Log.d(
                                tag,
                                "Remote fetch success, but data field is null. Clearing local schemes."
                            )
                            // If null data from remote means local should be cleared
                            localDataSource.deleteAllSchemes()
                        }
                    }

                    is NetworkResult.Error -> {
                        Log.i(tag, "Error: ${remoteOutput.message}")
                        remoteFetchError = NetworkResult.Error(remoteOutput.message ?: "Unknown remote error", remoteOutput.exception)
                        // Do not emit error here if we want to fall back to local data.
                        // The Flow below will emit local data. If local is empty, then an empty list will be shown.
                        // If you need to explicitly signal a refresh error, emit(remoteOutput) here,
                        // but then the local data might not be emitted if the Flow collector stops on error.
                    }

                    is NetworkResult.Loading -> {
                        Log.i(tag, "Loading")
                    }
                }
            } else {
                Log.d(tag, "getSchemeListObservable: Not fetching from remote. Will rely on local data.")
            }


            // Emit data from local data source and observe changes
            // This Flow will automatically re-emit when the underlying data in Room changes
            Log.d(tag, "getSchemeListObservable: Subscribing to local data source changes.")
            localDataSource.observeSchemeList() // This is Flow<List<SchemeEntity>>
                .map { entityList ->
                    Log.d(
                        tag,
                        "getSchemeListObservable: Local data changed, mapping ${entityList.size} entities to domain."
                    )
                    if (entityList.isEmpty() && remoteFetchAttempted && remoteFetchError != null) {
                        // If local data is empty AFTER a remote fetch attempt that failed, emit the remote error.
                        Log.w(tag, "getSchemeListObservable: Local data is empty and there was a remote fetch error. Emitting remote error.")
                        remoteFetchError!!
                    } else if (entityList.isEmpty() && remoteFetchAttempted && remoteFetchError == null) {
                        // Local data is empty, remote fetch was attempted and succeeded but resulted in no data (or cleared local)
                        Log.d(tag, "getSchemeListObservable: Local data is empty after successful remote sync (which might have returned no data).")
                        NetworkResult.Success(emptyList<SchemeModel>()) // Explicitly success with empty
                    }
                    else {
                        NetworkResult.Success(entityList.toDomainModelList())
                    }
                }.catch { e ->
                    Log.e(tag, "getSchemeListObservable: Error collecting from local data source", e)
                    val errorToEmit = if (remoteFetchAttempted && remoteFetchError != null) {
                        // If a remote fetch was tried and failed, that error is likely more relevant
                        remoteFetchError!!
                    } else {
                        NetworkResult.Error("Error reading from local database: ${e.localizedMessage}", e)
                    }
                    emit(errorToEmit)
                }
                .collect { result -> // Collect from the inner flow and emit its results
                    emit(result)
                }
        }.flowOn(defaultDispatcher) // Use default dispatcher if mapping is CPU intensive

    // Alternative: One-time fetch (less reactive, but simpler if no observation is needed)

    override suspend fun getSchemeListOnce(): NetworkResult<List<SchemeModel>> {
        Log.d(tag, "In $tag getSchemeListOnce")
        return withContext(defaultDispatcher) { // Or ioDispatcher if remote call is dominant
            when (val remoteOutput = remoteDataSource.fetchSchemeList()) {
                is NetworkResult.Success -> {
                    val data = remoteOutput.data
                    if (data.isNotEmpty()) {
                        localDataSource.deleteAllSchemes()
                        localDataSource.insertSchemes(data)
                    }
                    // Even if remote is empty, we proceed to fetch from (potentially empty) local
                }

                is NetworkResult.Error -> {
                    Log.w(
                        tag,
                        "Error fetching from remote: ${remoteOutput.message}. Will try local."
                    )
                    // Don't return error yet, try to serve from local cache
                }

                is NetworkResult.Loading -> {
                    // This state might not be directly observable here if it's a one-shot suspend function
                    // unless NetworkResult.Loading is returned from remoteDataSource.
                    // If so, you might want to return it: return NetworkResult.Loading
                }
            }

            try {
                val localOutput =
                    localDataSource.getSchemeList() // Get current list if Flow, or make DAO suspend
                val domainOutput = localOutput.toDomainModelList()
                NetworkResult.Success(domainOutput)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching from local after remote attempt", e)
                NetworkResult.Error("Failed to load schemes: ${e.localizedMessage}", e)
            }
        }
    }

    // Example (needs to be implemented properly, likely in your local data source or a utility class)
// This should be a suspend function if it involves I/O (like reading from SharedPreferences or another DB table)
    private fun checkLocalDataStaleness(): Boolean {
        // Implement your staleness logic here.
        // For example, read a lastUpdateTimestamp from SharedPreferences.
        val lastUpdateTime = sessionManager.getLong(SessionConstants.SESSION_LAST_SYNC_DATE, 0L)
        Log.i(tag, "In $tag checkLocalDataStaleness sessionSyncDateTime -> $lastUpdateTime")
         val currentTime = System.currentTimeMillis()
         val staleThreshold = TimeUnit.HOURS.toMillis(3) // e.g., 1 hour
        Log.d(tag, "checkLocalDataStaleness: Placeholder called. Returning false for now.")
        return (currentTime - lastUpdateTime > staleThreshold)
    }

    suspend fun updateLastRefreshTimestamp() {
        val timestamp = System.currentTimeMillis()
        withContext(ioDispatcher) {
            sessionManager.set(SessionConstants.SESSION_LAST_SYNC_DATE, timestamp)
        }
    }


// Ensure you have a suspend function in your LocalDataSource for a one-shot read
// In SchemeLocalDataSource.kt:
// suspend fun getSchemeListSuspend(): List<SchemeEntity>
// In SchemeLocalDataSourceImpl.kt:
// override suspend fun getSchemeListSuspend(): List<SchemeEntity> {
// return withContext(ioDispatcher) { dao.getSchemeList() } // Assuming dao.getSchemeList() is not a Flow
// }

}