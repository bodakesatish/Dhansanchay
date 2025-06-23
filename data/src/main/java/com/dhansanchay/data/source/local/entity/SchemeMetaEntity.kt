package com.dhansanchay.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schemes_meta")
data class SchemeMetaEntity(
    @PrimaryKey
    val schemeCode: Int,
    val schemeName: String,
    val fundHouse: String,
    val isInGrowth: String,
    val schemeCategory: String,
    val schemeType: String
)