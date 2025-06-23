package com.dhansanchay.data.source.remote

import com.dhansanchay.data.source.remote.model.SchemeApiResponse
import com.dhansanchay.data.source.remote.model.SchemeDetailApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MutualFundApiService {
    /**
     * Fetches a list of official mutual fund schemes.
     * API Response example: [{"schemeCode":100027,"schemeName":"Grindlays Super Saver Income Fund-GSSIF-Half Yearly Dividend","isinGrowth":null,"isinDivReinvestment":null}]
     */
    @GET("mf") // Replace with your actual API endpoint
    suspend fun getOfficialMutualFunds(): Response<List<SchemeApiResponse>>

    /**
     * Fetches detailed information for a specific mutual fund scheme, including NAV history.
     * API Response example: {"meta":{...},"data":[{"date":"17-05-2025","nav":"10.00000"}],"status":"SUCCESS"}
     */
    @GET("detail") // Replace with your actual API endpoint
    suspend fun getMutualFundDetail(@Query("schemeCode") schemeCode: Int): Response<SchemeDetailApiResponse>
}