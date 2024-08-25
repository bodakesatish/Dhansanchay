package com.dhansanchay.data.source.remote.source

import com.dhansanchay.data.mapper.remote.SchemeRemoteMapper
import com.dhansanchay.data.source.DataSource
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.data.source.remote.source.base.BaseDataSourceRemote
import com.dhansanchay.domain.model.base.BaseRequest
import com.dhansanchay.domain.model.response.SchemeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemeDataSourceRemote
@Inject
constructor(
    private val apiService: ApiService,
    private val schemeRemoteMapper: SchemeRemoteMapper
) : BaseDataSourceRemote<BaseRequest>(),
DataSource.SchemeSource {

    override suspend fun getSchemeList(): BaseOutput<List<SchemeModel>> {
        val response = sendRequest { apiService.schemeList() }
        return getOutput(response) { schemeRemoteMapper.map(response.body()!!) }
    }


}