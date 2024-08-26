package com.dhansanchay.data.source

import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.data.source.remote.entity.SchemeResponse
import com.dhansanchay.domain.model.response.SchemeModel

interface DataSource {

    interface SchemeSource {
        suspend fun getSchemeList(): BaseOutput<List<SchemeModel>>
        suspend fun getSchemeListCount() : BaseOutput<Int>
        suspend fun getPaginatedSchemeList(currentPage: Int, pageSize: Int): BaseOutput<List<SchemeModel>>
    }
}