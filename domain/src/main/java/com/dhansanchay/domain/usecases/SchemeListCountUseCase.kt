package com.dhansanchay.domain.usecases

import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.usecases.base.BaseUseCase
import javax.inject.Inject

class SchemeListCountUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository
) :
BaseUseCase<SchemeListCountUseCase.Request, SchemeListCountUseCase.Response, Any, Int>(){


    override suspend fun buildUseCase(request: Request): Response {
        return schemeRepository.getSchemeList()
    }

    class Request : BaseUseCase.Request<Any>()

    class Response : BaseUseCase.Response<Int>()

}