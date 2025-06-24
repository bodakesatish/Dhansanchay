package com.dhansanchay.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.io.path.name

@Entity(tableName = "schemes_meta")
data class SchemeMetaEntity(
    @PrimaryKey
    val schemeCode: Int,
    val schemeName: String,
    val fundHouse: String,
    val isInGrowth: String,
    val schemeCategory: String,
    val schemeType: String,
    // New fields for detail fetching process
    var detailFetchStatus: String = DetailFetchStatus.PENDING.name, // PENDING, SUCCESS, FAILED, PROCESSING
    var detailLastAttemptTimestamp: Long = 0L,
    var detailFetchRetryCount: Int = 0,
    var detailErrorMessage: String? = null // Store last error message
)
// Define an enum for status
enum class DetailFetchStatus {
    PENDING,    // Default, needs to be fetched
    PROCESSING, // Currently being attempted by a worker
    SUCCESS,    // Detail fetched and stored successfully
    FAILED      // Fetching failed after retries
}