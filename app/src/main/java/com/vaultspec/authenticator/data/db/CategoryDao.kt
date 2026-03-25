package com.vaultspec.authenticator.data.db

import androidx.room.*
import com.vaultspec.authenticator.data.db.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY is_default DESC, name ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    @Delete
    suspend fun delete(category: Category)
}
