package com.dhansanchay.data.repository

import android.util.Log
import com.dhansanchay.data.mapper.base.BaseOutputRemoteMapper
import com.dhansanchay.data.security.prefs.SessionConstants
import com.dhansanchay.data.security.prefs.SessionManager
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.data.source.local.source.SchemeDataSourceLocal
import com.dhansanchay.data.source.remote.source.SchemeDataSourceRemote
import com.dhansanchay.domain.model.ResponseCode
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.usecases.SchemeListUseCase
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


    override suspend fun getSchemeList(): SchemeListUseCase.Response {
        Log.i(tag, "In $tag getSchemeList")
        val response = SchemeListUseCase.Response()

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
            val localOutput = localDataSource.getSchemeList()
            val responseData = if (localOutput is BaseOutput.Success) {
                localOutput.output!!
            } else {
                ArrayList()
            }
            response.setData(responseData)
            return response
        }

        val remoteOutput = remoteDataSource.getSchemeList()
        val baseOutputMapper = BaseOutputRemoteMapper<List<SchemeModel>>()

        baseOutputMapper.mapBaseOutput(remoteOutput, response,
            executeOnSuccess = { remoteSchemeList ->
                localDataSource.deleteAll()
                localDataSource.insertAll(remoteSchemeList)
                val localOutput = localDataSource.getSchemeList()
                sessionManager.set(SessionConstants.SESSION_LAST_SYNC_DATE,Date().time)
                if (localOutput is BaseOutput.Success) {
                    return@mapBaseOutput localOutput.output!!
                } else {
                    return@mapBaseOutput ArrayList()
                }
            },
            executeOnError = {
                val localOutput = localDataSource.getSchemeList()
                if (localOutput is BaseOutput.Success) {
                    return@mapBaseOutput localOutput.output!!
                } else {
                    return@mapBaseOutput ArrayList()
                }
            })
        return response
    }
}