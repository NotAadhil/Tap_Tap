package com.tapflow.automation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes a single, safe, demonstrable automation action for a matching tag.
 */
class SafeActionExecutor(private val context: Context) {

    suspend fun executeAction(actionType: String, actionPayload: String): ActionResult =
        withContext(Dispatchers.Main) {
            when (actionType) {
                "OPEN_URL" -> executeOpenUrl(actionPayload)
                else -> ActionResult.Failure(reason = "Unsupported action type: $actionType")
            }
        }

    private fun executeOpenUrl(url: String): ActionResult {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            cleanUrl = "https://$cleanUrl"
        }
        val validationError = ActionValidator.validateUrl(cleanUrl)
        if (validationError != null) {
            return ActionResult.Failure(reason = "Invalid URL: $validationError")
        }

        return try {
            val uri = Uri.parse(cleanUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (cleanUrl.contains("youtube.com", ignoreCase = true) || cleanUrl.contains("youtu.be", ignoreCase = true)) {
                val pm = context.packageManager
                val ytIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube")
                if (ytIntent.resolveActivity(pm) != null) {
                    intent.setPackage("com.google.android.youtube")
                }
            }
            context.startActivity(intent)
            ActionResult.Success(message = "Opened URL: $cleanUrl")
        } catch (e: ActivityNotFoundException) {
            ActionResult.Failure(reason = "No web browser or app found to handle URL: $cleanUrl")
        } catch (e: Exception) {
            ActionResult.Failure(reason = "Failed to open URL: ${e.message}")
        }
    }
}
