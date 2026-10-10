package com.spendsmart.data.dao

import androidx.room.*
import com.spendsmart.data.entities.EarnedBadge

@Dao
interface EarnedBadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(badge: EarnedBadge): Long

    @Query("SELECT * FROM earned_badges WHERE userId = :userId")
    suspend fun getAllForUser(userId: Int): List<EarnedBadge>

    @Query("SELECT COUNT(*) FROM earned_badges WHERE userId = :userId AND badgeId = :badgeId")
    suspend fun hasBadge(userId: Int, badgeId: String): Int
}
