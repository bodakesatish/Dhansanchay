package com.dhansanchay.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dhansanchay.data.source.local.SchemeLocalDataSource
import com.dhansanchay.data.source.remote.SchemeRemoteDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.EntryPointAccessors

class SimpleTestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val remoteDataSource: SchemeRemoteDataSource
    private val localDataSource: SchemeLocalDataSource

    init {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WorkerDependenciesEntryPoint::class.java
        )
        remoteDataSource = hiltEntryPoint.remoteDataSource()
        localDataSource = hiltEntryPoint.localDataSource()
    }

    override suspend fun doWork(): Result {
        Log.d("SimpleTestWorker", "SimpleTestWorker is running!")
        return Result.success()
    }
}