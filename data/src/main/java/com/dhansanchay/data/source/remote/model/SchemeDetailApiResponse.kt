package com.dhansanchay.data.source.remote.model

import com.google.gson.annotations.SerializedName

data class SchemeDetailApiResponse(
    @SerializedName("meta") val meta: MetaData,
    @SerializedName("data") val data: List<NavDataResponse>,
    @SerializedName("status") val status: String
) {
    data class MetaData(
        @SerializedName("fund_house") val fundHouse: String,
        @SerializedName("scheme_type") val schemeType: String,
        @SerializedName("scheme_category") val schemeCategory: String,
        @SerializedName("scheme_code") val schemeCode: Int,
        @SerializedName("scheme_name") val schemeName: String,
        @SerializedName("isin_growth") val isinGrowth: String?,
        @SerializedName("isin_div_reinvestment") val isinDivReinvestment: String?
    )

    data class NavDataResponse(
        @SerializedName("date") val date: String,
        @SerializedName("nav") val nav: String // Keep as String to handle parsing errors
    )
}