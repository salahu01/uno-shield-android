package com.unoshield.mdm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * MDM Database - Stores MDM policy data locally
 * This will sync with server data in the future
 */
@Database(
    entities = [BlockedApp::class, BlacklistNumber::class, WhitelistNumber::class],
    version = 2,
    exportSchema = false
)
abstract class MDMDatabase : RoomDatabase() {
    
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blacklistNumberDao(): BlacklistNumberDao
    abstract fun whitelistNumberDao(): WhitelistNumberDao
    
    companion object {
        @Volatile
        private var INSTANCE: MDMDatabase? = null
        
        fun getDatabase(context: Context): MDMDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MDMDatabase::class.java,
                    "mdm_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create blacklist_numbers table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS blacklist_numbers (
                        phoneNumber TEXT NOT NULL PRIMARY KEY,
                        name TEXT,
                        addedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create whitelist_numbers table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS whitelist_numbers (
                        phoneNumber TEXT NOT NULL PRIMARY KEY,
                        name TEXT,
                        addedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}

