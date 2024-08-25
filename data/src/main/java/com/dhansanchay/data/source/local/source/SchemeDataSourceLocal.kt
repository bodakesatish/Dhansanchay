package com.dhansanchay.data.source.local.source

import com.dhansanchay.data.mapper.local.SchemeLocalMapper
import com.dhansanchay.data.source.DataSource
import com.dhansanchay.data.source.base.ApiResponseCode
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.data.source.local.dao.SchemeDao
import com.dhansanchay.domain.model.response.SchemeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemeDataSourceLocal
@Inject
constructor(
    private val schemeDao: SchemeDao,
    private val schemeLocalMapper: SchemeLocalMapper
) : DataSource.SchemeSource {

    override suspend fun getSchemeList(): BaseOutput<List<SchemeModel>> {
        val data = schemeDao.getSchemeList()
        return BaseOutput.Success(ApiResponseCode.SUCCESS,schemeLocalMapper.map(data))
    }

   suspend fun deleteAll() {
        schemeDao.deleteSchemeList()
    }

    suspend fun insertAll(schemeList: List<SchemeModel>) {
        val data = schemeLocalMapper.reverse(schemeList)
        schemeDao.insertSchemeList(data)
    }

}