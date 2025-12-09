package com.selfproxy.vpn.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selfproxy.vpn.data.model.ServerProfile

/**
 * Room database for the SelfProxy VPN application.
 * 
 * Contains all database entities and provides DAOs for data access.
 */
@Database(
    entities = [ServerProfile::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to profile operations.
     */
    abstract fun profileDao(): ProfileDao
    
    companion object {
        private const val DATABASE_NAME = "selfproxy.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Gets the singleton database instance.
         * 
         * @param context Application context
         * @return The database instance
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(/* Future migrations will be added here */)
                .fallbackToDestructiveMigration() // For development only - remove in production
                .build()
        }
        
        /**
         * Clears the database instance (for testing purposes).
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}

/**
 * Migration from version 1 to version 2 (placeholder for future use).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Future migration logic will be added here
    }
}
