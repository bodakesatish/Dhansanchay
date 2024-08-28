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
        return BaseOutput.Success(ApiResponseCode.SUCCESS, schemeLocalMapper.map(data))
    }

    override suspend fun getSchemeListCount(): BaseOutput<Int> {
        val data = schemeDao.getSchemeListCount()
        return BaseOutput.Success(ApiResponseCode.SUCCESS, data)
    }

    override suspend fun getPaginatedSchemeList(
        currentPage: Int,
        pageSize: Int
    ): BaseOutput<List<SchemeModel>> {
        val offset = currentPage * pageSize
        val data = schemeDao.getPaginatedSchemeList(limit = pageSize, offset = offset)
        return BaseOutput.Success(ApiResponseCode.SUCCESS,schemeLocalMapper.map(data))
    }

    suspend fun deleteAll() {
        schemeDao.deleteSchemeList()
    }

    suspend fun insertAll(schemeList: List<SchemeModel>) {
        val data = schemeLocalMapper.reverse(schemeList)
        schemeDao.insertSchemeList(data)
    }

    suspend fun getPagingSchemeList(
        pageSize : Int,
        offset : Int
    ): BaseOutput<List<SchemeModel>> {
        val data = schemeDao.getPaginatedSchemeList(limit = pageSize, offset = offset)
        return BaseOutput.Success(ApiResponseCode.SUCCESS,schemeLocalMapper.map(data))
    }

}