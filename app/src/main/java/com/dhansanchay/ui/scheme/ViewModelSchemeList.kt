package com.dhansanchay.ui.scheme

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.usecases.GetSchemeListUseCase
import com.dhansanchay.domain.usecases.ObserveSchemeListUseCase
import com.dhansanchay.domain.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchemeListUiState(
    val schemes: List<SchemeModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ViewModelSchemeList @Inject constructor(
    private val observeSchemeListUseCase: ObserveSchemeListUseCase
    // Inject dispatchers if needed for testing:
    // private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val tag = this.javaClass.simpleName


    // A more robust way often involves a trigger that the main data flow observes.
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 0) // Or MutableStateFlow for some scenarios


    // The "master" StateFlow directly reflecting the NetworkResult from the use case.
    // This can remain private if only used to build the public uiState.
    private val schemeNetworkResult: StateFlow<NetworkResult<List<SchemeModel>>> =
        observeSchemeListUseCase() // This returns Flow<NetworkResult<List<SchemeModel>>>
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = NetworkResult.Loading // Assuming NetworkResult.Loading() is valid
            )

    // Single public StateFlow for the entire UI state
    val uiState: StateFlow<SchemeListUiState> = schemeNetworkResult
        .map { result ->
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
                        // You might want to keep displaying stale data while loading new data
                        // schemes = uiState.value.schemes, // To keep previous list during load
                        isLoading = true,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    SchemeListUiState(
                        // You might want to keep displaying stale data on error
                        // schemes = uiState.value.schemes, // To keep previous list on error
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                // Handle any other NetworkResult subtypes if they exist
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SchemeListUiState(isLoading = true) // Initial UI state
        )

    /**
    // Public StateFlows derived from schemeNetworkResult
    val schemeList: StateFlow<List<SchemeModel>> = schemeNetworkResult
        .map { result ->
            if (result is NetworkResult.Success) {
                result.data ?: emptyList() // Handle nullable data from NetworkResult.Success
            } else {
                emptyList() // Or return previous list if you want to keep stale data on error/loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val isLoading: StateFlow<Boolean> = schemeNetworkResult
        .map { result ->
            result is NetworkResult.Loading
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            // Set initial based on the initialValue of schemeNetworkResult
            initialValue = schemeNetworkResult.value is NetworkResult.Loading
        )

    val error: StateFlow<String?> = schemeNetworkResult
        .map { result ->
            if (result is NetworkResult.Error) {
                result.message // Your NetworkResult.Error should have a message field
            } else {
                null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )
**/

    // In ViewModelSchemeList.kt

// Assume ObserveSchemeListUseCase can be triggered or re-invoked
// or you have a separate use case for refreshing.

// A simple way (if observeSchemeListUseCase() is a cold flow and stateIn will re-collect):
// This is often NOT how stateIn works for refresh by default; it shares the existing subscription.
// You usually need a more explicit trigger.

// Modify how schemeNetworkResult is built to incorporate this trigger
// (This is a more advanced setup, the example below is simpler but might not always work as expected for refresh)

    // Simplified conceptual refresh - assumes your use case or repo handles the actual re-fetch
// when observeSchemeListUseCase() is called again or has an internal refresh mechanism.
    fun refreshSchemes() {
        Log.d(tag, "ViewModel refreshSchemes called")
        viewModelScope.launch {
            // This is a placeholder. The actual mechanism to force a refresh depends on your
            // use case and repository design.
            // Option 1: If your use case is a factory for new flows each time.
            // _schemeNetworkResult.value = NetworkResult.Loading() // Show loading immediately
            // observeSchemeListUseCase().stateIn(...) // This would create a new flow if not careful
            // The above is generally not how you refresh with a single stateIn.

            // Option 2: If your use case or repo has a refresh method.
            // e.g., schemeRepository.forceRefreshData()
            // and then the existing Flow from observeSchemeListUseCase() would emit new values.

            // Option 3: Using a trigger flow (more common for complex refresh logic with stateIn)
            // refreshTrigger.emit(Unit)
            // And your main data flow (`observeSchemeListUseCase().flatMapLatest { trigger -> ... }`)
            // would react to this trigger.

            // For now, let's assume calling the use case again (if it's a cold flow
            // that's not being shared in a way that prevents re-execution) might work,
            // or that it triggers an internal refresh in your repository.
            // This is a common point of complexity with StateFlow and refreshes.
            // The simplest for now might be to ensure observeSchemeListUseCase() internally
            // knows it needs to refresh if called again after a certain state.

            // A better approach is usually that the use case itself can be triggered to refresh.
            // If your use case returns a new flow each time and you want to re-collect:
            // This would require changing how schemeNetworkResult is defined (e.g. using a MutableStateFlow
            // and launching collection into it, which you wanted to avoid from init).

            // For this example, let's assume your ObserveSchemeListUseCase or its underlying repository
            // will handle being called again as a signal to refresh.
            // This is a simplification; a dedicated refresh trigger in the use case/repo is better.
            // No direct action here, as the fragment calls, and we assume the existing
            // stateIn setup will just continue. If the use case itself is smart enough to
            // re-fetch when its flow is collected again by a new subscriber (which stateIn doesn't
            // do easily for refresh), or if it has an internal refresh mechanism.
            // THIS IS LIKELY INSUFFICIENT FOR A REAL REFRESH WITH STATEIN
            // You'd typically need to design your use case/repository to be refreshable
            // and expose a method here to trigger that.
            Log.w(tag, "refreshSchemes() in ViewModel needs a proper implementation to trigger data reload via the use case/repository.")
        }
    }

}