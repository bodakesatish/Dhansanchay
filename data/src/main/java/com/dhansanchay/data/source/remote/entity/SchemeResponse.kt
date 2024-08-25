package com.dhansanchay.data.source.remote.entity

import com.dhansanchay.data.source.base.BaseEntity

data class SchemeResponse(
    val schemeCode: String,
    val schemeName: String
) : BaseEntity()