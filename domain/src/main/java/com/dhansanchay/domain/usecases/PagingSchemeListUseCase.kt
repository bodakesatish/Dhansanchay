package com.dhansanchay.domain.usecases

import androidx.paging.Pager
import androidx.paging.PagingData
import com.dhansanchay.domain.model.request.PageRequest
import com.dhansanchay.domain.model.response.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.usecases.base.BaseUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PagingSchemeListUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository
) :
BaseUseCase<PagingSchemeListUseCase.Request, PagingSchemeListUseCase.Response, PageRequest,Flow<PagingData<SchemeModel>>>(){


    override suspend fun buildUseCase(request: Request): Response {
        return schemeRepository.getPagingSchemeList(1, 20)
    }

    class Request : BaseUseCase.Request<PageRequest>()

    class Response : BaseUseCase.Response<Flow<PagingData<SchemeModel>>>()

}