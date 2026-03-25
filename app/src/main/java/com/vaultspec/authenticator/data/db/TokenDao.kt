package com.vaultspec.authenticator.data.db

import androidx.room.*
import com.vaultspec.authenticator.data.db.entity.TokenEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TokenDao {

    @Query("SELECT * FROM tokens ORDER BY sort_order ASC, created_at ASC")
    fun observeAll(): Flow<List<TokenEntry>>

    @Query("SELECT * FROM tokens WHERE category = :category ORDER BY sort_order ASC, created_at ASC")
    fun observeByCategory(category: String): Flow<List<TokenEntry>>

    @Query("SELECT * FROM tokens WHERE issuer LIKE '%' || :query || '%' OR account_name LIKE '%' || :query || '%' ORDER BY sort_order ASC")
    fun search(query: String): Flow<List<TokenEntry>>

    @Query("SELECT * FROM tokens ORDER BY sort_order ASC, created_at ASC")
    suspend fun getAll(): List<TokenEntry>

    @Query("SELECT * FROM tokens WHERE id = :id")
    suspend fun getById(id: Long): TokenEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: TokenEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tokens: List<TokenEntry>)

    @Update
    suspend fun update(token: TokenEntry)

    @Delete
    suspend fun delete(token: TokenEntry)

    @Query("DELETE FROM tokens")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT category FROM tokens")
    fun observeCategories(): Flow<List<String>>
}
