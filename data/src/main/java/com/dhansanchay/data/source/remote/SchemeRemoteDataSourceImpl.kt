package com.dhansanchay.data.source.remote

import android.util.Log
import com.dhansanchay.data.source.remote.model.SchemeApiResponse
import com.dhansanchay.data.source.remote.model.SchemeDetailApiResponse
import com.dhansanchay.domain.utils.NetworkResult
import com.dhansanchay.domain.model.SchemeModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


interface SchemeRemoteDataSource {
    suspend fun fetchSchemeList(): NetworkResult<List<SchemeApiResponse>>
    suspend fun fetchSchemeDetail(schemeCode: Int): NetworkResult<SchemeDetailApiResponse>
}

@Singleton // Make the implementation a Singleton if it's stateless and expensive to create
class SchemeRemoteDataSourceImpl @Inject constructor(
    private val apiService: MutualFundApiService,
) : SchemeRemoteDataSource {

    private val tag = this.javaClass.simpleName
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Inject dispatcher

    override suspend fun fetchSchemeList(): NetworkResult<List<SchemeApiResponse>> {
        Log.d(tag, "Fetching scheme list from remote")
        return safeApiCall { apiService.getOfficialMutualFunds() }
    }

    override suspend fun fetchSchemeDetail(schemeCode: Int): NetworkResult<SchemeDetailApiResponse> {
        Log.d(tag, "Fetching scheme detail for code: $schemeCode from remote")
        return safeApiCall { apiService.getMutualFundDetail(schemeCode) }
    }

    private fun handleResponse(response: Response<SchemeModel>, message: String) {
        if (response.isSuccessful && response.body() != null) {
            NetworkResult.Success(Pair(true, message))
        } else {
            NetworkResult.Success(Pair(false, "Something went wrong"))
        }
    }

    // Helper function to make API calls safer and reduce boilerplate
    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
        return withContext(ioDispatcher) { // Ensure API call is on IO dispatcher
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    response.body()?.let {
                        NetworkResult.Success(it)
                    } ?: NetworkResult.Error("API response body is null")
                } else {
                    val errorBody = response.errorBody()?.charStream()?.readText()
                    val errorMessage = if (!errorBody.isNullOrEmpty()) {
                        try {
                            JSONObject(errorBody).getString("message")
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to parse error body JSON: $errorBody", e)
                            "Error ${response.code()}: ${response.message()} - Invalid error format"
                        }
                    } else {
                        "Error ${response.code()}: ${response.message()}"
                    }
                    Log.w(tag, "API Error: $errorMessage, Code: ${response.code()}")
                    NetworkResult.Error(errorMessage)
                }
            } catch (e: IOException) { // For network connectivity issues
                Log.e(tag, "Network Error", e)
                NetworkResult.Error("Network Error: ${e.localizedMessage}", e)
            } catch (e: Exception) { // For other unexpected errors (e.g., JSON parsing within Retrofit)
                Log.e(tag, "Unexpected error during API call", e)
                NetworkResult.Error("An unexpected error occurred: ${e.localizedMessage}", e)
            }
        }
    }
}