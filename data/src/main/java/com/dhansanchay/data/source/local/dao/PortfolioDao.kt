package com.dhansanchay.data.source.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhansanchay.data.source.local.entity.PortfolioItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    // --- Portfolio Items ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolioItem(item: PortfolioItemEntity): Long // Returns row ID

    @Update
    suspend fun updatePortfolioItem(item: PortfolioItemEntity)

    @Delete
    suspend fun deletePortfolioItem(item: PortfolioItemEntity)

    // Using Flow for more reactive data streams, LiveData also works
    @Query("SELECT * FROM portfolio_items WHERE userId = :userId ORDER BY schemeName ASC")
    fun observeUserPortfolio(userId: String): Flow<List<PortfolioItemEntity>>

    @Query("SELECT * FROM portfolio_items WHERE id = :portfolioItemId AND userId = :userId")
    fun observePortfolioItemById(portfolioItemId: Int, userId: String): Flow<PortfolioItemEntity?>

}