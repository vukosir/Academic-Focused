package com.spendsmart.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.spendsmart.data.entities.BudgetGoal

@Dao
interface BudgetGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: BudgetGoal): Long

    @Update
    suspend fun update(goal: BudgetGoal)

    @Delete
    suspend fun delete(goal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE userId = :userId")
    fun getAllForUser(userId: Int): LiveData<List<BudgetGoal>>

    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND categoryId IS NULL LIMIT 1")
    suspend fun getOverallGoal(userId: Int): BudgetGoal?

    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND categoryId = :catId LIMIT 1")
    suspend fun getForCategory(userId: Int, catId: Int): BudgetGoal?

    @Query("SELECT * FROM budget_goals WHERE userId = :userId")
    suspend fun getAllSync(userId: Int): List<BudgetGoal>
}
