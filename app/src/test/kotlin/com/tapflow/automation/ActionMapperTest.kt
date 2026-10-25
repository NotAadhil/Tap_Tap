package com.tapflow.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionMapperTest {

    @Test
    fun openAppAction_roundTripSerialization_succeeds() {
        val original = AutomationAction.OpenApp(
            id = 10,
            automationId = 1,
            sortOrder = 0,
            isEnabled = true,
            packageName = "com.spotify.music",
            appDisplayName = "Spotify"
        )

        val entity = ActionMapper.toEntity(original)
        assertEquals(ActionMapper.TYPE_OPEN_APP, entity.type)
        assertEquals(10L, entity.id)
        assertEquals(1L, entity.automationId)
        assertTrue(entity.configJson.contains("com.spotify.music"))

        val restored = ActionMapper.toDomain(entity)
        assertTrue(restored is AutomationAction.OpenApp)
        val casted = restored as AutomationAction.OpenApp
        assertEquals("com.spotify.music", casted.packageName)
        assertEquals("Spotify", casted.appDisplayName)
        assertEquals("Open Spotify", casted.describe())
        assertNull(casted.validate())
    }

    @Test
    fun openUrlAction_roundTripSerialization_succeeds() {
        val original = AutomationAction.OpenUrl(
            id = 20,
            automationId = 1,
            sortOrder = 1,
            isEnabled = true,
            url = "https://developer.android.com"
        )

        val entity = ActionMapper.toEntity(original)
        assertEquals(ActionMapper.TYPE_OPEN_URL, entity.type)
        assertTrue(entity.configJson.contains("https://developer.android.com"))

        val restored = ActionMapper.toDomain(entity)
        assertTrue(restored is AutomationAction.OpenUrl)
        val casted = restored as AutomationAction.OpenUrl
        assertEquals("https://developer.android.com", casted.url)
        assertEquals("Open: https://developer.android.com", casted.describe())
        assertNull(casted.validate())
    }

    @Test
    fun showNotificationAction_roundTripSerialization_succeeds() {
        val original = AutomationAction.ShowNotification(
            id = 30,
            automationId = 2,
            sortOrder = 0,
            isEnabled = true,
            title = "Focus Time",
            body = "Study session initiated"
        )

        val entity = ActionMapper.toEntity(original)
        assertEquals(ActionMapper.TYPE_SHOW_NOTIFICATION, entity.type)

        val restored = ActionMapper.toDomain(entity)
        assertTrue(restored is AutomationAction.ShowNotification)
        val casted = restored as AutomationAction.ShowNotification
        assertEquals("Focus Time", casted.title)
        assertEquals("Study session initiated", casted.body)
        assertEquals("Notify: \"Focus Time\"", casted.describe())
        assertNull(casted.validate())
    }

    @Test
    fun showMessageAction_roundTripSerialization_succeeds() {
        val original = AutomationAction.ShowMessage(
            id = 40,
            automationId = 3,
            sortOrder = 2,
            isEnabled = false,
            message = "Workout started!"
        )

        val entity = ActionMapper.toEntity(original)
        assertEquals(ActionMapper.TYPE_SHOW_MESSAGE, entity.type)
        assertEquals(false, entity.isEnabled)

        val restored = ActionMapper.toDomain(entity)
        assertTrue(restored is AutomationAction.ShowMessage)
        val casted = restored as AutomationAction.ShowMessage
        assertEquals("Workout started!", casted.message)
        assertEquals("Message: \"Workout started!\"", casted.describe())
        assertNull(casted.validate())
    }

    @Test
    fun actionValidation_detectsErrors() {
        val invalidUrl = AutomationAction.OpenUrl(url = "not-a-url")
        assertEquals("URL must begin with http:// or https://", invalidUrl.validate())

        val emptyApp = AutomationAction.OpenApp(packageName = "", appDisplayName = "")
        assertEquals("Select an application", emptyApp.validate())

        val emptyNotif = AutomationAction.ShowNotification(title = "", body = "test")
        assertEquals("Enter a notification title", emptyNotif.validate())

        val emptyMsg = AutomationAction.ShowMessage(message = "")
        assertEquals("Enter a message", emptyMsg.validate())
    }
}
