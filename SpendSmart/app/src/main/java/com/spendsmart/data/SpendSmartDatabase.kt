package com.spendsmart.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spendsmart.data.dao.*
import com.spendsmart.data.entities.*

/**
 * Main database class for the SpendSmart application.
 * Defines entities and provides access to DAOs.
 */
@Database(
    entities = [User::class, Category::class, Expense::class, BudgetGoal::class, EarnedBadge::class],
    version = 1,
    exportSchema = false
)
abstract class SpendSmartDatabase : RoomDatabase() {

    // DAOs for data access operations
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun earnedBadgeDao(): EarnedBadgeDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: SpendSmartDatabase? = null

        /**
         * Returns the singleton instance of the database.
         */
        fun getDatabase(context: Context): SpendSmartDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendSmartDatabase::class.java,
                    "spendsmart_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
