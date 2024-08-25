package com.dhansanchay.data.source.base

object ApiResponseCode {
    const val SUCCESS = 0
    const val EMPTY = 1
    const val NETWORK_ERROR = 2
    const val UNKNOWN_ERROR = 3
    const val AUTHENTICATION_FAILED = 4
    const val NOT_FOUND = 5
    const val BAD_REQUEST = 6
    const val SPOT_UPDATE_AFTER_5_PM = 7
    const val SPOT_CANCEL_AFTER_5_PM = 8
    const val SPOT_BOOKING_AFTER_5_PM = 9
    const val DUPLICATE_SPOT_BOOKING = 10
    const val SPOT_ALREADY_BOOKED = 11
    const val SPOT_BOOKING_CANCELLED = 12
    const val SPOT_EDIT_CANCEL_AFTER_CHECK_IN = 13
    const val SPOT_CHECK_IN_AFTER_5_PM = 14
    const val FLOOR_DETAILS_NOT_FOUND = 15
}