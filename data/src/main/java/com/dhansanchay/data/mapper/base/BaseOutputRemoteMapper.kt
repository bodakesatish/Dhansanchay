package com.dhansanchay.data.mapper.base

import com.dhansanchay.data.source.base.ApiResponseCode
import com.dhansanchay.data.source.base.BaseOutput
import com.dhansanchay.domain.model.ResponseCode
import com.dhansanchay.domain.usecases.base.BaseUseCase

class BaseOutputRemoteMapper<T : Any> {

//    fun mapBaseOutput(output: BaseOutput<T>, response: BaseUseCase.Response<T>) {
//        when (output) {
//            is BaseOutput.Success -> {
//                when (output.code) {
//                    ApiResponseCode.SUCCESS -> {
//                        mapSuccessOutput(output, response)
//                    }
//                    ApiResponseCode.EMPTY -> {
//                        mapEmptyOutput(output, response)
//                    }
//                }
//            }
//
//            is BaseOutput.Error -> {
//                mapErrorOutput(output, response)
//            }
//        }
//    }

//    suspend fun mapBaseOutput(
//        output: BaseOutput<T>,
//        response: BaseUseCase.Response<T>,
//        executeOnSuccess: suspend (T) -> T
//    ) {
//        when (output) {
//            is BaseOutput.Success -> {
//                when (output.code) {
//                    ApiResponseCode.SUCCESS -> {
//                        val data = executeOnSuccess.invoke(output.output!!)
//                        mapSuccessOutput(data, response)
//                    }
//                    ApiResponseCode.EMPTY -> {
//                        mapEmptyOutput(output, response)
//                    }
//                }
//            }
//            is BaseOutput.Error -> {
//                mapErrorOutput(output, response)
//            }
//        }
//    }

    suspend fun mapBaseOutput(
        output: BaseOutput<T>,
        response: BaseUseCase.Response<T>,
        executeOnSuccess: suspend (T) -> T,
        executeOnError: suspend () -> T
    ) {
        when (output) {
            is BaseOutput.Success -> {
                when (output.code) {
                    ApiResponseCode.SUCCESS -> {
                        val data = executeOnSuccess.invoke(output.output!!)
                        mapSuccessOutput(data, response)
                    }
                    ApiResponseCode.EMPTY -> {
                        mapEmptyOutput(output, response)
                    }
                }
            }
            is BaseOutput.Error -> {
                mapErrorOutput(output, response, executeOnError)
            }
        }
    }

    suspend fun mapBaseOutput(
        output: BaseOutput<T>,
        response: BaseUseCase.Response<T>,
        executeOnSuccess: suspend (T) -> T,
        executeOnEmpty: suspend () -> T,
        executeOnError: suspend () -> T
    ) {
        when (output) {
            is BaseOutput.Success -> {
                when (output.code) {
                    ApiResponseCode.SUCCESS -> {
                        val data = executeOnSuccess.invoke(output.output!!)
                        mapSuccessOutput(data, response)
                    }
                    ApiResponseCode.EMPTY -> {
                        val data = executeOnEmpty.invoke()
                        if(data is List<*> && data.size > 0) {
                            mapSuccessOutput(data, response)
                        } else {
                            mapEmptyOutput(data, response)
                        }

                    }
                }
            }
            is BaseOutput.Error -> {
                mapErrorOutput(output, response, executeOnError)
            }
        }
    }


    private fun mapSuccessOutput(output: BaseOutput.Success<T>, response: BaseUseCase.Response<T>) {
            response.setData(output.output)
            response.setResponseCode(ResponseCode.Success)
    }

    private fun mapSuccessOutput(data: T, response: BaseUseCase.Response<T>) {
        response.setData(data)
        response.setResponseCode(ResponseCode.Success)
    }

    private fun mapEmptyOutput(output: BaseOutput.Success<T>, response: BaseUseCase.Response<T>) {
            response.setData(output.output)
            response.setResponseCode(ResponseCode.Empty)
    }

    private fun mapEmptyOutput(data : T , response: BaseUseCase.Response<T>) {
        response.setData(data)
        response.setResponseCode(ResponseCode.Empty)
    }


    private fun mapErrorOutput(output: BaseOutput.Error, response: BaseUseCase.Response<T>) {
        when (output.code) {
            ApiResponseCode.NETWORK_ERROR -> {
                response.setResponseCode(ResponseCode.Network)
            }
            ApiResponseCode.AUTHENTICATION_FAILED -> {
                response.setResponseCode(ResponseCode.Authentication)
            }
            else -> {
                response.setResponseCode(ResponseCode.Fail)
            }
        }
    }

    private suspend fun mapErrorOutput(
        output: BaseOutput.Error,
        response: BaseUseCase.Response<T>,
        executeOnError: suspend () -> T
    ) {
        val data = executeOnError.invoke()
        response.setData(data)
        when (output.code) {
            ApiResponseCode.NETWORK_ERROR -> {
                response.setResponseCode(ResponseCode.Network)
            }
            ApiResponseCode.AUTHENTICATION_FAILED -> {
                response.setResponseCode(ResponseCode.Authentication)
            }
            ApiResponseCode.UNKNOWN_ERROR -> {
                response.setResponseCode(ResponseCode.Fail)
            }
            else -> {
                response.setResponseCode(ResponseCode.Fail)
            }
        }
    }

}