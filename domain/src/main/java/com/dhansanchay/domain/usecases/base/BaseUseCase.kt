package com.dhansanchay.domain.usecases.base

import androidx.annotation.WorkerThread
import com.dhansanchay.domain.model.ResponseCode

abstract class BaseUseCase<P : BaseUseCase.Request<R>, Q : BaseUseCase.Response<S>, R : Any, S : Any> {

    open class Request<R> {
        private var requestModel: R? = null

        fun setRequestModel(requestModel: R) {
            this.requestModel = requestModel
        }

        fun getRequestModel(): R {
            return requestModel!!
        }
    }

    open class Response<S> {
        private lateinit var responseCode : ResponseCode
        fun getResponseCode()  = responseCode
        fun setResponseCode(responseCode: ResponseCode) {
            this.responseCode = responseCode
        }

        private var data: S? = null

        fun  getData(): S? {
            return data
        }

        fun setData(data: S?) {
            this.data = data
        }
    }

    suspend fun executeUseCase(request: P): Q {
        return buildUseCase(request)
    }

    @WorkerThread
    protected abstract suspend fun buildUseCase(request: P): Q
}