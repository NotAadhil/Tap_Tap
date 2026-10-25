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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: RegisteredTagEntity)

    @Update
    suspend fun update(tag: RegisteredTagEntity)

    @Delete
    suspend fun delete(tag: RegisteredTagEntity)

    @Query("DELETE FROM registered_tags WHERE tagKey = :tagKey")
    suspend fun deleteByKey(tagKey: String)
}
