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
    }

    fun withEnabled(enabled: Boolean): AutomationAction = when (this) {
        is OpenApp -> copy(isEnabled = enabled)
        is OpenUrl -> copy(isEnabled = enabled)
        is ShowNotification -> copy(isEnabled = enabled)
        is ShowMessage -> copy(isEnabled = enabled)
        is ToggleFlashlight -> copy(isEnabled = enabled)
        is CopyToClipboard -> copy(isEnabled = enabled)
        is PlaySound -> copy(isEnabled = enabled)
    }

    fun withAutomationId(newAutomationId: Long): AutomationAction = when (this) {
        is OpenApp -> copy(automationId = newAutomationId)
        is OpenUrl -> copy(automationId = newAutomationId)
        is ShowNotification -> copy(automationId = newAutomationId)
        is ShowMessage -> copy(automationId = newAutomationId)
        is ToggleFlashlight -> copy(automationId = newAutomationId)
        is CopyToClipboard -> copy(automationId = newAutomationId)
        is PlaySound -> copy(automationId = newAutomationId)
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
        val enable: Boolean = true
    ) : AutomationAction {
        override fun describe() = if (enable) "Turn Flashlight ON" else "Turn Flashlight OFF"
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
}
