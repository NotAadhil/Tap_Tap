package com.tapflow.automation

/**
 * Domain representation of an automation action.
 */
sealed interface AutomationAction {
    val id: Long
    val automationId: Long
    val sortOrder: Int
    val isEnabled: Boolean

    fun describe(): String
    fun validate(): String?

    fun withSortOrder(newOrder: Int): AutomationAction = when (this) {
        is OpenApp -> copy(sortOrder = newOrder)
        is OpenUrl -> copy(sortOrder = newOrder)
        is ShowNotification -> copy(sortOrder = newOrder)
        is ShowMessage -> copy(sortOrder = newOrder)
        is ToggleFlashlight -> copy(sortOrder = newOrder)
        is CopyToClipboard -> copy(sortOrder = newOrder)
        is PlaySound -> copy(sortOrder = newOrder)
        is OpenSettings -> copy(sortOrder = newOrder)
        is SetRingerMode -> copy(sortOrder = newOrder)
        is ComposeSms -> copy(sortOrder = newOrder)
        is DelayAction -> copy(sortOrder = newOrder)
        is SendWebhook -> copy(sortOrder = newOrder)
        is TimeWindowFilter -> copy(sortOrder = newOrder)
        is SetDndMode -> copy(sortOrder = newOrder)
    }

    fun withEnabled(enabled: Boolean): AutomationAction = when (this) {
        is OpenApp -> copy(isEnabled = enabled)
        is OpenUrl -> copy(isEnabled = enabled)
        is ShowNotification -> copy(isEnabled = enabled)
        is ShowMessage -> copy(isEnabled = enabled)
        is ToggleFlashlight -> copy(isEnabled = enabled)
        is CopyToClipboard -> copy(isEnabled = enabled)
        is PlaySound -> copy(isEnabled = enabled)
        is OpenSettings -> copy(isEnabled = enabled)
        is SetRingerMode -> copy(isEnabled = enabled)
        is ComposeSms -> copy(isEnabled = enabled)
        is DelayAction -> copy(isEnabled = enabled)
        is SendWebhook -> copy(isEnabled = enabled)
        is TimeWindowFilter -> copy(isEnabled = enabled)
        is SetDndMode -> copy(isEnabled = enabled)
    }

    fun withAutomationId(newAutomationId: Long): AutomationAction = when (this) {
        is OpenApp -> copy(automationId = newAutomationId)
        is OpenUrl -> copy(automationId = newAutomationId)
        is ShowNotification -> copy(automationId = newAutomationId)
        is ShowMessage -> copy(automationId = newAutomationId)
        is ToggleFlashlight -> copy(automationId = newAutomationId)
        is CopyToClipboard -> copy(automationId = newAutomationId)
        is PlaySound -> copy(automationId = newAutomationId)
        is OpenSettings -> copy(automationId = newAutomationId)
        is SetRingerMode -> copy(automationId = newAutomationId)
        is ComposeSms -> copy(automationId = newAutomationId)
        is DelayAction -> copy(automationId = newAutomationId)
        is SendWebhook -> copy(automationId = newAutomationId)
        is TimeWindowFilter -> copy(automationId = newAutomationId)
        is SetDndMode -> copy(automationId = newAutomationId)
    }

    data class OpenApp(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val packageName: String,
        val appDisplayName: String
    ) : AutomationAction {
        override fun describe() = "Open $appDisplayName"
        override fun validate(): String? {
            return if (packageName.isBlank()) "Select an application" else null
        }
    }

    data class OpenUrl(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val url: String
    ) : AutomationAction {
        override fun describe() = "Open: $url"
        override fun validate(): String? {
            if (url.isBlank()) return "Enter a URL"
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return "URL must begin with http:// or https://"
            }
            return ActionValidator.validateUrl(url)
        }
    }

    data class ShowNotification(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val title: String,
        val body: String
    ) : AutomationAction {
        override fun describe() = "Notify: \"$title\""
        override fun validate(): String? {
            return if (title.isBlank()) "Enter a notification title" else null
        }
    }

    data class ShowMessage(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val message: String
    ) : AutomationAction {
        override fun describe() = "Message: \"$message\""
        override fun validate(): String? {
            return if (message.isBlank()) "Enter a message" else null
        }
    }

    data class ToggleFlashlight(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val enable: Boolean = true,
        val mode: String = if (enable) "ON" else "OFF"
    ) : AutomationAction {
        override fun describe() = when (mode) {
            "ON" -> "Turn Flashlight ON"
            "OFF" -> "Turn Flashlight OFF"
            "TOGGLE" -> "Toggle Flashlight (Flip-Flop)"
            else -> if (enable) "Turn Flashlight ON" else "Turn Flashlight OFF"
        }
        override fun validate(): String? = null
    }

    data class CopyToClipboard(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val text: String,
        val label: String = "TapFlow"
    ) : AutomationAction {
        override fun describe() = "Copy to Clipboard: \"${text.take(24)}${if (text.length > 24) "..." else ""}\""
        override fun validate(): String? {
            return if (text.isBlank()) "Enter text to copy" else null
        }
    }

    data class PlaySound(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val toneName: String = "Confirmation Beep"
    ) : AutomationAction {
        override fun describe() = "Play Sound ($toneName)"
        override fun validate(): String? {
            return if (toneName.isBlank()) "Select a tone sound" else null
        }
    }

    data class OpenSettings(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val settingType: String = "WIFI"
    ) : AutomationAction {
        override fun describe() = when (settingType) {
            "WIFI" -> "Open Wi-Fi Settings"
            "BLUETOOTH" -> "Open Bluetooth Settings"
            "NFC" -> "Open NFC Settings"
            "SOUND" -> "Open Sound Settings"
            "DISPLAY" -> "Open Display Settings"
            "BATTERY" -> "Open Battery Settings"
            else -> "Open System Settings"
        }
        override fun validate(): String? = null
    }

    data class SetRingerMode(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val ringerMode: String = "VIBRATE" // SILENT, VIBRATE, NORMAL
    ) : AutomationAction {
        override fun describe() = when (ringerMode) {
            "SILENT" -> "Set Ringer: Silent"
            "VIBRATE" -> "Set Ringer: Vibrate"
            "NORMAL" -> "Set Ringer: Normal (Loud)"
            else -> "Set Ringer Mode"
        }
        override fun validate(): String? = null
    }

    data class ComposeSms(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val phoneNumber: String = "",
        val message: String = ""
    ) : AutomationAction {
        override fun describe() = "Compose SMS to ${phoneNumber.ifBlank { "Contact" }}"
        override fun validate(): String? {
            return if (phoneNumber.isBlank() && message.isBlank()) "Enter a phone number or message" else null
        }
    }

    data class DelayAction(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val durationMs: Long = 1000L
    ) : AutomationAction {
        override fun describe() = "Wait ${(durationMs / 1000.0).toString().trimEnd('0').trimEnd('.')}s"
        override fun validate(): String? = if (durationMs < 100L) "Minimum delay is 100ms" else null
    }

    data class SendWebhook(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val url: String,
        val method: String = "GET", // "GET", "POST"
        val body: String = ""
    ) : AutomationAction {
        override fun describe() = "HTTP $method: ${url.take(28)}${if (url.length > 28) "..." else ""}"
        override fun validate(): String? {
            if (url.isBlank()) return "Enter a Webhook URL"
            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                return "URL must begin with http:// or https://"
            }
            return ActionValidator.validateUrl(url)
        }
    }

    data class TimeWindowFilter(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val startHour: Int = 9,
        val startMinute: Int = 0,
        val endHour: Int = 17,
        val endMinute: Int = 0
    ) : AutomationAction {
        override fun describe() = "Run only %02d:%02d - %02d:%02d".format(startHour, startMinute, endHour, endMinute)
        override fun validate(): String? {
            if (startHour !in 0..23 || endHour !in 0..23) return "Hours must be between 0 and 23"
            if (startMinute !in 0..59 || endMinute !in 0..59) return "Minutes must be between 0 and 59"
            return null
        }
    }

    data class SetDndMode(
        override val id: Long = 0,
        override val automationId: Long = 0,
        override val sortOrder: Int = 0,
        override val isEnabled: Boolean = true,
        val dndMode: String = "PRIORITY" // "PRIORITY", "TOTAL_SILENCE", "OFF"
    ) : AutomationAction {
        override fun describe() = when (dndMode) {
            "PRIORITY" -> "DND: Priority Only"
            "TOTAL_SILENCE" -> "DND: Total Silence"
            "OFF" -> "Turn DND OFF"
            else -> "Set DND Mode"
        }
        override fun validate(): String? = null
    }
}
