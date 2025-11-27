package com.unoshield.mdm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for BlacklistNumber entity
 */
@Dao
interface BlacklistNumberDao {
    
    @Query("SELECT * FROM blacklist_numbers ORDER BY name ASC, phoneNumber ASC")
    fun getAllBlacklistNumbers(): Flow<List<BlacklistNumber>>
    
    @Query("SELECT * FROM blacklist_numbers WHERE phoneNumber = :phoneNumber")
    suspend fun getBlacklistNumber(phoneNumber: String): BlacklistNumber?
    
    @Query("SELECT phoneNumber FROM blacklist_numbers")
    suspend fun getAllBlacklistPhoneNumbers(): List<String>
    
    @Query("SELECT phoneNumber FROM blacklist_numbers")
    fun getAllBlacklistPhoneNumbersFlow(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklistNumber(blacklistNumber: BlacklistNumber)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklistNumbers(blacklistNumbers: List<BlacklistNumber>)
    
    @Delete
    suspend fun deleteBlacklistNumber(blacklistNumber: BlacklistNumber)
    
    @Query("DELETE FROM blacklist_numbers WHERE phoneNumber = :phoneNumber")
    suspend fun deleteBlacklistNumberByPhone(phoneNumber: String)
    
    @Query("DELETE FROM blacklist_numbers")
    suspend fun deleteAllBlacklistNumbers()
}

