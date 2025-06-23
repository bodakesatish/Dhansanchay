package com.dhansanchay.domain.usecases

import com.dhansanchay.domain.model.SchemeMetaModel
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSchemeMetaUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository
)  {
    operator fun invoke(schemeCode : Int, forceRefresh: Boolean = false): Flow<NetworkResult<SchemeMetaModel>> = schemeRepository.getSchemeDetailNavLatest(schemeCode = schemeCode, forceRefresh)
}