package com.tapflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An individual action step belonging to an automation.
 */
@Entity(tableName = "actions")
data class ActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: Long,
    val type: String, // "OPEN_APP", "OPEN_URL", "SHOW_NOTIFICATION", "SHOW_MESSAGE"
    val configJson: String,
    val sortOrder: Int,
    val isEnabled: Boolean = true,
    val label: String = ""
)
