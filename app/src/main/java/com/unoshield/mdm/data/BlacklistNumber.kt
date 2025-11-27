package com.unoshield.mdm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a blacklisted phone number
 */
@Entity(tableName = "blacklist_numbers")
data class BlacklistNumber(
    @PrimaryKey
    val phoneNumber: String,
    val name: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

