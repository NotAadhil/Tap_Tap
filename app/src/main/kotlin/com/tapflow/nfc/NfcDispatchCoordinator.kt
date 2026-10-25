package com.tapflow.nfc

import com.tapflow.automation.ActionResult
import com.tapflow.automation.SafeActionExecutor
import com.tapflow.data.db.entity.RegisteredTagEntity
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
        val isExecuted: Boolean
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
 * enforces debounce guards, and delegates to the action executor.
 */
class NfcDispatchCoordinator(
    private val tagRepository: TagRepository,
    private val actionExecutor: SafeActionExecutor,
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
            } else {
                if (!registeredTag.isEnabled) {
                    _scanEvents.emit(
                        ScanEvent.Matched(
                            tag = registeredTag,
                            result = ActionResult.Failure("Tag is disabled."),
                            isExecuted = false
                        )
                    )
                    return@launch
                }

                // Debounce check
                val now = System.currentTimeMillis()
                if (registeredTag.tagKey == lastTagKey && (now - lastExecutionTimeMs) < debounceWindowMs) {
                    _scanEvents.emit(ScanEvent.Debounced(registeredTag))
                    return@launch
                }

                lastTagKey = registeredTag.tagKey
                lastExecutionTimeMs = now

                // Execute safe demonstrable action
                val actionResult = actionExecutor.executeAction(
                    actionType = registeredTag.actionType,
                    actionPayload = registeredTag.actionPayload
                )

                _scanEvents.emit(
                    ScanEvent.Matched(
                        tag = registeredTag,
                        result = actionResult,
                        isExecuted = true
                    )
                )
            }
        }
    }
}
