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
    fun toggleFlashlightAction_roundTripSerialization_succeeds() {
        val originalOn = AutomationAction.ToggleFlashlight(
            id = 50,
            automationId = 4,
            sortOrder = 0,
            isEnabled = true,
            enable = true
        )
        val entityOn = ActionMapper.toEntity(originalOn)
        assertEquals(ActionMapper.TYPE_TOGGLE_FLASHLIGHT, entityOn.type)
        val restoredOn = ActionMapper.toDomain(entityOn) as AutomationAction.ToggleFlashlight
        assertTrue(restoredOn.enable)
        assertEquals("Turn Flashlight ON", restoredOn.describe())
        assertNull(restoredOn.validate())

        val originalOff = AutomationAction.ToggleFlashlight(enable = false)
        val entityOff = ActionMapper.toEntity(originalOff)
        val restoredOff = ActionMapper.toDomain(entityOff) as AutomationAction.ToggleFlashlight
        assertEquals(false, restoredOff.enable)
        assertEquals("Turn Flashlight OFF", restoredOff.describe())
    }

    @Test
    fun copyToClipboardAction_roundTripSerialization_succeeds() {
        val original = AutomationAction.CopyToClipboard(
            id = 60,
            automationId = 4,
            sortOrder = 1,
            isEnabled = true,
            text = "GuestWiFiPassword123!",
            label = "Wi-Fi Pass"
        )
        val entity = ActionMapper.toEntity(original)
        assertEquals(ActionMapper.TYPE_COPY_TO_CLIPBOARD, entity.type)

        val restored = ActionMapper.toDomain(entity) as AutomationAction.CopyToClipboard
        assertEquals("GuestWiFiPassword123!", restored.text)
        assertEquals("Wi-Fi Pass", restored.label)
        assertEquals("Copy to Clipboard: \"GuestWiFiPassword123!\"", restored.describe())
        assertNull(restored.validate())
    }

    @Test
    fun playSoundAction_roundTripSerialization_succeeds() {
        val original = AutomationAction.PlaySound(
            id = 70,
            automationId = 4,
            sortOrder = 2,
            isEnabled = true,
            toneName = "Beep"
        )
        val entity = ActionMapper.toEntity(original)
        assertEquals(ActionMapper.TYPE_PLAY_SOUND, entity.type)

        val restored = ActionMapper.toDomain(entity) as AutomationAction.PlaySound
        assertEquals("Beep", restored.toneName)
        assertEquals("Play Sound (Beep)", restored.describe())
        assertNull(restored.validate())
    }

    @Test
    fun newActions_roundTripSerialization_succeeds() {
        val settings = AutomationAction.OpenSettings(settingType = "WIFI")
        val settingsEntity = ActionMapper.toEntity(settings)
        assertEquals(ActionMapper.TYPE_OPEN_SETTINGS, settingsEntity.type)
        val restoredSettings = ActionMapper.toDomain(settingsEntity) as AutomationAction.OpenSettings
        assertEquals("WIFI", restoredSettings.settingType)

        val ringer = AutomationAction.SetRingerMode(ringerMode = "SILENT")
        val ringerEntity = ActionMapper.toEntity(ringer)
        assertEquals(ActionMapper.TYPE_SET_RINGER, ringerEntity.type)
        val restoredRinger = ActionMapper.toDomain(ringerEntity) as AutomationAction.SetRingerMode
        assertEquals("SILENT", restoredRinger.ringerMode)

        val sms = AutomationAction.ComposeSms(phoneNumber = "+123456", message = "Hi")
        val smsEntity = ActionMapper.toEntity(sms)
        assertEquals(ActionMapper.TYPE_COMPOSE_SMS, smsEntity.type)
        val restoredSms = ActionMapper.toDomain(smsEntity) as AutomationAction.ComposeSms
        assertEquals("+123456", restoredSms.phoneNumber)
        assertEquals("Hi", restoredSms.message)

        val delay = AutomationAction.DelayAction(durationMs = 2000L)
        val delayEntity = ActionMapper.toEntity(delay)
        assertEquals(ActionMapper.TYPE_DELAY, delayEntity.type)
        val restoredDelay = ActionMapper.toDomain(delayEntity) as AutomationAction.DelayAction
        assertEquals(2000L, restoredDelay.durationMs)
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

        val emptyClipboard = AutomationAction.CopyToClipboard(text = "")
        assertEquals("Enter text to copy", emptyClipboard.validate())

        val emptySound = AutomationAction.PlaySound(toneName = "")
        assertEquals("Select a tone sound", emptySound.validate())
    }
}
