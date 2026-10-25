package com.tapflow.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tapflow.data.db.entity.ActionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for automation actions.
 */
@Dao
interface ActionDao {

    @Query("SELECT * FROM actions WHERE automationId = :automationId ORDER BY sortOrder ASC")
    fun getActionsFlow(automationId: Long): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE automationId = :automationId ORDER BY sortOrder ASC")
    suspend fun getActions(automationId: Long): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ActionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(actions: List<ActionEntity>)

    @Update
    suspend fun update(action: ActionEntity)

    @Delete
    suspend fun delete(action: ActionEntity)

    @Query("DELETE FROM actions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM actions WHERE automationId = :automationId")
    suspend fun deleteAllForAutomation(automationId: Long)
}
