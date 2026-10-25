package com.tapflow.automation

import com.tapflow.data.db.entity.ActionEntity
import org.json.JSONObject

/**
 * Maps between Room database ActionEntity and typed AutomationAction domain objects.
 */
object ActionMapper {

    const val TYPE_OPEN_APP = "OPEN_APP"
    const val TYPE_OPEN_URL = "OPEN_URL"
    const val TYPE_SHOW_NOTIFICATION = "SHOW_NOTIFICATION"
    const val TYPE_SHOW_MESSAGE = "SHOW_MESSAGE"

    fun toDomain(entity: ActionEntity): AutomationAction {
        val json = try {
            JSONObject(entity.configJson)
        } catch (e: Exception) {
            JSONObject()
        }

        return when (entity.type) {
            TYPE_OPEN_APP -> AutomationAction.OpenApp(
                id = entity.id,
                automationId = entity.automationId,
                sortOrder = entity.sortOrder,
                isEnabled = entity.isEnabled,
                packageName = json.optString("packageName", ""),
                appDisplayName = json.optString("appDisplayName", "App")
            )
            TYPE_OPEN_URL -> AutomationAction.OpenUrl(
                id = entity.id,
                automationId = entity.automationId,
                sortOrder = entity.sortOrder,
                isEnabled = entity.isEnabled,
                url = json.optString("url", "")
            )
            TYPE_SHOW_NOTIFICATION -> AutomationAction.ShowNotification(
                id = entity.id,
                automationId = entity.automationId,
                sortOrder = entity.sortOrder,
                isEnabled = entity.isEnabled,
                title = json.optString("title", ""),
                body = json.optString("body", "")
            )
            TYPE_SHOW_MESSAGE -> AutomationAction.ShowMessage(
                id = entity.id,
                automationId = entity.automationId,
                sortOrder = entity.sortOrder,
                isEnabled = entity.isEnabled,
                message = json.optString("message", "")
            )
            else -> AutomationAction.ShowMessage(
                id = entity.id,
                automationId = entity.automationId,
                sortOrder = entity.sortOrder,
                isEnabled = entity.isEnabled,
                message = "Unsupported action type: ${entity.type}"
            )
        }
    }

    fun toEntity(domain: AutomationAction): ActionEntity {
        val json = JSONObject()
        val type = when (domain) {
            is AutomationAction.OpenApp -> {
                json.put("packageName", domain.packageName)
                json.put("appDisplayName", domain.appDisplayName)
                TYPE_OPEN_APP
            }
            is AutomationAction.OpenUrl -> {
                json.put("url", domain.url)
                TYPE_OPEN_URL
            }
            is AutomationAction.ShowNotification -> {
                json.put("title", domain.title)
                json.put("body", domain.body)
                TYPE_SHOW_NOTIFICATION
            }
            is AutomationAction.ShowMessage -> {
                json.put("message", domain.message)
                TYPE_SHOW_MESSAGE
            }
        }

        return ActionEntity(
            id = domain.id,
            automationId = domain.automationId,
            type = type,
            configJson = json.toString(),
            sortOrder = domain.sortOrder,
            isEnabled = domain.isEnabled,
            label = domain.describe()
        )
    }
}
