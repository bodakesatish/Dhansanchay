package com.dhansanchay.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import com.dhansanchay.data.mapper.base.BaseOutputRemoteMapper
import com.dhansanchay.data.security.prefs.SessionConstants
import com.dhansanchay.data.security.prefs.SessionManager
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.data.source.local.paging.RoomPagingSource
import com.dhansanchay.data.source.local.source.SchemeDataSourceLocal
import com.dhansanchay.data.source.remote.source.SchemeDataSourceRemote
import com.dhansanchay.domain.model.ResponseCode
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.usecases.PaginatedSchemeListUseCase
import com.dhansanchay.domain.usecases.PagingSchemeListUseCase
import com.dhansanchay.domain.usecases.SchemeListCountUseCase
import java.util.Date
import javax.inject.Inject

class SchemeRepositoryImpl
@Inject
constructor(
    private val sessionManager: SessionManager,
    private val remoteDataSource: SchemeDataSourceRemote,
    private val localDataSource: SchemeDataSourceLocal,
) : SchemeRepository {

    private val tag = this.javaClass.simpleName


    override suspend fun getSchemeList(): SchemeListCountUseCase.Response {
        Log.i(tag, "In $tag getSchemeList")
        val response = SchemeListCountUseCase.Response()

        val THIRTY_SECONDS_IN_MILLIS =30000L // 30 seconds * 1000 milliseconds/second
        val ONE_HOUR_IN_MILLIS = 3600000L // 60 minutes * 60 seconds * 1000 milliseconds
        val sessionSyncDateTime = sessionManager.getLong(SessionConstants.SESSION_LAST_SYNC_DATE,0L)
        Log.i(tag, "In $tag getSchemeList sessionSyncDateTime -> $sessionSyncDateTime")
        val currentDateTime = Date().time
        Log.i(tag, "In $tag getSchemeList currentDateTime -> $currentDateTime")
        val difference = currentDateTime - sessionSyncDateTime
        Log.i(tag, "In $tag getSchemeList difference -> $difference")

        if(difference < THIRTY_SECONDS_IN_MILLIS) {
            response.setResponseCode(ResponseCode.Success)
            val localOutput = localDataSource.getSchemeListCount()
            val responseData = if (localOutput is BaseOutput.Success) {
                localOutput.output!!
            } else {
                0
            }
            response.setData(responseData)
            return response
        }

        val remoteOutput = remoteDataSource.getSchemeList()
        if(remoteOutput is BaseOutput.Success) {
            response.setResponseCode(ResponseCode.Success)
            localDataSource.deleteAll()
            remoteOutput.output?.let { localDataSource.insertAll(it) }
            response.setData(remoteOutput.output?.size)
        } else {
            response.setResponseCode(ResponseCode.Fail)
        }
        return response
    }

    override suspend fun getPagingSchemeList(
        currentPage: Int,
        pageSize: Int
    ): PagingSchemeListUseCase.Response {

        Log.i(tag, "In $tag getPaginatedSchemeList")
        val response = PagingSchemeListUseCase.Response()
        response.setResponseCode(ResponseCode.Success)

        val pager = Pager(
            config = PagingConfig(pageSize = pageSize),
            pagingSourceFactory = { RoomPagingSource(localDataSource) }
        )
        response.setData(pager.flow)
        return response
    }

    override suspend fun getPaginatedSchemeList(
        currentPage: Int,
        pageSize: Int
    ): PaginatedSchemeListUseCase.Response {

        Log.i(tag, "In $tag getPaginatedSchemeList")
        val response = PaginatedSchemeListUseCase.Response()
        response.setResponseCode(ResponseCode.Success)
        val localOutput = localDataSource.getPaginatedSchemeList(currentPage = currentPage, pageSize = pageSize)
        val responseData = if (localOutput is BaseOutput.Success) {
            localOutput.output!!
        } else {
            ArrayList()
        }
        response.setData(responseData)
        return response

    }
}