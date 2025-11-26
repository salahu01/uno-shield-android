package com.unoshield.mdm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * MDM Database - Stores MDM policy data locally
 * This will sync with server data in the future
 */
@Database(
    entities = [BlockedApp::class],
    version = 1,
    exportSchema = false
)
abstract class MDMDatabase : RoomDatabase() {
    
    abstract fun blockedAppDao(): BlockedAppDao
    
    companion object {
        @Volatile
        private var INSTANCE: MDMDatabase? = null
        
        fun getDatabase(context: Context): MDMDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MDMDatabase::class.java,
                    "mdm_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

