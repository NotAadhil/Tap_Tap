package com.tapflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Historical record of an NFC tag scan and automation execution.
 */
@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tagKey: String,
    val tagDisplayName: String,
    val automationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val overallSuccess: Boolean,
    val actionResultsJson: String
)
