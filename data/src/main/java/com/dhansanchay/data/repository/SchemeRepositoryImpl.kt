package com.dhansanchay.data.repository

import android.util.Log
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
import javax.inject.Inject

class SchemeRepositoryImpl @Inject constructor(
    private val remoteDataSource: SchemeRemoteDataSource,
    private val localDataSource: SchemeLocalDataSource,
) : SchemeRepository {
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default // For CPU-bound mapping

    private val tag = this.javaClass.simpleName

    override fun getSchemeListObservable(): Flow<NetworkResult<List<SchemeModel>>> = flow {
        Log.d(tag, "getSchemeListObservable: Emitting loading state")
        emit(NetworkResult.Loading) // Emit loading state initially


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
                    Log.d(tag, "Remote fetch success. Calling smartUpdateSchemes with ${remoteApiData.size} items.")
                    localDataSource.smartUpdateSchemes(remoteApiData) // Single call for smart update
                } else {
                    Log.d(tag, "Remote fetch success, but data field is null. Clearing local schemes.")
                    // If null data from remote means local should be cleared
                    localDataSource.deleteAllSchemes()
                }
            }

            is NetworkResult.Error -> {
                Log.i(tag, "Error: ${remoteOutput.message}")
                // Do not emit error here if we want to fall back to local data.
                // The Flow below will emit local data. If local is empty, then an empty list will be shown.
                // If you need to explicitly signal a refresh error, emit(remoteOutput) here,
                // but then the local data might not be emitted if the Flow collector stops on error.
            }

            is NetworkResult.Loading -> {
                Log.i(tag, "Loading")
            }
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
                NetworkResult.Success(entityList.toDomainModelList()) // Map to domain model list
            }.catch { e ->
                Log.e(tag, "getSchemeListObservable: Error collecting from local data source", e)
                emit(
                    NetworkResult.Error(
                        "Error reading from local database: ${e.localizedMessage}",
                        e
                    )
                )
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
                    Log.w(tag, "Error fetching from remote: ${remoteOutput.message}. Will try local.")
                    // Don't return error yet, try to serve from local cache
                }
                is NetworkResult.Loading -> {
                    // This state might not be directly observable here if it's a one-shot suspend function
                    // unless NetworkResult.Loading is returned from remoteDataSource.
                    // If so, you might want to return it: return NetworkResult.Loading
                }
            }

            try {
                val localOutput = localDataSource.getSchemeList() // Get current list if Flow, or make DAO suspend
                val domainOutput = localOutput.toDomainModelList()
                NetworkResult.Success(domainOutput)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching from local after remote attempt", e)
                NetworkResult.Error("Failed to load schemes: ${e.localizedMessage}", e)
            }
        }
    }


}