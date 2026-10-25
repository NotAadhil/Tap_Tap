package com.tapflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tapflow.data.db.entity.ExecutionLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Execution Logs.
 */
@Dao
interface ExecutionLogDao {

    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogsFlow(): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLogFlow(): Flow<ExecutionLogEntity?>

    @Insert
    suspend fun insert(log: ExecutionLogEntity): Long

    @Query("DELETE FROM execution_logs")
    suspend fun clearAll()

    @Query("DELETE FROM execution_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
