package com.dhansanchay.domain.model

sealed class ResponseCode {
    object Success : ResponseCode()
    object Empty : ResponseCode()
    object Network : ResponseCode()
    object Authentication : ResponseCode()
    object Fail : ResponseCode()
    object NOT_FOUND : ResponseCode()
    object BAD_REQUEST : ResponseCode()
}