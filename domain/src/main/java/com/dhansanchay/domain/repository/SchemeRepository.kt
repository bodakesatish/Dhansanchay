package com.dhansanchay.domain.repository

import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.utils.NetworkResult
import kotlinx.coroutines.flow.Flow

interface SchemeRepository {
    // Option 1: Return Flow for observable data (recommended for lists)
    fun getSchemeListObservable(isForceRefresh: Boolean): Flow<NetworkResult<List<SchemeModel>>>

    // Option 2: Suspend function for one-time fetch (if UI doesn't need to observe changes directly)
    suspend fun getSchemeListOnce(): NetworkResult<List<SchemeModel>>

// Example for detail (can also be Flow or suspend)
// suspend fun getSchemeDetail(schemeCode: Int): NetworkResult<SchemeDetailModel>
}