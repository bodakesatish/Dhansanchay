package com.dhansanchay.domain.usecases

import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.usecases.base.BaseUseCase
import javax.inject.Inject

class SchemeListUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository
) :
BaseUseCase<SchemeListUseCase.Request, SchemeListUseCase.Response, Any, List<SchemeModel>>(){


    override suspend fun buildUseCase(request: Request): Response {
        return schemeRepository.getSchemeList()
    }

    class Request : BaseUseCase.Request<Any>()

    class Response : BaseUseCase.Response<List<SchemeModel>>()

}