package com.dhansanchay.data.source.remote.source

import com.dhansanchay.data.source.remote.entity.SchemeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("mf")
    suspend fun schemeList() : Response<List<SchemeResponse>>

    @GET("mf/search")
    suspend fun searchSchemeList(@Query("q") query: String) : Response<List<SchemeResponse>>

}