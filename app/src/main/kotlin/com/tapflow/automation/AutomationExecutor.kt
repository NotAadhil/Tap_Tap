package com.tapflow.automation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Executes a sequence of automation actions with debouncing, error isolation,
 * and result reporting.
 */
class AutomationExecutor(
    private val context: Context,
    private val channelId: String = "tapflow_automations"
) {

    private val _messageEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    private var lastExecutedTagKey: String = ""
    private var lastExecutedAt: Long = 0L
    private val debounceMs = 2000L

    suspend fun execute(tagKey: String, actions: List<AutomationAction>): List<ActionResult> {
        val now = System.currentTimeMillis()
        if (tagKey.isNotEmpty() && tagKey.equals(lastExecutedTagKey, ignoreCase = true) && (now - lastExecutedAt) < debounceMs) {
            return listOf(
                ActionResult.Skipped(
                    actionId = 0,
                    description = "Sequence Debounce",
                    reason = "Ignored rapid tap (< 2000ms)"
                )
            )
        }
        lastExecutedTagKey = tagKey
        lastExecutedAt = now

        // Trigger brief haptic pulse on scan execution
        vibrateBriefly()

        val results = mutableListOf<ActionResult>()
        val enabledActions = actions.filter { it.isEnabled }

        for (action in enabledActions) {
            try {
                runAction(action)
                results.add(ActionResult.Success(action.id, action.describe()))
            } catch (e: Exception) {
                results.add(
                    ActionResult.Failure(
                        action.id,
                        action.describe(),
                        e.message ?: "Action failed"
                    )
                )
            }
        }
        return results
    }

    private fun runAction(action: AutomationAction) {
        when (action) {
            is AutomationAction.OpenApp -> {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage(action.packageName)
                    ?: throw IllegalStateException("App '${action.appDisplayName}' (${action.packageName}) is not installed.")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.app.ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(
                            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    }
                } else null

                context.startActivity(intent, options?.toBundle())
            }

            is AutomationAction.OpenUrl -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.app.ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(
                            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    }
                } else null

                context.startActivity(intent, options?.toBundle())
            }

            is AutomationAction.ShowNotification -> {
                val notificationManager = NotificationManagerCompat.from(context)
                if (!notificationManager.areNotificationsEnabled()) {
                    throw IllegalStateException("Notifications are disabled for TapFlow in device Settings.")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        throw SecurityException("Notification permission not granted.")
                    }
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(action.title)
                    .setContentText(action.body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                try {
                    NotificationManagerCompat.from(context).notify(
                        System.currentTimeMillis().toInt(),
                        notification
                    )
                } catch (e: SecurityException) {
                    throw SecurityException("Cannot post notification: ${e.message}")
                }
            }

            is AutomationAction.ShowMessage -> {
                _messageEvents.tryEmit(action.message)
            }
        }
    }

    private fun vibrateBriefly() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration failure
        }
    }
}
