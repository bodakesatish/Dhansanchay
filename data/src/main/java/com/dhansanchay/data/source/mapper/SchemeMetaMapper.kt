package com.dhansanchay.data.source.mapper

import com.dhansanchay.data.source.local.entity.SchemeMetaEntity
import com.dhansanchay.data.source.remote.model.SchemeDetailApiResponse
import com.dhansanchay.domain.model.SchemeMetaModel

object SchemeMetaMapper : RemoteMapper<SchemeDetailApiResponse, SchemeMetaEntity, SchemeMetaModel> {

    override fun SchemeDetailApiResponse.mapToData(): SchemeMetaEntity {
        return SchemeMetaEntity(
            schemeCode = meta.schemeCode,
            schemeName = meta.schemeName,
            fundHouse = meta.fundHouse,
            isInGrowth = meta.isinGrowth ?: "",
            schemeCategory = meta.schemeCategory,
            schemeType = meta.schemeType
        )
    }

    override fun SchemeMetaEntity.mapToDomain(): SchemeMetaModel {
        return SchemeMetaModel(
            schemeCode = schemeCode,
            schemeName = schemeName,
            fundHouse = fundHouse,
            isInGrowth = isInGrowth,
            schemeCategory = schemeCategory,
            schemeType = schemeType
        )
    }

    fun List<SchemeMetaEntity>.toDomainModelList(): List<SchemeMetaModel> {
        return this.map { it.mapToDomain() } // Calls the extension within the object's scope
    }

    fun List<SchemeDetailApiResponse>.toEntityModelList(): List<SchemeMetaEntity> {
        return this.map { it.mapToData() }
    }
}














