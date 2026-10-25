package com.tapflow.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tapflow.data.db.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Automations.
 */
@Dao
interface AutomationDao {

    @Query("SELECT * FROM automations ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AutomationEntity?

    @Query("SELECT COUNT(*) FROM automations")
    fun getAutomationCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(automation: AutomationEntity): Long

    @Update
    suspend fun update(automation: AutomationEntity)

    @Delete
    suspend fun delete(automation: AutomationEntity)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
