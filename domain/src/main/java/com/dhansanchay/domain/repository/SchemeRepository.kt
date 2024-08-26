package com.dhansanchay.domain.repository

import com.dhansanchay.domain.usecases.PaginatedSchemeListUseCase
import com.dhansanchay.domain.usecases.SchemeListCountUseCase

interface SchemeRepository {
    suspend fun getSchemeList() : SchemeListCountUseCase.Response
    suspend fun getPaginatedSchemeList(currentPage: Int, pageSize: Int): PaginatedSchemeListUseCase.Response
}