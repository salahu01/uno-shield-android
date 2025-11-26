package com.unoshield.mdm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for BlockedApp entity
 */
@Dao
interface BlockedAppDao {
    
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllBlockedApps(): Flow<List<BlockedApp>>
    
    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getBlockedApp(packageName: String): BlockedApp?
    
    @Query("SELECT packageName FROM blocked_apps")
    suspend fun getAllBlockedPackageNames(): List<String>
    
    @Query("SELECT packageName FROM blocked_apps")
    fun getAllBlockedPackageNamesFlow(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(blockedApp: BlockedApp)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApps(blockedApps: List<BlockedApp>)
    
    @Delete
    suspend fun deleteBlockedApp(blockedApp: BlockedApp)
    
    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedAppByPackageName(packageName: String)
    
    @Query("DELETE FROM blocked_apps")
    suspend fun deleteAllBlockedApps()
}

