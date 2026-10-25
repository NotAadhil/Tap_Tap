package com.tapflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named automation sequence containing ordered actions.
 */
@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
