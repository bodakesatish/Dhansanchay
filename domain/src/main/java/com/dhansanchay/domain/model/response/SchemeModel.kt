package com.dhansanchay.domain.model.response

import com.dhansanchay.domain.model.base.BaseResponse

data class SchemeModel(
    val id: Int,
    val schemeCode: String,
    val schemeName: String
) : BaseResponse