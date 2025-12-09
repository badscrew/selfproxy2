package com.selfproxy.vpn.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selfproxy.vpn.data.model.AppRoutingConfig
import com.selfproxy.vpn.data.model.ServerProfile

/**
 * Room database for the SelfProxy VPN application.
 * 
 * Contains all database entities and provides DAOs for data access.
 */
@Database(
    entities = [ServerProfile::class, AppRoutingConfig::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to profile operations.
     */
    abstract fun profileDao(): ProfileDao
    
    /**
     * Provides access to app routing configuration operations.
     */
    abstract fun appRoutingDao(): AppRoutingDao
    
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
                .addMigrations(MIGRATION_1_2)
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
 * Migration from version 1 to version 2: Add app routing configuration table.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create app_routing_config table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS app_routing_config (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                profileId INTEGER,
                routeAllApps INTEGER NOT NULL,
                packageNames TEXT NOT NULL,
                lastUpdated INTEGER NOT NULL
            )
        """.trimIndent())
        
        // Create index on profileId for faster lookups
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_app_routing_config_profileId 
            ON app_routing_config(profileId)
        """.trimIndent())
    }
}
