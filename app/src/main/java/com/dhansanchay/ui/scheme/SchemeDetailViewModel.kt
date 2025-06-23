package com.dhansanchay.ui.scheme

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhansanchay.domain.model.SchemeMetaModel
import com.dhansanchay.domain.usecases.GetSchemeMetaUseCase
import com.dhansanchay.domain.utils.NetworkResult // Assuming you have this class
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher // For potential dispatcher injection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
// Assuming you might have a dispatchers module for Hilt providing e.g., @IoDispatcher
// import com.yourpackage.IoDispatcher
// import kotlinx.coroutines.Dispatchers // Or use Dispatchers.IO directly if not injecting

/**
 * UI state for the Scheme Meta detail screen.
 *
 * @property schemeDetail The details of the scheme. Null if not loaded or error.
 * @property isLoading True if data is currently being loaded.
 * @property errorMessage Error message if loading failed, null otherwise.
 */
data class SchemeMetaUiState(
    val schemeDetail: SchemeMetaModel? = null, // Changed to nullable for clarity
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SchemeMetaViewModel @Inject constructor(
    private val getSchemeMetaUseCase: GetSchemeMetaUseCase, // Renamed for clarity if it gets single meta
    private val savedStateHandle: SavedStateHandle
    // Example: @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Inject dispatcher
) : ViewModel() {

    private val tag = this.javaClass.simpleName
    private val schemeCodeArgKey = "schemeCodeArg" // Define key for SavedStateHandle

    // Holds the current scheme code. Initialized from SavedStateHandle.
    private val _schemeCode = MutableStateFlow<Int?>(savedStateHandle.get<Int>(schemeCodeArgKey))

    // Triggers data loading/refresh. Boolean indicates if it's a forced refresh.
    private val refreshTrigger = MutableSharedFlow<Boolean>(replay = 1)

    init {
        Log.d(tag, "ViewModel initialized. Initial schemeCode from SavedStateHandle: ${_schemeCode.value}")

        // If a scheme code was present from SavedStateHandle (e.g., ViewModel recreation),
        // trigger an initial, non-forced load for it.
        _schemeCode.value?.let { initialCode ->
            Log.d(tag, "SchemeCode $initialCode present in SavedStateHandle during init. Triggering initial load.")
            viewModelScope.launch { // Consider using injected ioDispatcher
                refreshTrigger.emit(false) // Not a forced refresh
            }
        }
        // If _schemeCode.value is null, the flow will wait for loadSchemeDetail() to be called.
    }

    /**
     * Loads the scheme details for the given code.
     * It updates the internal scheme code and triggers a data fetch.
     *
     * @param code The scheme code to load details for.
     * @param isForcedRefresh True to force a refresh from the network, false to potentially use cache.
     */
    fun loadSchemeDetail(code: Int, isForcedRefresh: Boolean = false) {
        Log.d(tag, "loadSchemeDetail called with code: $code, isForced: $isForcedRefresh")
        val previousCode = _schemeCode.value

        // Update schemeCode and save to SavedStateHandle for process death restoration
        _schemeCode.value = code
        savedStateHandle[schemeCodeArgKey] = code

        // Trigger a refresh if:
        // 1. The scheme code has changed.
        // 2. A refresh is explicitly forced.
        // 3. It's the first time this specific code is being loaded by this ViewModel instance
        //    (handles the case where init didn't trigger because schemeCode was initially null).
        if (previousCode != code || isForcedRefresh) {
            viewModelScope.launch { // Consider using injected ioDispatcher
                Log.d(tag, "Triggering refresh. previousCode: $previousCode, newCode: $code, isForced: $isForcedRefresh")
                refreshTrigger.emit(isForcedRefresh)
            }
        } else if (previousCode == null && code != null) {
            // This case handles when the ViewModel is created, schemeCode is initially null,
            // and loadSchemeDetail is the first method to set it.
            // The init block's trigger wouldn't have run for _schemeCode.
            viewModelScope.launch {
                Log.d(tag, "Triggering initial refresh for code $code as it was first set by loadSchemeDetail.")
                refreshTrigger.emit(false) // Not forced for the first load via this path
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val schemeMetasResult: StateFlow<NetworkResult<SchemeMetaModel>> =
        _schemeCode.filterNotNull() // Only proceed if schemeCode is not null
            // No distinctUntilChanged on _schemeCode here, as refreshTrigger handles re-fetch logic
            .combine(refreshTrigger) { code, isForced -> // Combine with refresh trigger
                Log.d(tag, "Combining schemeCode: $code with refreshTrigger isForced: $isForced")
                Pair(code, isForced)
            }
            // Ensure that consecutive identical pairs of (code, isForced) don't trigger flatMapLatest unnecessarily,
            // unless the use case itself is idempotent or handles this.
            // If refreshTrigger emits `false` multiple times for the same code, flatMapLatest will re-trigger.
            // distinctUntilChanged() here applies to the Pair(code, isForced).
            .distinctUntilChanged { old, new -> old.first == new.first && old.second == new.second }
            .flatMapLatest { (code, isForced) ->
                Log.d(tag, "flatMapLatest triggered. schemeCode: $code, isForced: $isForced")
                getSchemeMetaUseCase(schemeCode = code, forceRefresh = isForced)
            }
            .stateIn(
                scope = viewModelScope, // Consider using viewModelScope.plus(ioDispatcher)
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = NetworkResult.Loading // Initial state before any code is processed
            )

    /**
     * The UI state for the scheme meta detail screen, observed by the Fragment.
     */
    val uiState: StateFlow<SchemeMetaUiState> = schemeMetasResult
        .map { result ->
            // Access the current schemeDetail from the uiState.value if it exists to carry over
            val currentSchemeDetail = uiState.value.schemeDetail // This is SchemeMetaModel?

            when (result) {
                is NetworkResult.Success -> {
                    Log.d(tag, "Mapping Success: Data received for schemeCode: ${result.data?.schemeCode}")
                    SchemeMetaUiState(
                        schemeDetail = result.data, // result.data is SchemeMetaModel or null
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is NetworkResult.Loading -> {
                    Log.d(tag, "Mapping Loading")
                    SchemeMetaUiState(
                        schemeDetail = currentSchemeDetail, // Keep old data while loading
                        isLoading = true,
                        errorMessage = null // Clear previous error
                    )
                }
                is NetworkResult.Error -> {
                    Log.d(tag, "Mapping Error: ${result.message}")
                    SchemeMetaUiState(
                        schemeDetail = currentSchemeDetail, // Keep old data on error
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                // Handle other NetworkResult subtypes if you have them
            }
        }
        .stateIn(
            scope = viewModelScope, // Consider using viewModelScope.plus(ioDispatcher)
            started = SharingStarted.WhileSubscribed(5000L),
            // Initial UI state: loading, no data, no error.
            initialValue = SchemeMetaUiState(isLoading = true, schemeDetail = null, errorMessage = null)
        )

    /**
     * Forces a refresh of the current scheme details from the network.
     */
    fun forceRefreshSchemeDetail() {
        val currentCode = _schemeCode.value
        if (currentCode != null) {
            Log.d(tag, "forceRefreshSchemeDetail called for schemeCode: $currentCode")
            viewModelScope.launch { // Consider using injected ioDispatcher
                refreshTrigger.emit(true) // isForced = true
            }
        } else {
            Log.w(tag, "forceRefreshSchemeDetail called but schemeCode is null. Cannot refresh.")
        }
    }
}