package com.dhansanchay.domain.usecases

import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.repository.SchemeRepository
import com.dhansanchay.domain.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveSchemeListUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository
)  {
    operator fun invoke(isForceRefresh: Boolean = false): Flow<NetworkResult<List<SchemeModel>>> = schemeRepository.getSchemeListObservable(isForceRefresh)
}