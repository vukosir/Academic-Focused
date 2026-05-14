package com.spendsmart.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "earned_badges",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")]
)
data class EarnedBadge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val badgeId: String,
    val earnedAt: Long = System.currentTimeMillis()
)
