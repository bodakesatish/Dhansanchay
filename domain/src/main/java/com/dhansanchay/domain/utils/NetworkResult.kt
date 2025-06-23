package com.dhansanchay.domain.utils


// Sealed class for Network Results (Modified to include an optional error code)
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()

    /**
     * Represents a failed network operation.
     * @param message A descriptive error message.
     * @param code An optional code (e.g., HTTP status code, or custom error code).
     * @param exception The optional Throwable that caused the error.
     */
    data class Error(
        val message: String,
        val code: Int? = null, // Added: Optional error code
        val exception: Throwable? = null
    ) : NetworkResult<Nothing>()

    object Loading : NetworkResult<Nothing>() // Represents the loading state
}