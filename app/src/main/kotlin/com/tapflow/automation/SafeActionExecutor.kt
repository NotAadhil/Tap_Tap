package com.tapflow.automation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ActionResult {
    data class Success(val message: String) : ActionResult
    data class Failure(val reason: String) : ActionResult
}

/**
 * Executes a single, safe, demonstrable automation action for a matching tag.
 */
class SafeActionExecutor(private val context: Context) {

    suspend fun executeAction(actionType: String, actionPayload: String): ActionResult =
        withContext(Dispatchers.Main) {
            when (actionType) {
                "OPEN_URL" -> executeOpenUrl(actionPayload)
                else -> ActionResult.Failure("Unsupported action type: $actionType")
            }
        }

    private fun executeOpenUrl(url: String): ActionResult {
        val validationError = ActionValidator.validateUrl(url)
        if (validationError != null) {
            return ActionResult.Failure("Invalid URL: $validationError")
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened URL: $url")
        } catch (e: ActivityNotFoundException) {
            ActionResult.Failure("No web browser or app found to handle URL: $url")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to open URL: ${e.message}")
        }
    }
}
