package com.spendsmart.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.spendsmart.data.entities.Expense

data class CategoryTotal(
    val categoryId: Int?,
    val categoryName: String?,
    val colorHex: String?,
    val total: Double
)

data class DailyTotal(
    val date: String,
    val total: Double
)

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun getAllForUser(userId: Int): LiveData<List<Expense>>

    @Query("""
        SELECT e.*, c.name as catName FROM expenses e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.userId = :userId AND e.date >= :from AND e.date <= :to
        ORDER BY e.date DESC, e.createdAt DESC
    """)
    suspend fun getInRange(userId: Int, from: String, to: String): List<Expense>

    @Query("""
        SELECT e.categoryId, c.name as categoryName, c.colorHex, SUM(e.amount) as total
        FROM expenses e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.userId = :userId AND e.date >= :from AND e.date <= :to
        GROUP BY e.categoryId
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotals(userId: Int, from: String, to: String): List<CategoryTotal>

    @Query("""
        SELECT e.date, SUM(e.amount) as total
        FROM expenses e
        WHERE e.userId = :userId AND e.date >= :from AND e.date <= :to
        GROUP BY e.date
        ORDER BY e.date ASC
    """)
    suspend fun getDailyTotals(userId: Int, from: String, to: String): List<DailyTotal>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date >= :from AND date <= :to")
    suspend fun getTotalInRange(userId: Int, from: String, to: String): Double?

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): Expense?

    @Query("SELECT COUNT(*) FROM expenses WHERE userId = :userId")
    suspend fun countForUser(userId: Int): Int
}
