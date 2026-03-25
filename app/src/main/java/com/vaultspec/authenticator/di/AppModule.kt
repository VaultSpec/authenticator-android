package com.vaultspec.authenticator.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaultspec.authenticator.data.db.AppDatabase
import com.vaultspec.authenticator.data.db.CategoryDao
import com.vaultspec.authenticator.data.db.TokenDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `categories` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `is_default` INTEGER NOT NULL DEFAULT 0
                )"""
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('All', 1)")
            db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('Work', 1)")
            db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('Personal', 1)")
            db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('Social', 1)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vaultspec.db"
        )
        .addMigrations(MIGRATION_1_2)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('All', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('Work', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('Personal', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, is_default) VALUES ('Social', 1)")
            }
        })
        .build()
    }

    @Provides
    fun provideTokenDao(database: AppDatabase): TokenDao {
        return database.tokenDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }
}
