package com.tapflow.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tapflow.data.db.entity.RegisteredTagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for registered NFC tags.
 */
@Dao
interface RegisteredTagDao {

    @Query("SELECT * FROM registered_tags ORDER BY registeredAt DESC")
    fun getAllTagsFlow(): Flow<List<RegisteredTagEntity>>

    @Query("SELECT * FROM registered_tags WHERE tagKey = :tagKey LIMIT 1")
    suspend fun findByTagKey(tagKey: String): RegisteredTagEntity?

    @Query("SELECT COUNT(*) FROM registered_tags WHERE tagKey = :tagKey")
    suspend fun countByTagKey(tagKey: String): Int

    @Query("SELECT COUNT(*) FROM registered_tags")
    fun getTagCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: RegisteredTagEntity)

    @Update
    suspend fun update(tag: RegisteredTagEntity)

    @Query("UPDATE registered_tags SET displayName = :displayName WHERE tagKey = :tagKey")
    suspend fun updateDisplayName(tagKey: String, displayName: String)

    @Query("UPDATE registered_tags SET automationId = :automationId WHERE tagKey = :tagKey")
    suspend fun updateAutomationId(tagKey: String, automationId: Long?)

    @Query("UPDATE registered_tags SET automationId = NULL WHERE automationId = :automationId")
    suspend fun clearAutomationReferences(automationId: Long)

    @Query("UPDATE registered_tags SET isEnabled = :isEnabled WHERE tagKey = :tagKey")
    suspend fun updateEnabled(tagKey: String, isEnabled: Boolean)

    @Delete
    suspend fun delete(tag: RegisteredTagEntity)

    @Query("DELETE FROM registered_tags WHERE tagKey = :tagKey")
    suspend fun deleteByKey(tagKey: String)
}
