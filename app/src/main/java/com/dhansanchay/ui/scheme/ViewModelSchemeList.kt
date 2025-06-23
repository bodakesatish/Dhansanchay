package com.dhansanchay.ui.scheme

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.usecases.ObserveSchemeListUseCase
import com.dhansanchay.domain.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher // For injection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
// import kotlinx.coroutines.flow.onStart // Alternative to initialValue for schemeNetworkResult
import kotlinx.coroutines.launch
import javax.inject.Inject // Assuming you have a DispatchersModule for Hilt
// import com.yourpackage.IoDispatcher // Assuming a qualifier for your dispatcher

data class SchemeListUiState(
    val schemes: List<SchemeModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ViewModelSchemeList @Inject constructor(
    private val observeSchemeListUseCase: ObserveSchemeListUseCase
    // Example: @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val tag = this.javaClass.simpleName

    private val refreshTrigger = MutableSharedFlow<Boolean>(replay = 1).apply {
        tryEmit(false) // Initial, non-forced load trigger
        Log.d(tag, "refreshTrigger initialized and initial value emitted (false).")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val schemeNetworkResult: StateFlow<NetworkResult<List<SchemeModel>>> =
        refreshTrigger
            .flatMapLatest { isForced ->
                Log.d(tag, "flatMapLatest triggered. isForced: $isForced")
                observeSchemeListUseCase(isForceRefresh = isForced)
                // Alternative to initialValue in stateIn:
                // .onStart { emit(NetworkResult.Loading) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = NetworkResult.Loading // Start with loading
            )

    val uiState: StateFlow<SchemeListUiState> = schemeNetworkResult
        .map { result ->
            // To preserve previous data, you need the *current* uiState.
            // This relies on uiState.value providing the previously emitted state.
            val currentSchemes = uiState.value.schemes

            when (result) {
                is NetworkResult.Success -> {
                    SchemeListUiState(
                        schemes = result.data ?: emptyList(),
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is NetworkResult.Loading -> {
                    SchemeListUiState(
                        schemes = currentSchemes, // Preserve previous schemes
                        isLoading = true,
                        errorMessage = null // Clear previous error
                    )
                }
                is NetworkResult.Error -> {
                    SchemeListUiState(
                        schemes = currentSchemes, // Preserve previous schemes
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            // Initial UI state reflects that data is being loaded.
            // Schemes list is empty initially.
            initialValue = SchemeListUiState(isLoading = true, schemes = emptyList(), errorMessage = null)
        )

    /**
     * Triggers a data refresh, potentially fetching new data from the source.
     */
    fun forceRefreshSchemes() {
        Log.d(tag, "ViewModel forceRefreshSchemes called")
        viewModelScope.launch { // Consider launching with injected ioDispatcher if use case is I/O bound
            refreshTrigger.emit(true)
        }
    }

    /**
     * Triggers a data load or refresh, potentially using cached data if available
     * and not forced.
     */
    fun refreshSchemes() { // Renamed from refreshSchemesIfNotForced for clarity
        Log.d(tag, "ViewModel refreshSchemes (non-forced) called")
        viewModelScope.launch { // Consider launching with injected ioDispatcher
            refreshTrigger.emit(false)
        }
    }

    // The old `refreshSchemes()` placeholder function with Log.w should be removed.
}