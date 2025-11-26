package com.unoshield.mdm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a blocked app in the MDM system
 * This data will come from the server in the future
 */
@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val blockedAt: Long = System.currentTimeMillis()
)

