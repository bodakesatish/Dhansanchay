package com.dhansanchay

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App :Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Optional: Add logging in onCreate to confirm Hilt injection is working at the Application level
    override fun onCreate() {
        super.onCreate()
        if (::workerFactory.isInitialized) {
            Log.d("AppClass", "App onCreate: HiltWorkerFactory is initialized.")
        } else {
            Log.e("AppClass", "App onCreate: HiltWorkerFactory IS NOT INITIALIZED. Hilt setup issue in Application class.")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG) // Optional: for easier debugging
            .build()
}