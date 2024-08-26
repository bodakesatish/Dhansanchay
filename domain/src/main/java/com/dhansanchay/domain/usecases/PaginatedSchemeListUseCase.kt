package com.dhansanchay.domain.usecases

import com.dhansanchay.domain.model.request.PageRequest
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.usecases.base.BaseUseCase
import javax.inject.Inject

class PaginatedSchemeListUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository
) :
BaseUseCase<PaginatedSchemeListUseCase.Request, PaginatedSchemeListUseCase.Response, PageRequest, List<SchemeModel>>(){


    override suspend fun buildUseCase(request: Request): Response {
        return schemeRepository.getPaginatedSchemeList(request.getRequestModel().currentPage, request.getRequestModel().pageSize)
    }

    class Request : BaseUseCase.Request<PageRequest>()

    class Response : BaseUseCase.Response<List<SchemeModel>>()

}