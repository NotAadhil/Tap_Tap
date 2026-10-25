package com.tapflow.nfc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tapflow.MainActivity
import com.tapflow.automation.ActionResult
import com.tapflow.automation.AutomationAction
import com.tapflow.automation.AutomationExecutor
import com.tapflow.data.db.entity.RegisteredTagEntity
import com.tapflow.data.repository.AutomationRepository
import com.tapflow.data.repository.ExecutionLogRepository
import com.tapflow.data.repository.TagRepository
import com.tapflow.nfc.model.TagData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface ScanEvent {
    data class Matched(
        val tag: RegisteredTagEntity,
        val result: ActionResult,
        val isExecuted: Boolean,
        val allResults: List<ActionResult> = listOf(result)
    ) : ScanEvent

    data class Unregistered(
        val tagData: TagData
    ) : ScanEvent

    data class Error(
        val message: String,
        val tagData: TagData? = null
    ) : ScanEvent

    data class Debounced(
        val tag: RegisteredTagEntity
    ) : ScanEvent
}

/**
 * Coordinates incoming tag reads: matches against registered tags in local persistence,
 * enforces debounce guards, runs automation sequences, and records execution history.
 */
class NfcDispatchCoordinator(
    private val tagRepository: TagRepository,
    private val automationRepository: AutomationRepository,
    private val automationExecutor: AutomationExecutor,
    private val executionLogRepository: ExecutionLogRepository,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _scanEvents = MutableSharedFlow<ScanEvent>(replay = 1)
    val scanEvents: SharedFlow<ScanEvent> = _scanEvents.asSharedFlow()

    private var lastTagKey: String? = null
    private var lastExecutionTimeMs: Long = 0L
    private val debounceWindowMs: Long = 500L

    fun onTagDetected(tagData: TagData) {
        scope.launch {
            // Guard 1: Random UID
            if (tagData.hasRandomUid) {
                _scanEvents.emit(
                    ScanEvent.Error(
                        message = "This tag has a dynamic random UID (starts with 0x08). It cannot be used as a reliable trigger.",
                        tagData = tagData
                    )
                )
                return@launch
            }

            // Guard 2: Payment/Security card
            if (tagData.isPaymentCard) {
                _scanEvents.emit(
                    ScanEvent.Error(
                        message = "Payment and banking cards are not supported for safety and privacy reasons.",
                        tagData = tagData
                    )
                )
                return@launch
            }

            // Match against registered tags in database
            val registeredTag = tagRepository.getTag(tagData.tagKey)

            if (registeredTag == null) {
                _scanEvents.emit(ScanEvent.Unregistered(tagData))
                postUnknownTagNotification(tagData)
            } else {
                if (!registeredTag.isEnabled) {
                    _scanEvents.emit(
                        ScanEvent.Matched(
                            tag = registeredTag,
                            result = ActionResult.Failure(reason = "Tag '${registeredTag.displayName}' is disabled."),
                            isExecuted = false
                        )
                    )
                    return@launch
                }

                // Debounce check
                val now = System.currentTimeMillis()
                if (registeredTag.tagKey.equals(lastTagKey, ignoreCase = true) && (now - lastExecutionTimeMs) < debounceWindowMs) {
                    _scanEvents.emit(ScanEvent.Debounced(registeredTag))
                    return@launch
                }

                lastTagKey = registeredTag.tagKey
                lastExecutionTimeMs = now

                val startTime = System.currentTimeMillis()

                // Check for multi-action automation sequence
                if (registeredTag.automationId != null) {
                    val automation = automationRepository.getAutomationById(registeredTag.automationId)
                    if (automation != null && automation.isEnabled) {
                        val actions = automationRepository.getActions(automation.id)
                        if (actions.isNotEmpty()) {
                            val results = automationExecutor.execute(registeredTag.tagKey, actions)
                            val duration = System.currentTimeMillis() - startTime
                            executionLogRepository.recordExecution(
                                tagKey = registeredTag.tagKey,
                                tagDisplayName = registeredTag.displayName,
                                automationName = automation.name,
                                durationMs = duration,
                                results = results
                            )

                            val primaryResult = results.firstOrNull { it is ActionResult.Failure }
                                ?: results.firstOrNull()
                                ?: ActionResult.Success(message = "Executed ${actions.size} actions for '${automation.name}'")

                            _scanEvents.emit(
                                ScanEvent.Matched(
                                    tag = registeredTag,
                                    result = primaryResult,
                                    isExecuted = true,
                                    allResults = results
                                )
                            )
                            return@launch
                        }
                    }
                }

                // Fallback to single action (e.g. OPEN_URL)
                if (registeredTag.actionPayload.isNotBlank()) {
                    val singleAction = AutomationAction.OpenUrl(
                        id = 1,
                        automationId = 0,
                        url = registeredTag.actionPayload
                    )
                    val results = automationExecutor.execute(registeredTag.tagKey, listOf(singleAction))
                    val duration = System.currentTimeMillis() - startTime
                    val primaryResult = results.firstOrNull()
                        ?: ActionResult.Success(message = "Executed action for '${registeredTag.displayName}'")

                    executionLogRepository.recordExecution(
                        tagKey = registeredTag.tagKey,
                        tagDisplayName = registeredTag.displayName,
                        automationName = "Quick Action",
                        durationMs = duration,
                        results = results
                    )

                    _scanEvents.emit(
                        ScanEvent.Matched(
                            tag = registeredTag,
                            result = primaryResult,
                            isExecuted = true,
                            allResults = results
                        )
                    )
                } else {
                    _scanEvents.emit(
                        ScanEvent.Matched(
                            tag = registeredTag,
                            result = ActionResult.Success(message = "Scanned '${registeredTag.displayName}', but no actions assigned yet."),
                            isExecuted = false
                        )
                    )
                }
            }
        }
    }

    private fun postUnknownTagNotification(tagData: TagData) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, "tapflow_automations")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New NFC Tag Detected")
                .setContentText("Tap to register '${tagData.rawUidHex}' in TapFlow")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(999, notification)
        } catch (_: Exception) {
            // Notifications may be denied or blocked
        }
    }
}
