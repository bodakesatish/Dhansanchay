package com.dhansanchay.data.source.remote.source.base

import com.dhansanchay.data.source.base.ApiResponseCode
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.domain.model.base.BaseRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

abstract class BaseDataSourceRemote<P : BaseRequest> {

    suspend fun <T: Any> sendRequest(call: suspend () -> Response<T>) : Response<T> {
        return try {
            call.invoke()
        } catch (t : Throwable) {
            t.printStackTrace()
            return if(t is IOException && t.localizedMessage.equals("NetworkException", ignoreCase = true)) {
                Response.error<T>(503, getDummyResponseBody())
            } else  {
                Response.error<T>(500, getDummyResponseBody())
            }
        }
    }

    suspend fun <T : Any, Q : Any> getOutput(response : Response<T>, mapper : suspend() -> Q) : BaseOutput<Q> {
        return when {
            isValidResponse(response) -> BaseOutput.Success(ApiResponseCode.SUCCESS, mapper.invoke())
            isEmptyResponse(response) -> BaseOutput.Success(ApiResponseCode.EMPTY, null)
            isNetworkError(response) -> BaseOutput.Error(ApiResponseCode.NETWORK_ERROR)
            else -> BaseOutput.Error(ApiResponseCode.UNKNOWN_ERROR)
        }
    }

    private fun getDummyResponseBody() : ResponseBody{
        return "".toResponseBody("application/json".toMediaTypeOrNull())
    }

    private fun<T> isValidResponse(response: Response<T>) : Boolean {
        return response.isSuccessful && response.code() == 200 && response.body() != null
    }

    private fun<T> isEmptyResponse(response: Response<T>) : Boolean {
        return response.code() == 204
    }

    private fun <T> isNetworkError(response: Response<T>): Boolean {
        return response.code() == 503
    }
}