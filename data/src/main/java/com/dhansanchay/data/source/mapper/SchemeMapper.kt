package com.dhansanchay.data.source.mapper

import com.dhansanchay.data.source.local.entity.DetailFetchStatus
import com.dhansanchay.data.source.local.entity.SchemeEntity
import com.dhansanchay.data.source.local.entity.SchemeMetaEntity
import com.dhansanchay.data.source.remote.model.SchemeApiResponse
import com.dhansanchay.domain.model.SchemeModel

object SchemeMapper : RemoteMapper<SchemeApiResponse, SchemeEntity, SchemeModel> {

    override fun SchemeApiResponse.mapToData(): SchemeEntity {
        return SchemeEntity(
            schemeCode = schemeCode,
            schemeName = schemeName
        )
    }

    override fun SchemeEntity.mapToDomain(): SchemeModel {
        return SchemeModel(
            schemeCode = schemeCode,
            schemeName = schemeName
        )
    }

    fun List<SchemeEntity>.toDomainModelList(): List<SchemeModel> {
        return this.map { it.mapToDomain() } // Calls the extension within the object's scope
    }

    fun List<SchemeApiResponse>.toEntityModelList(): List<SchemeEntity> {
        return this.map { it.mapToData() }
    }

    fun List<SchemeEntity>.toSchemeMetaEntityList(): List<SchemeMetaEntity> {
        return this.map {
            SchemeMetaEntity(
                schemeCode = it.schemeCode,
                schemeName = it.schemeName,
                "",
                "",
                "",
                "",
                DetailFetchStatus.PENDING.name,
                0L,
                0,
                null
            )
        }
    }
}














