package com.tapflow.data.repository

import com.tapflow.data.db.dao.RegisteredTagDao
import com.tapflow.data.db.entity.RegisteredTagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagRepositoryTest {

    private lateinit var fakeDao: FakeRegisteredTagDao
    private lateinit var repository: TagRepository

    @Before
    fun setUp() {
        fakeDao = FakeRegisteredTagDao()
        repository = TagRepository(fakeDao)
    }

    @Test
    fun registerTag_success_persistsTagAndEmitsUpdate() = runTest {
        val result = repository.registerTag(
            tagKey = "04A1B2C3:NfcA,Ndef",
            displayName = "Desk Tag",
            rawUidHex = "04A1B2C3",
            techList = "NfcA,Ndef",
            actionType = "OPEN_URL",
            actionPayload = "https://example.com"
        )

        assertTrue("Registration must succeed for a new tag", result.isSuccess)
        val saved = repository.getTag("04A1B2C3:NfcA,Ndef")
        assertNotNull("Tag must be findable by its tagKey", saved)
        assertEquals("Desk Tag", saved?.displayName)
        assertEquals("04A1B2C3", saved?.rawUidHex)

        val allTags = repository.allTagsFlow.first()
        assertEquals(1, allTags.size)
    }

    @Test
    fun registerTag_duplicateRegistration_rejectedWithError() = runTest {
        // First registration
        val firstResult = repository.registerTag(
            tagKey = "04A1B2C3:NfcA",
            displayName = "Desk Tag",
            rawUidHex = "04A1B2C3",
            techList = "NfcA"
        )
        assertTrue(firstResult.isSuccess)

        // Duplicate registration attempt with identical tagKey
        assertTrue("Duplicate detection check must return true", repository.isDuplicate("04A1B2C3:NfcA"))
        val secondResult = repository.registerTag(
            tagKey = "04A1B2C3:NfcA",
            displayName = "Second Desk Tag",
            rawUidHex = "04A1B2C3",
            techList = "NfcA"
        )

        assertTrue("Duplicate registration must fail", secondResult.isFailure)
        assertEquals("Tag is already registered.", secondResult.exceptionOrNull()?.message)
    }

    @Test
    fun getTag_nonExistent_returnsNull() = runTest {
        val tag = repository.getTag("UNKNOWN_KEY")
        assertNull("Non-existent tagKey must return null", tag)
    }

    @Test
    fun deleteTag_removesFromStorage() = runTest {
        val result = repository.registerTag(
            tagKey = "04A1B2C3:NfcA",
            displayName = "Temporary Tag",
            rawUidHex = "04A1B2C3",
            techList = "NfcA"
        )
        val entity = result.getOrThrow()

        repository.deleteTag(entity)
        assertNull("Tag must no longer exist after deletion", repository.getTag("04A1B2C3:NfcA"))
        assertFalse(repository.isDuplicate("04A1B2C3:NfcA"))
        assertEquals(0, repository.allTagsFlow.first().size)
    }
}

/**
 * In-memory test double verifying DAO contract and reactive Flow emissions.
 */
private class FakeRegisteredTagDao : RegisteredTagDao {
    private val tags = mutableMapOf<String, RegisteredTagEntity>()
    private val _tagsFlow = MutableStateFlow<List<RegisteredTagEntity>>(emptyList())

    override fun getAllTagsFlow(): Flow<List<RegisteredTagEntity>> = _tagsFlow

    override fun getTagCountFlow(): Flow<Int> = kotlinx.coroutines.flow.flow { emit(tags.size) }

    override suspend fun findByTagKey(tagKey: String): RegisteredTagEntity? = tags[tagKey]

    override suspend fun countByTagKey(tagKey: String): Int = if (tags.containsKey(tagKey)) 1 else 0

    override suspend fun insert(tag: RegisteredTagEntity) {
        if (tags.containsKey(tag.tagKey)) {
            throw IllegalStateException("Duplicate primary key: ${tag.tagKey}")
        }
        tags[tag.tagKey] = tag
        emitUpdate()
    }

    override suspend fun update(tag: RegisteredTagEntity) {
        tags[tag.tagKey] = tag
        emitUpdate()
    }

    override suspend fun updateDisplayName(tagKey: String, displayName: String) {
        tags[tagKey]?.let {
            tags[tagKey] = it.copy(displayName = displayName)
            emitUpdate()
        }
    }

    override suspend fun updateAutomationId(tagKey: String, automationId: Long?) {
        tags[tagKey]?.let {
            tags[tagKey] = it.copy(automationId = automationId)
            emitUpdate()
        }
    }

    override suspend fun updateEnabled(tagKey: String, isEnabled: Boolean) {
        tags[tagKey]?.let {
            tags[tagKey] = it.copy(isEnabled = isEnabled)
            emitUpdate()
        }
    }

    override suspend fun delete(tag: RegisteredTagEntity) {
        tags.remove(tag.tagKey)
        emitUpdate()
    }

    override suspend fun deleteByKey(tagKey: String) {
        tags.remove(tagKey)
        emitUpdate()
    }

    private fun emitUpdate() {
        _tagsFlow.update { tags.values.toList() }
    }
}
