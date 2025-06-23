package com.dhansanchay.data.source.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


/**
 * Represents a mutual fund scheme added to the user's portfolio.
 * This does NOT store individual buy/sell transactions, only a link to the scheme.
 * Individual transactions are stored in TransactionEntity.
 */
@Entity(
    tableName = "portfolio_items",
    foreignKeys = [
        ForeignKey(
            entity = SchemeEntity::class,
            parentColumns = ["schemeCode"],
            childColumns = ["schemeCode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["schemeCode"], unique = true)] // Ensure one portfolio item per scheme
)
data class PortfolioItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // To link to a specific user's portfolio
    val schemeCode: Int,
    val schemeName: String, // Denormalized for easier display without extra joins
    val lastUpdatedNav: Double? = null, // Store latest NAV for performance calc
    val lastNavUpdateDate: Long? = null // Timestamp of last NAV update
)
