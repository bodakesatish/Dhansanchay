package com.dhansanchay.work

import com.dhansanchay.data.source.local.SchemeLocalDataSource
import com.dhansanchay.data.source.remote.SchemeRemoteDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class) // Or ApplicationComponent if using older Hilt
interface WorkerDependenciesEntryPoint {
    fun remoteDataSource(): SchemeRemoteDataSource
    fun localDataSource(): SchemeLocalDataSource
}