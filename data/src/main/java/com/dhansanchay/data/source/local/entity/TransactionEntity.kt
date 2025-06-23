package com.dhansanchay.data.source.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Represents an individual buy or sell transaction for a mutual fund scheme
 * within the user's portfolio.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["portfolioItemId"],
            onDelete = ForeignKey.CASCADE // If portfolio item is deleted, delete its transactions
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val portfolioItemId: Int, // Link to the PortfolioItemEntity
    val transactionDate: Long, // Unix timestamp in milliseconds
    val type: String, // "BUY" or "SELL"
    val units: Double,
    val pricePerUnit: Double, // NAV at the time of transaction
    val amount: Double // Calculated as units * pricePerUnit
)