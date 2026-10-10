package com.spendsmart.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("userId"), Index("categoryId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryId: Int?,
    val amount: Double,
    val description: String,
    val date: String,
    val startTime: String = "",
    val endTime: String = "",
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
