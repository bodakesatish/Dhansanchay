package com.dhansanchay.domain.repository

import com.dhansanchay.domain.usecases.SchemeListUseCase

interface SchemeRepository {
    suspend fun getSchemeList() : SchemeListUseCase.Response
}