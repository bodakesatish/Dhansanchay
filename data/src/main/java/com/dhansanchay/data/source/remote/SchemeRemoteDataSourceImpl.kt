package com.dhansanchay.data.source.remote

import android.util.Log
import com.dhansanchay.data.source.remote.di.IoDispatcher
import com.dhansanchay.data.source.remote.model.SchemeApiResponse
import com.dhansanchay.data.source.remote.model.SchemeDetailApiResponse
import com.dhansanchay.domain.utils.NetworkResult // Assuming NetworkResult.Error can take a code
// import com.yourpackage.IoDispatcher // If using Hilt qualifiers
import kotlinx.coroutines.CoroutineDispatcher
// import kotlinx.coroutines.Dispatchers // No longer needed here if injected
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Assume your NetworkResult.Error is defined something like:
// sealed class NetworkResult<out T> {
//     data class Success<out T>(val data: T) : NetworkResult<T>()
//     data class Error(val message: String, val code: Int? = null, val exception: Throwable? = null) : NetworkResult<Nothing>()
//     object Loading : NetworkResult<Nothing>() // If you use it from data source
// }


interface SchemeRemoteDataSource {
    suspend fun fetchSchemeList(): NetworkResult<List<SchemeApiResponse>>
    suspend fun fetchSchemeDetail(schemeCode: Int): NetworkResult<SchemeDetailApiResponse>
}

@Singleton
class SchemeRemoteDataSourceImpl @Inject constructor(
    private val apiService: MutualFundApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher // Inject dispatcher
) : SchemeRemoteDataSource {

    private val tag = SchemeRemoteDataSourceImpl::class.java.simpleName

    override suspend fun fetchSchemeList(): NetworkResult<List<SchemeApiResponse>> {
        Log.d(tag, "Fetching scheme list from remote")
        return safeApiCall { apiService.getOfficialMutualFunds() }
    }

    override suspend fun fetchSchemeDetail(schemeCode: Int): NetworkResult<SchemeDetailApiResponse> {
        Log.d(tag, "Fetching scheme detail for code: $schemeCode from remote")
        return safeApiCall { apiService.getMutualFundDetail(schemeCode) }
    }

    // This function is currently unused. Consider removing or integrating it.
    // private fun handleResponse(response: Response<SchemeModel>, message: String) {
    //     if (response.isSuccessful && response.body() != null) {
    //         NetworkResult.Success(Pair(true, message))
    //     } else {
    //         NetworkResult.Success(Pair(false, "Something went wrong"))
    //     }
    // }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
        return withContext(ioDispatcher) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        NetworkResult.Success(body)
                    } ?: NetworkResult.Error(
                        message = "API response body is null for successful request code: ${response.code()}",
                        code = response.code() // Or a specific code for this case
                    )
                } else {
                    val errorBody = response.errorBody()?.charStream()?.readText()
                    val errorMessage = if (!errorBody.isNullOrEmpty()) {
                        try {
                            JSONObject(errorBody).getString("message")
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to parse error body JSON: $errorBody", e)
                            // Fallback error message if parsing fails
                            "Error ${response.code()}: ${response.message()} (Raw: $errorBody)"
                        }
                    } else {
                        "Error ${response.code()}: ${response.message()}"
                    }
                    Log.w(tag, "API Error: $errorMessage, Code: ${response.code()}")
                    NetworkResult.Error(message = errorMessage, code = response.code())
                }
            } catch (e: IOException) { // For network connectivity issues (no HTTP response)
                Log.e(tag, "Network Error: ${e.message}", e)
                NetworkResult.Error(
                    message = "Network Error: ${e.localizedMessage ?: "Could not connect to server."}",
                    code = null, // Or a specific code like -1 for network errors
                    exception = e
                )
            } catch (e: Exception) { // For other unexpected errors (e.g., JSON parsing within Retrofit for SUCCESS, etc.)
                Log.e(tag, "Unexpected error during API call: ${e.message}", e)
                NetworkResult.Error(
                    message = "An unexpected error occurred: ${e.localizedMessage ?: "Unknown error."}",
                    code = null, // Or a specific code for client-side unexpected errors
                    exception = e
                )
            }
        }
    }
}