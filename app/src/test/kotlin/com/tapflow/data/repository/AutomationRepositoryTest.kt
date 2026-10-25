package com.tapflow.data.repository

import com.tapflow.automation.AutomationAction
import com.tapflow.data.db.dao.ActionDao
import com.tapflow.data.db.dao.AutomationDao
import com.tapflow.data.db.entity.ActionEntity
import com.tapflow.data.db.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutomationRepositoryTest {

    private lateinit var fakeAutoDao: FakeAutomationDao
    private lateinit var fakeActionDao: FakeActionDao
    private lateinit var repository: AutomationRepository

    @Before
    fun setUp() {
        fakeAutoDao = FakeAutomationDao()
        fakeActionDao = FakeActionDao()
        repository = AutomationRepository(fakeAutoDao, fakeActionDao)
    }

    @Test
    fun saveAutomation_withActions_persistsInOrder() = runTest {
        val automation = AutomationEntity(name = "Study Mode")
        val actions = listOf(
            AutomationAction.ShowMessage(message = "Starting Study Mode"),
            AutomationAction.OpenUrl(url = "https://study.example.com")
        )

        val savedId = repository.saveAutomation(automation, actions)
        assertTrue("Automation ID must be generated", savedId > 0)

        val loadedAuto = repository.getAutomationById(savedId)
        assertNotNull(loadedAuto)
        assertEquals("Study Mode", loadedAuto?.name)

        val loadedActions = repository.getActions(savedId)
        assertEquals(2, loadedActions.size)
        assertEquals("Message: \"Starting Study Mode\"", loadedActions[0].describe())
        assertEquals(0, loadedActions[0].sortOrder)
        assertEquals("Open: https://study.example.com", loadedActions[1].describe())
        assertEquals(1, loadedActions[1].sortOrder)
    }

    @Test
    fun deleteAutomation_removesAutomationAndAssociatedActions() = runTest {
        val automation = AutomationEntity(name = "Routine")
        val actions = listOf(AutomationAction.ShowMessage(message = "Hi"))
        val id = repository.saveAutomation(automation, actions)

        repository.deleteAutomation(id)

        assertNull("Automation must be removed", repository.getAutomationById(id))
        assertTrue("Associated actions must be cleared", repository.getActions(id).isEmpty())
    }
}

private class FakeAutomationDao : AutomationDao {
    private val automations = mutableMapOf<Long, AutomationEntity>()
    private val _flow = MutableStateFlow<List<AutomationEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllFlow(): Flow<List<AutomationEntity>> = _flow
    override fun getAutomationCountFlow(): Flow<Int> = kotlinx.coroutines.flow.flow { emit(automations.size) }

    override suspend fun findById(id: Long): AutomationEntity? = automations[id]

    override suspend fun insert(automation: AutomationEntity): Long {
        val targetId = if (automation.id == 0L) nextId++ else automation.id
        val item = automation.copy(id = targetId)
        automations[targetId] = item
        _flow.update { automations.values.toList() }
        return targetId
    }

    override suspend fun update(automation: AutomationEntity) {
        automations[automation.id] = automation
        _flow.update { automations.values.toList() }
    }

    override suspend fun delete(automation: AutomationEntity) {
        automations.remove(automation.id)
        _flow.update { automations.values.toList() }
    }

    override suspend fun deleteById(id: Long) {
        automations.remove(id)
        _flow.update { automations.values.toList() }
    }
}

private class FakeActionDao : ActionDao {
    private val actions = mutableMapOf<Long, ActionEntity>()
    private var nextId = 1L

    override fun getActionsFlow(automationId: Long): Flow<List<ActionEntity>> {
        return kotlinx.coroutines.flow.flow {
            emit(actions.values.filter { it.automationId == automationId }.sortedBy { it.sortOrder })
        }
    }

    override suspend fun getActions(automationId: Long): List<ActionEntity> {
        return actions.values.filter { it.automationId == automationId }.sortedBy { it.sortOrder }
    }

    override suspend fun findById(id: Long): ActionEntity? = actions[id]

    override suspend fun insert(action: ActionEntity): Long {
        val targetId = if (action.id == 0L) nextId++ else action.id
        actions[targetId] = action.copy(id = targetId)
        return targetId
    }

    override suspend fun insertAll(items: List<ActionEntity>) {
        items.forEach { insert(it) }
    }

    override suspend fun update(action: ActionEntity) {
        actions[action.id] = action
    }

    override suspend fun delete(action: ActionEntity) {
        actions.remove(action.id)
    }

    override suspend fun deleteById(id: Long) {
        actions.remove(id)
    }

    override suspend fun deleteAllForAutomation(automationId: Long) {
        actions.entries.removeIf { it.value.automationId == automationId }
    }
}
