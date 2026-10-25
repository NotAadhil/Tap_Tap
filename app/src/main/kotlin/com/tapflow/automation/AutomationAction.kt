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
}
