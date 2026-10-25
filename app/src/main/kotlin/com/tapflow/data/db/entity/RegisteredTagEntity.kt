package com.tapflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a registered NFC tag stored in the local Room database.
 */
@Entity(tableName = "registered_tags")
data class RegisteredTagEntity(
    @PrimaryKey val tagKey: String,
    val displayName: String,
    val rawUidHex: String,
    val techList: String,
    val actionType: String = "OPEN_URL",
    val actionPayload: String = "",
    val registeredAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)
