package com.dhansanchay.domain.usecases

import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.utils.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSchemeListUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository
)  {
    suspend operator fun invoke(): NetworkResult<List<SchemeModel>> = schemeRepository.getSchemeListOnce()
}