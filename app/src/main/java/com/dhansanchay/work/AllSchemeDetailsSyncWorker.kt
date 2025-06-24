package com.dhansanchay.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters // Keep this import
import androidx.work.workDataOf
import com.dhansanchay.data.source.local.SchemeLocalDataSource
import com.dhansanchay.data.source.local.entity.DetailFetchStatus
import com.dhansanchay.data.source.local.entity.SchemeMetaEntity
import com.dhansanchay.data.source.remote.SchemeRemoteDataSource
import com.dhansanchay.domain.utils.NetworkResult
import dagger.assisted.Assisted // <<< ADD THIS IMPORT
import dagger.assisted.AssistedInject // <<< ADD THIS IMPORT
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@HiltWorker
class AllSchemeDetailsSyncWorker @AssistedInject constructor( // <<< ANNOTATE CONSTRUCTOR
    @Assisted appContext: Context, // <<< ANNOTATE WITH @Assisted
    @Assisted workerParams: WorkerParameters, // <<< ANNOTATE WITH @Assisted
) : CoroutineWorker(appContext, workerParams) {

    private val remoteDataSource: SchemeRemoteDataSource
    private val localDataSource: SchemeLocalDataSource

    init {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WorkerDependenciesEntryPoint::class.java
        )
        remoteDataSource = hiltEntryPoint.remoteDataSource()
        localDataSource = hiltEntryPoint.localDataSource()
    }

    // ... (rest of your worker code: companion object, doWork(), etc. remains the same) ...

    companion object {
        const val WORK_NAME = "AllSchemeDetailsSyncWorker"
        const val NOTIFICATION_ID = 101
        const val NOTIFICATION_CHANNEL_ID = "SchemeSyncChannel"

        // Input data keys (optional, could fetch all pending from DB)
        // const val KEY_FORCE_RESET = "force_reset"

        // Progress data keys
        const val KEY_PROGRESS_CURRENT = "progress_current"
        const val KEY_PROGRESS_TOTAL = "progress_total"
        const val KEY_PROGRESS_FAILED_TEMP = "progress_failed_temp" // Failures in this run
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Define a limited concurrency dispatcher for network calls within the worker
    private val networkCallDispatcher = Dispatchers.IO.limitedParallelism(5) // Adjust concurrency

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        createNotificationChannel()
        return androidx.work.ForegroundInfo(NOTIFICATION_ID, createNotification(0, 0, 0)) // Initial notification
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "doWork started.")
        // Optionally handle inputData for force_reset
        // if (inputData.getBoolean(KEY_FORCE_RESET, false)) {
        //     localDataSource.resetAllSchemeDetailStatusToPending()
        // }

        val initialCounts = localDataSource.getSchemeDetailFetchCounts()
        val totalSchemesToProcess = (initialCounts[DetailFetchStatus.PENDING] ?: 0) +
                (initialCounts[DetailFetchStatus.FAILED] ?: 0) // Consider failed as pending for retry

        if (totalSchemesToProcess == 0 && (initialCounts[DetailFetchStatus.PROCESSING] ?: 0) == 0) {
            Log.d(WORK_NAME, "No schemes to process.")
            updateNotification(processed = 0, total = 0, failedInRun = 0, isComplete = true)
            return Result.success()
        }

        var processedInThisRun = 0
        var failedInThisRun = 0
        val batchSize = 50 // How many schemes to query and process in one internal loop

        while (true) { // Loop until no more items or work is stopped
            if (isStopped) {
                Log.d(WORK_NAME, "Work stopped by cancellation.")
                // Schemes marked as PROCESSING will be picked up in the next run
                return Result.failure() // Or Result.retry() if appropriate
            }

            // Prioritize retries for items that previously failed (with backoff logic)
            val schemesToProcess = getNextBatchOfSchemes(batchSize)

            if (schemesToProcess.isEmpty()) {
                Log.d(WORK_NAME, "No more schemes to process in this iteration.")
                break // Exit loop
            }

            // Mark this batch as PROCESSING
            schemesToProcess.forEach { localDataSource.markSchemeAsProcessing(it.schemeCode) }

            // Process the current batch concurrently
            val results = coroutineScope { // <--- Add this coroutineScope builder
                schemesToProcess.map { scheme ->
                    async(networkCallDispatcher) { // 'async' is now an extension on the 'coroutineScope'
                        if (isStopped) {
                            // If stopped, we might want to ensure the scheme status reflects it hasn't been processed
                            // or is reverted to PENDING if it was marked PROCESSING earlier.
                            // For simplicity, let's assume it gets picked up again.
                            Log.d(WORK_NAME, "Async task for ${scheme.schemeCode} cancelled before start/during execution.")
                            return@async Pair(scheme.schemeCode, DetailFetchStatus.PENDING) // Or another appropriate status
                        }

                        Log.d(WORK_NAME, "Fetching detail for ${scheme.schemeCode}")
                        try {
                            when (val remoteResult = remoteDataSource.fetchSchemeDetail(scheme.schemeCode)) {
                                is NetworkResult.Success -> {
                                    localDataSource.insertSchemeMeta(remoteResult.data)
                                    localDataSource.updateSchemeDetailFetchStatus(scheme.schemeCode, DetailFetchStatus.SUCCESS)
                                    Pair(scheme.schemeCode, DetailFetchStatus.SUCCESS)
                                }
                                is NetworkResult.Error -> {
                                    Log.w(WORK_NAME, "Error for ${scheme.schemeCode}: ${remoteResult.message}, Ex: ${remoteResult.exception}")
                                    if (isRetryableError(remoteResult.exception, remoteResult.message)) {
                                        localDataSource.incrementSchemeDetailRetryCount(scheme.schemeCode)
                                        localDataSource.updateSchemeDetailFetchStatus(scheme.schemeCode, DetailFetchStatus.PENDING, remoteResult.message)
                                    } else {
                                        localDataSource.updateSchemeDetailFetchStatus(scheme.schemeCode, DetailFetchStatus.FAILED, remoteResult.message)
                                    }
                                    Pair(scheme.schemeCode, DetailFetchStatus.FAILED)
                                }
                                is NetworkResult.Loading -> Pair(scheme.schemeCode, DetailFetchStatus.PROCESSING) // Should ideally not be the final state here
                            }
                        } catch (e: Exception) {
                            Log.e(WORK_NAME, "Unhandled exception for ${scheme.schemeCode}: $e")
                            localDataSource.updateSchemeDetailFetchStatus(scheme.schemeCode, DetailFetchStatus.FAILED, e.localizedMessage ?: "Unknown exception")
                            Pair(scheme.schemeCode, DetailFetchStatus.FAILED)
                        }
                    }
                }
            }.awaitAll() // awaitAll is called on the List<Deferred> returned by map


            results.forEach { (_, status) ->
                if (status == DetailFetchStatus.SUCCESS) {
                    // Already handled
                } else if (status == DetailFetchStatus.FAILED) {
                    failedInThisRun++
                }
            }
            processedInThisRun += schemesToProcess.size

            // Update progress notification and WorkManager progress
            val currentProgressCounts = localDataSource.getSchemeDetailFetchCounts()
            val totalSuccessfullyProcessed = currentProgressCounts[DetailFetchStatus.SUCCESS] ?: 0
            val overallTotalEligible = (initialCounts.values.sum()) - (initialCounts[DetailFetchStatus.PROCESSING] ?:0) // Initial total minus those that were already processing

            setProgressAsync(
                workDataOf(
                    KEY_PROGRESS_CURRENT to totalSuccessfullyProcessed,
                    KEY_PROGRESS_TOTAL to overallTotalEligible,
                    KEY_PROGRESS_FAILED_TEMP to failedInThisRun
                )
            )
            updateNotification(totalSuccessfullyProcessed, overallTotalEligible, failedInThisRun)

            // Small delay to allow system to breathe, especially if processing very fast locally
            delay(100)
        }

        val finalCounts = localDataSource.getSchemeDetailFetchCounts()
        val stillPending = (finalCounts[DetailFetchStatus.PENDING] ?: 0) + (finalCounts[DetailFetchStatus.PROCESSING] ?: 0)
        val permanentlyFailed = finalCounts[DetailFetchStatus.FAILED] ?: 0

        Log.d(WORK_NAME, "doWork finished. Still Pending/Processing: $stillPending, Permanently Failed: $permanentlyFailed, Failed in this run: $failedInThisRun")

        updateNotification(finalCounts[DetailFetchStatus.SUCCESS]?:0, (initialCounts.values.sum()), failedInThisRun, isComplete = true, permanentlyFailed = permanentlyFailed)

        return if (stillPending > 0) {
            Log.i(WORK_NAME, "Work has more items to process, requesting retry.")
            Result.retry() // WorkManager will retry with backoff
        } else {
            Log.i(WORK_NAME, "All eligible items processed.")
            Result.success()
        }
    }

    private suspend fun getNextBatchOfSchemes(batchSize: Int): List<SchemeMetaEntity> {
        val maxRetries = 5 // Configurable
        val retryDelayMillis = 15 * 60 * 1000L // 15 minutes (for items that failed before)

        // Prioritize items that have failed and their retry time is up
        var schemes = localDataSource.getSchemesToRetry(batchSize, maxRetries, System.currentTimeMillis() - retryDelayMillis)
        if (schemes.isNotEmpty()) {
            Log.d(WORK_NAME, "Found ${schemes.size} schemes to retry.")
            return schemes
        }

        // Then get pending schemes
        schemes = localDataSource.getPendingSchemeDetails(batchSize)
        if (schemes.isNotEmpty()) {
            Log.d(WORK_NAME, "Found ${schemes.size} pending schemes.")
        }
        return schemes
    }

    private fun isRetryableError(exception: Throwable?, message: String?): Boolean {
        // Server errors (5xx), timeout errors, temporary network issues are typically retryable.
        // Client errors (4xx, except 429 Too Many Requests) are often not.
        return when (exception) {
            is java.io.IOException -> true // Network issues
            is java.util.concurrent.TimeoutException -> true
            else -> {
                // Check message for HTTP codes if not directly available from exception
                message?.contains("500") == true || message?.contains("502") == true ||
                        message?.contains("503") == true || message?.contains("504") == true ||
                        message?.contains("timeout") == true || message?.contains("timed out") == true
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scheme Data Sync"
            val descriptionText = "Notifications for scheme data synchronization"
            val importance = NotificationManager.IMPORTANCE_LOW // Low to avoid sound/vibration unless critical
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(processed: Int, total: Int, failedInRun: Int, isComplete: Boolean = false, permanentlyFailed: Int = 0): Notification {
        val title = if (isComplete) "Scheme Sync Complete" else "Syncing Scheme Details..."
        var contentText = "Processed: $processed / $total"
        if (failedInRun > 0) contentText += " (Failures this run: $failedInRun)"
        if (isComplete && permanentlyFailed > 0) contentText += ". Permanently failed: $permanentlyFailed."
        if (isComplete && permanentlyFailed == 0 && processed == total) contentText = "All schemes synced successfully!"


        // Intent to open app (optional)
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Cancel action for the notification (and thus the worker)
        val cancelIntent = androidx.work.WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id) // id is the Worker's ID

        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Replace with your app icon
            .setOngoing(!isComplete) // Makes it non-dismissable if not complete
            .setAutoCancel(isComplete) // Dismiss on tap if complete
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel Sync", cancelIntent)
            .setProgress(total, processed, total == 0 && processed == 0 && !isComplete) // Indeterminate if starting
            .build()
    }

    private fun updateNotification(processed: Int, total: Int, failedInRun: Int, isComplete: Boolean = false, permanentlyFailed: Int = 0) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(processed, total, failedInRun, isComplete, permanentlyFailed))
    }
}