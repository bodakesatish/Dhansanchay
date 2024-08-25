package com.dhansanchay.data.mapper.local

import com.dhansanchay.data.mapper.base.BaseMapper
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.domain.model.response.SchemeModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemeLocalMapper
@Inject constructor() : BaseMapper<SchemeModel, SchemeEntity>(){

    override fun map(entity: SchemeEntity): SchemeModel {
        return SchemeModel(
            entity.id,
            entity.schemeCode,
            entity.schemeName
        )
    }

    override fun reverse(model: SchemeModel): SchemeEntity {
        return SchemeEntity(
            0,
            model.schemeCode,
            model.schemeName
        )
    }
}