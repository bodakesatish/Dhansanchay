package com.dhansanchay.data.repository

import android.util.Log
import com.dhansanchay.data.mapper.base.BaseOutputRemoteMapper
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.data.source.local.source.SchemeDataSourceLocal
import com.dhansanchay.data.source.remote.source.SchemeDataSourceRemote
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.usecases.SchemeListUseCase
import javax.inject.Inject

class SchemeRepositoryImpl
@Inject
constructor(
    private val remoteDataSource: SchemeDataSourceRemote,
    private val localDataSource: SchemeDataSourceLocal
) : SchemeRepository {

    private val tag = this.javaClass.simpleName

    override suspend fun getSchemeList(): SchemeListUseCase.Response {
        Log.i(tag, "In $tag getSchemeList")
        val remoteOutput = remoteDataSource.getSchemeList()
        val response = SchemeListUseCase.Response()
        val baseOutputMapper = BaseOutputRemoteMapper<List<SchemeModel>>()

        baseOutputMapper.mapBaseOutput(remoteOutput, response,
            executeOnSuccess = { remoteSchemeList ->
                localDataSource.deleteAll()
                localDataSource.insertAll(remoteSchemeList)
                val localOutput = localDataSource.getSchemeList()
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