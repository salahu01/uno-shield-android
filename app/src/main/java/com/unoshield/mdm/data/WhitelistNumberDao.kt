package com.unoshield.mdm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WhitelistNumber entity
 */
@Dao
interface WhitelistNumberDao {
    
    @Query("SELECT * FROM whitelist_numbers ORDER BY name ASC, phoneNumber ASC")
    fun getAllWhitelistNumbers(): Flow<List<WhitelistNumber>>
    
    @Query("SELECT * FROM whitelist_numbers WHERE phoneNumber = :phoneNumber")
    suspend fun getWhitelistNumber(phoneNumber: String): WhitelistNumber?
    
    @Query("SELECT phoneNumber FROM whitelist_numbers")
    suspend fun getAllWhitelistPhoneNumbers(): List<String>
    
    @Query("SELECT phoneNumber FROM whitelist_numbers")
    fun getAllWhitelistPhoneNumbersFlow(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelistNumber(whitelistNumber: WhitelistNumber)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelistNumbers(whitelistNumbers: List<WhitelistNumber>)
    
    @Delete
    suspend fun deleteWhitelistNumber(whitelistNumber: WhitelistNumber)
    
    @Query("DELETE FROM whitelist_numbers WHERE phoneNumber = :phoneNumber")
    suspend fun deleteWhitelistNumberByPhone(phoneNumber: String)
    
    @Query("DELETE FROM whitelist_numbers")
    suspend fun deleteAllWhitelistNumbers()
}

