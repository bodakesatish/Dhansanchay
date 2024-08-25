package com.dhansanchay.data.mapper.remote

import com.dhansanchay.data.mapper.base.BaseMapper
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.remote.entity.SchemeResponse
import com.dhansanchay.domain.model.response.SchemeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemeRemoteMapper
@Inject constructor() : BaseMapper<SchemeModel, SchemeResponse>(){

    override fun map(entity: SchemeResponse): SchemeModel {
        return SchemeModel(
            0,
            entity.schemeCode,
            entity.schemeName
        )
    }

}