package com.tapflow.data.repository

import com.tapflow.automation.ActionMapper
import com.tapflow.automation.AutomationAction
import com.tapflow.data.db.dao.ActionDao
import com.tapflow.data.db.dao.AutomationDao
import com.tapflow.data.db.dao.RegisteredTagDao
import com.tapflow.data.db.entity.AutomationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository coordinating automations and their ordered child actions.
 */
class AutomationRepository(
    private val automationDao: AutomationDao,
    private val actionDao: ActionDao,
    private val tagDao: RegisteredTagDao? = null
) {

    fun getAllAutomationsFlow(): Flow<List<AutomationEntity>> {
        return automationDao.getAllFlow()
    }

    fun getAutomationCountFlow(): Flow<Int> {
        return automationDao.getAutomationCountFlow()
    }

    suspend fun getAutomationById(id: Long): AutomationEntity? {
        return automationDao.findById(id)
    }

    fun getActionsFlow(automationId: Long): Flow<List<AutomationAction>> {
        return actionDao.getActionsFlow(automationId).map { entities ->
            entities.map { ActionMapper.toDomain(it) }
        }
    }

    suspend fun getActions(automationId: Long): List<AutomationAction> {
        return actionDao.getActions(automationId).map { ActionMapper.toDomain(it) }
    }

    suspend fun saveAutomation(
        automation: AutomationEntity,
        actions: List<AutomationAction>
    ): Long {
        val updatedAutomation = automation.copy(updatedAt = System.currentTimeMillis())
        val savedId = automationDao.insert(updatedAutomation)
        val targetId = if (automation.id == 0L) savedId else automation.id

        // Clear existing actions for this automation and replace with ordered list
        actionDao.deleteAllForAutomation(targetId)
        val entities = actions.mapIndexed { index, action ->
            val domainWithUpdatedOrder = action.withSortOrder(index).withAutomationId(targetId)
            ActionMapper.toEntity(domainWithUpdatedOrder)
        }
        actionDao.insertAll(entities)
        return targetId
    }

    suspend fun updateAutomationEnabled(id: Long, isEnabled: Boolean) {
        val automation = automationDao.findById(id) ?: return
        automationDao.update(automation.copy(isEnabled = isEnabled, updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicateAutomation(automationId: Long): Long = withContext(Dispatchers.IO) {
        val original = getAutomationById(automationId) ?: return@withContext 0L
        val actions = getActions(automationId)
        val copy = original.copy(id = 0, name = "${original.name} (Copy)", updatedAt = System.currentTimeMillis())
        val newId = automationDao.insert(copy)
        val clonedActions = actions.map { it.withAutomationId(newId) }
        saveAutomation(copy.copy(id = newId), clonedActions)
        newId
    }

    suspend fun deleteAutomation(id: Long) {
        tagDao?.clearAutomationReferences(id)
        actionDao.deleteAllForAutomation(id)
        automationDao.deleteById(id)
    }
}
