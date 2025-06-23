package com.dhansanchay.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an official mutual fund scheme stored in the local database.
 * This entity is populated from the first API response.
 */
@Entity(tableName = "mutual_fund_schemes")
data class SchemeEntity(
    @PrimaryKey
    val schemeCode: Int,
    val schemeName: String
)