package com.tapflow.automation

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Calendar

/**
 * Executes a sequence of [AutomationAction]s sequentially, recording results,
 * isolating errors, respecting a 2-second debounce, and handling background activity launches.
 */
class AutomationExecutor(
    private val context: Context,
    private val channelId: String = "tapflow_automations",
    private val isHapticEnabled: () -> Boolean = { false },
    private val getCooldownMs: () -> Long = { 5000L }
) {

    private val _messageEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    private var lastExecutedTagKey: String = ""
    private var lastExecutedAt: Long = 0L
    private var isTorchOn: Boolean = false

    suspend fun execute(tagKey: String, actions: List<AutomationAction>): List<ActionResult> {
        val now = System.currentTimeMillis()
        val cooldown = getCooldownMs()
        if (tagKey.isNotEmpty() && tagKey.equals(lastExecutedTagKey, ignoreCase = true) && (now - lastExecutedAt) < cooldown) {
            return listOf(
                ActionResult.Skipped(
                    actionId = 0,
                    description = "Sequence Debounce",
                    reason = "Ignored rapid tap (< ${cooldown}ms)"
                )
            )
        }
        lastExecutedTagKey = tagKey
        lastExecutedAt = now

        // Trigger optional secondary haptic feedback only if user explicitly turned it on
        if (isHapticEnabled()) {
            vibrateBriefly()
        }

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

    private suspend fun runAction(action: AutomationAction) {
        when (action) {
            is AutomationAction.OpenApp -> {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage(action.packageName)
                    ?: throw IllegalStateException("App '${action.appDisplayName}' (${action.packageName}) is not installed.")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.app.ActivityOptions.makeBasic().apply {
                        @Suppress("DEPRECATION")
                        setPendingIntentBackgroundActivityStartMode(
                            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    }
                } else null

                context.startActivity(intent, options?.toBundle())
            }

            is AutomationAction.OpenUrl -> {
                var targetUrl = action.url.trim()
                if (!targetUrl.startsWith("http://", ignoreCase = true) && !targetUrl.startsWith("https://", ignoreCase = true)) {
                    targetUrl = "https://$targetUrl"
                }
                val uri = Uri.parse(targetUrl)
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                // If URL targets YouTube, route directly to the official YouTube app if installed
                val isYouTube = targetUrl.contains("youtube.com", ignoreCase = true) ||
                        targetUrl.contains("youtu.be", ignoreCase = true)
                if (isYouTube) {
                    try {
                        val pm = context.packageManager
                        val ytIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube")
                        if (ytIntent.resolveActivity(pm) != null) {
                            intent.setPackage("com.google.android.youtube")
                        }
                    } catch (_: Exception) {
                        // Fall back to system default handler
                    }
                }

                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.app.ActivityOptions.makeBasic().apply {
                        @Suppress("DEPRECATION")
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
                    notificationManager.notify(
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

            is AutomationAction.ToggleFlashlight -> {
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                        ?: throw IllegalStateException("Torch hardware not available on this device.")
                    val shouldTurnOn = when (action.mode) {
                        "ON" -> true
                        "OFF" -> false
                        else -> !isTorchOn
                    }
                    cameraManager.setTorchMode(cameraId, shouldTurnOn)
                    isTorchOn = shouldTurnOn
                    _messageEvents.tryEmit(if (shouldTurnOn) "Flashlight turned ON" else "Flashlight turned OFF")
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to toggle flashlight: ${e.message}")
                }
            }

            is AutomationAction.CopyToClipboard -> {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText(action.label.ifBlank { "TapFlow" }, action.text)
                    clipboard?.setPrimaryClip(clip)
                    _messageEvents.tryEmit("Copied to clipboard: \"${action.text.take(20)}\"")
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to copy to clipboard: ${e.message}")
                }
            }

            is AutomationAction.PlaySound -> {
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                } catch (e: Exception) {
                    // Audio fallback
                }
            }

            is AutomationAction.OpenSettings -> {
                val actionIntent = when (action.settingType) {
                    "WIFI" -> Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    "BLUETOOTH" -> Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    "NFC" -> Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
                    "SOUND" -> Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    "DISPLAY" -> Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                    "BATTERY" -> Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    "TETHERING" -> Intent("android.settings.TETHER_SETTINGS")
                    else -> Intent(android.provider.Settings.ACTION_SETTINGS)
                }.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(actionIntent)
                _messageEvents.tryEmit(action.describe())
            }

            is AutomationAction.SetRingerMode -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    ?: throw IllegalStateException("Audio system not accessible")
                when (action.ringerMode) {
                    "SILENT" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    "VIBRATE" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    "NORMAL" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
                _messageEvents.tryEmit(action.describe())
            }

            is AutomationAction.ComposeSms -> {
                val uri = Uri.parse("smsto:${action.phoneNumber}")
                val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                    if (action.message.isNotBlank()) {
                        putExtra("sms_body", action.message)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
            }

            is AutomationAction.DelayAction -> {
                kotlinx.coroutines.delay(action.durationMs)
            }

            is AutomationAction.SendWebhook -> {
                withContext(Dispatchers.IO) {
                    val url = URL(action.url)
                    val conn = url.openConnection() as HttpURLConnection
                    try {
                        conn.connectTimeout = 6000
                        conn.readTimeout = 6000
                        conn.requestMethod = action.method.uppercase()
                        conn.setRequestProperty("User-Agent", "TapFlow/1.0 (Android)")
                        if (action.method.equals("POST", ignoreCase = true)) {
                            conn.doOutput = true
                            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            if (action.body.isNotBlank()) {
                                conn.outputStream.use { os ->
                                    os.write(action.body.toByteArray(StandardCharsets.UTF_8))
                                }
                            }
                        }
                        val code = conn.responseCode
                        if (code in 200..299) {
                            _messageEvents.tryEmit("Webhook Success (HTTP $code)")
                        } else {
                            throw IllegalStateException("Webhook HTTP $code")
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
            }

            is AutomationAction.TimeWindowFilter -> {
                val now = Calendar.getInstance()
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val startMinutes = action.startHour * 60 + action.startMinute
                val endMinutes = action.endHour * 60 + action.endMinute

                val inWindow = if (startMinutes <= endMinutes) {
                    currentMinutes in startMinutes..endMinutes
                } else {
                    currentMinutes >= startMinutes || currentMinutes <= endMinutes
                }

                if (!inWindow) {
                    throw IllegalStateException("Skipped: time outside allowed window (%02d:%02d - %02d:%02d)".format(action.startHour, action.startMinute, action.endHour, action.endMinute))
                }
            }

            is AutomationAction.SetDndMode -> {
                val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    ?: throw IllegalStateException("Notification service unavailable")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notifManager.isNotificationPolicyAccessGranted) {
                    val settingsIntent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    throw IllegalStateException("Do Not Disturb permission required. Please allow TapFlow in Settings.")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    when (action.dndMode) {
                        "PRIORITY" -> notifManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        "TOTAL_SILENCE" -> notifManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
                        "OFF" -> notifManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                }
                _messageEvents.tryEmit(action.describe())
            }
        }
    }

    private fun vibrateBriefly() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(30)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration failure
        }
    }
}
