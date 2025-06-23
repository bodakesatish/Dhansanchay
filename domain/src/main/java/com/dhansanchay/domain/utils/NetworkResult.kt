package com.dhansanchay.domain.utils


// Sealed class for Network Results (ensure this is defined in your project)
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>() // Optional, if you want to represent loading state from here
}
