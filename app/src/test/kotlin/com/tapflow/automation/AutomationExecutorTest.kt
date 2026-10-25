package com.tapflow.automation

import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutomationExecutorTest {

    private lateinit var executor: AutomationExecutor

    @Before
    fun setUp() {
        val dummyContext = DummyContext()
        executor = AutomationExecutor(dummyContext)
    }

    @Test
    fun execute_debounce_skipsRapidDuplicateScans() = runTest {
        val action = AutomationAction.ShowMessage(message = "Hello")

        val firstRun = executor.execute("TAG_A", listOf(action))
        assertEquals(1, firstRun.size)
        assertTrue("First scan must succeed", firstRun[0] is ActionResult.Success)

        // Immediate second tap on same tag (< 5000ms)
        val secondRun = executor.execute("TAG_A", listOf(action))
        assertEquals(1, secondRun.size)
        assertTrue("Rapid second scan must be debounced and skipped", secondRun[0] is ActionResult.Skipped)
        assertTrue((secondRun[0] as ActionResult.Skipped).reason.contains("5000ms"))
    }

    @Test
    fun execute_differentTagKey_doesNotDebounce() = runTest {
        val action = AutomationAction.ShowMessage(message = "Hello")

        val firstRun = executor.execute("TAG_A", listOf(action))
        assertTrue(firstRun[0] is ActionResult.Success)

        val secondRun = executor.execute("TAG_B", listOf(action))
        assertTrue("Different tag must execute immediately", secondRun[0] is ActionResult.Success)
    }

    @Test
    fun execute_disabledActions_areSkipped() = runTest {
        val disabledAction = AutomationAction.ShowMessage(message = "Hidden", isEnabled = false)
        val enabledAction = AutomationAction.ShowMessage(message = "Visible", isEnabled = true)

        val results = executor.execute("TAG_C", listOf(disabledAction, enabledAction))
        assertEquals(1, results.size)
        assertEquals("Message: \"Visible\"", results[0].description)
    }
}

private class DummyContext : ContextWrapper(null) {
    override fun getSystemService(name: String): Any? = null
}
