package com.spendsmart.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.spendsmart.data.entities.Category

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun getAllForUser(userId: Int): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllForUserSync(userId: Int): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): Category?

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId AND name = :name")
    suspend fun countByName(userId: Int, name: String): Int
}
