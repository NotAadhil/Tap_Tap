package com.tapflow.data.repository

import com.tapflow.data.db.dao.RegisteredTagDao
import com.tapflow.data.db.entity.RegisteredTagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository handling tag persistence, automation assignment, and duplicate validation.
 */
class TagRepository(private val tagDao: RegisteredTagDao) {

    val allTagsFlow: Flow<List<RegisteredTagEntity>> = tagDao.getAllTagsFlow()
    val tagCountFlow: Flow<Int> = tagDao.getTagCountFlow()

    suspend fun getTag(tagKey: String): RegisteredTagEntity? = withContext(Dispatchers.IO) {
        tagDao.findByTagKey(tagKey)
    }

    suspend fun isDuplicate(tagKey: String): Boolean = withContext(Dispatchers.IO) {
        tagDao.countByTagKey(tagKey) > 0
    }

    suspend fun registerTag(
        tagKey: String,
        displayName: String,
        rawUidHex: String,
        techList: String,
        automationId: Long? = null,
        actionType: String = "OPEN_URL",
        actionPayload: String = ""
    ): Result<RegisteredTagEntity> = withContext(Dispatchers.IO) {
        if (tagDao.countByTagKey(tagKey) > 0) {
            return@withContext Result.failure(
                IllegalStateException("Tag is already registered.")
            )
        }

        val entity = RegisteredTagEntity(
            tagKey = tagKey,
            displayName = displayName.trim(),
            rawUidHex = rawUidHex,
            techList = techList,
            automationId = automationId,
            actionType = actionType,
            actionPayload = actionPayload.trim(),
            registeredAt = System.currentTimeMillis(),
            isEnabled = true
        )
        tagDao.insert(entity)
        Result.success(entity)
    }

    suspend fun updateDisplayName(tagKey: String, newName: String) = withContext(Dispatchers.IO) {
        tagDao.updateDisplayName(tagKey, newName.trim())
    }

    suspend fun updateAutomationId(tagKey: String, automationId: Long?) = withContext(Dispatchers.IO) {
        tagDao.updateAutomationId(tagKey, automationId)
    }

    suspend fun updateEnabled(tagKey: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        tagDao.updateEnabled(tagKey, isEnabled)
    }

    suspend fun updateTag(tag: RegisteredTagEntity) = withContext(Dispatchers.IO) {
        tagDao.update(tag)
    }

    suspend fun deleteTag(tag: RegisteredTagEntity) = withContext(Dispatchers.IO) {
        tagDao.delete(tag)
    }

    suspend fun deleteTagByKey(tagKey: String) = withContext(Dispatchers.IO) {
        tagDao.deleteByKey(tagKey)
    }
}
