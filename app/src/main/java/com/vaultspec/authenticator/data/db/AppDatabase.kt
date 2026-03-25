package com.vaultspec.authenticator.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vaultspec.authenticator.data.db.entity.Category
import com.vaultspec.authenticator.data.db.entity.TokenEntry

@Database(
    entities = [TokenEntry::class, Category::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun categoryDao(): CategoryDao
}
