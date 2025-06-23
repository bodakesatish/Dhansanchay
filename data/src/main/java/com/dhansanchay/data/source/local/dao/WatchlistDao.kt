package com.dhansanchay.data.source.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhansanchay.data.source.local.entity.WatchlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    // --- Watchlist Items ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItemEntity)

    @Delete
    suspend fun deleteWatchlistItem(item: WatchlistItemEntity)

    @Query("SELECT * FROM watchlist_items WHERE userId = :userId ORDER BY schemeName ASC")
    fun observeUserWatchlist(userId: String): Flow<List<WatchlistItemEntity>>

    @Query("SELECT COUNT(*) FROM watchlist_items WHERE userId = :userId AND schemeCode = :schemeCode")
    suspend fun isSchemeInWatchlist(userId: String, schemeCode: Int): Int

}