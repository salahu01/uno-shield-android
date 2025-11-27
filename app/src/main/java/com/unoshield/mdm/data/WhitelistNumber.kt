package com.unoshield.mdm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a whitelisted phone number
 */
@Entity(tableName = "whitelist_numbers")
data class WhitelistNumber(
    @PrimaryKey
    val phoneNumber: String,
    val name: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

