package com.dhansanchay.data.source.remote.model

import com.google.gson.annotations.SerializedName

data class SchemeApiResponse(
    @SerializedName("schemeCode") val schemeCode: Int,
    @SerializedName("schemeName") val schemeName: String
)
