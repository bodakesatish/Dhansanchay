package com.dhansanchay.data.source.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a mutual fund scheme added to the user's watchlist.
 */
@Entity(
    tableName = "watchlist_items",
    foreignKeys = [
        ForeignKey(
            entity = SchemeEntity::class,
            parentColumns = ["schemeCode"],
            childColumns = ["schemeCode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["schemeCode", "userId"], unique = true)] // Ensure unique per user
)
data class WatchlistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val schemeCode: Int,
    val schemeName: String // Denormalized for easier display
)