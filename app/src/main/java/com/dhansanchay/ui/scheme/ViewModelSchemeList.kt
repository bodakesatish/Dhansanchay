package com.dhansanchay.ui.scheme

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.dhansanchay.domain.model.SchemeModel
import com.dhansanchay.domain.usecases.ObserveSchemeListUseCase
import com.dhansanchay.domain.utils.NetworkResult
import com.dhansanchay.work.AllSchemeDetailsSyncWorker
import com.dhansanchay.work.SimpleTestWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject // Assuming you have a DispatchersModule for Hilt
// import com.yourpackage.IoDispatcher // Assuming a qualifier for your dispatcher

data class SchemeListUiState(
    val schemes: List<SchemeModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ViewModelSchemeList @Inject constructor(
    @ApplicationContext private val appContext: Context, // <-- Inject application context
    private val observeSchemeListUseCase: ObserveSchemeListUseCase
    // Example: @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val tag = this.javaClass.simpleName

    private val refreshTrigger = MutableSharedFlow<Boolean>(replay = 1).apply {
        tryEmit(false) // Initial, non-forced load trigger
        Log.d(tag, "refreshTrigger initialized and initial value emitted (false).")
    }

    val syncWorkInfo: LiveData<List<WorkInfo>> =
        WorkManager.getInstance(appContext)
            .getWorkInfosByTagLiveData(AllSchemeDetailsSyncWorker.WORK_NAME)


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

    fun startFullSchemeDetailSync(forceReset: Boolean = false) {
        val workManager = WorkManager.getInstance(this.appContext) // Use the injected appContext

        // Optional: Cancel any previous ongoing sync to avoid overlap if desired by policy
        // workManager.cancelUniqueWork(AllSchemeDetailsSyncWorker.WORK_NAME)

        val inputDataBuilder = Data.Builder()
        // if (forceReset) {
        //     inputDataBuilder.putBoolean(AllSchemeDetailsSyncWorker.KEY_FORCE_RESET, true)
        // }

        val syncWorkRequest = OneTimeWorkRequestBuilder<AllSchemeDetailsSyncWorker>()
            .setInputData(inputDataBuilder.build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED) // Or UNMETERED for large data
                    // .setRequiresStorageNotLow(true) // If DB operations are heavy
                    .build()
            )
            // Set backoff policy for retries initiated by Result.retry()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, // This is in seconds
                // No third argument needed if the above is the actual delay in millis
                // However, the function signature `setBackoffCriteria(BackoffPolicy, long, TimeUnit)`
                // expects the long to be interpreted *using* the TimeUnit.
                // So, the most correct form for the 3-arg version is:
//                30, // The duration
                TimeUnit.SECONDS // The unit for the duration
            )
            .addTag(AllSchemeDetailsSyncWorker.WORK_NAME) // For easy observation
            .build()

        workManager.enqueueUniqueWork(
            AllSchemeDetailsSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Or KEEP if you don't want to restart if already running
            syncWorkRequest
        )
        Log.i("SchemeSync", "Enqueued AllSchemeDetailsSyncWorker.")
    }

    fun onSyncAllSchemeDetailsClicked() {
        Log.d("ViewModel", "Sync all scheme details initiated by user.")
        // You can get the application context from AndroidViewModel
        startFullSchemeDetailSync(forceReset = false)
    }

    fun startSimpleTestWorker() {
        Log.d("ViewModelSchemeList", "Attempting to start SimpleTestWorker.")

        // Create a WorkRequest for SimpleTestWorker
        val simpleTestWorkRequest = OneTimeWorkRequestBuilder<AllSchemeDetailsSyncWorker>()
            // You can add constraints if needed, like network connectivity
            // .setConstraints(
            //     Constraints.Builder()
            //         .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            //         .build()
            // )
            // You can add a tag to observe or cancel it easily
            // .addTag("SimpleTestWorkerTag")
            .build()

        // Enqueue the WorkRequest
        WorkManager.getInstance(appContext).enqueue(simpleTestWorkRequest)

        Log.i("ViewModelSchemeList", "Enqueued SimpleTestWorker.")

        // If you want to observe its status, you can do so via its ID or tag
        // val workInfoLiveData = WorkManager.getInstance(appContext)
        //                                .getWorkInfoByIdLiveData(simpleTestWorkRequest.id)
        // workInfoLiveData.observe(lifecycleOwner, { workInfo -> // lifecycleOwner needs to be available
        //    if (workInfo != null) {
        //        Log.d("ViewModelSchemeList", "SimpleTestWorker State: ${workInfo.state}")
        //    }
        // })
    }

    // You could call this from a button click or another event in your UI
    fun onTestSimpleWorkerClicked() {
        startSimpleTestWorker()
    }



}