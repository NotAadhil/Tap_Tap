package com.tapflow.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapflow.AppContainer
import com.tapflow.automation.ActionResult
import com.tapflow.automation.ActionValidator
import com.tapflow.automation.SafeActionExecutor
import com.tapflow.data.db.entity.RegisteredTagEntity
import com.tapflow.data.repository.TagRepository
import com.tapflow.nfc.NfcDispatchCoordinator
import com.tapflow.nfc.NfcManager
import com.tapflow.nfc.ScanEvent
import com.tapflow.nfc.model.TagData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isNfcAvailable: Boolean = true,
    val isNfcEnabled: Boolean = true,
    val registeredTags: List<RegisteredTagEntity> = emptyList(),
    val activeScanEvent: ScanEvent? = null,
    val pendingRegistrationTag: TagData? = null,
    val userMessage: String? = null,
    val errorMessage: String? = null
)

class MainScreenViewModel(
    private val tagRepository: TagRepository,
    private val nfcManager: NfcManager,
    private val coordinator: NfcDispatchCoordinator,
    private val actionExecutor: SafeActionExecutor
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _pendingRegistrationTag = MutableStateFlow<TagData?>(null)
    private val _currentScanEvent = MutableStateFlow<ScanEvent?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        tagRepository.allTagsFlow,
        _currentScanEvent,
        _pendingRegistrationTag,
        _userMessage,
        _errorMessage
    ) { tags, scanEvent, pendingTag, msg, err ->
        MainUiState(
            isNfcAvailable = nfcManager.isNfcAvailable,
            isNfcEnabled = nfcManager.isNfcEnabled,
            registeredTags = tags,
            activeScanEvent = scanEvent,
            pendingRegistrationTag = pendingTag,
            userMessage = msg,
            errorMessage = err
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainUiState(
            isNfcAvailable = nfcManager.isNfcAvailable,
            isNfcEnabled = nfcManager.isNfcEnabled
        )
    )

    init {
        viewModelScope.launch {
            coordinator.scanEvents.collect { event ->
                _currentScanEvent.value = event
                when (event) {
                    is ScanEvent.Unregistered -> {
                        _pendingRegistrationTag.value = event.tagData
                        _errorMessage.value = null
                    }
                    is ScanEvent.Error -> {
                        _errorMessage.value = event.message
                    }
                    is ScanEvent.Matched -> {
                        val resultStr = when (val r = event.result) {
                            is ActionResult.Success -> r.message
                            is ActionResult.Failure -> "Failed: ${r.reason}"
                        }
                        _userMessage.value = "Triggered ${event.tag.displayName}: $resultStr"
                    }
                    is ScanEvent.Debounced -> {
                        _userMessage.value = "Ignored rapid re-scan (${event.tag.displayName})"
                    }
                }
            }
        }
    }

    fun registerPendingTag(displayName: String, url: String) {
        val pending = _pendingRegistrationTag.value ?: return

        val nameError = ActionValidator.validateTagName(displayName)
        if (nameError != null) {
            _errorMessage.value = nameError
            return
        }

        val urlError = ActionValidator.validateUrl(url)
        if (urlError != null) {
            _errorMessage.value = urlError
            return
        }

        viewModelScope.launch {
            val result = tagRepository.registerTag(
                tagKey = pending.tagKey,
                displayName = displayName,
                rawUidHex = pending.rawUidHex,
                techList = pending.techList.joinToString(","),
                actionType = "OPEN_URL",
                actionPayload = url
            )

            result.fold(
                onSuccess = {
                    _userMessage.value = "Tag \"${it.displayName}\" registered successfully!"
                    _pendingRegistrationTag.value = null
                    _errorMessage.value = null
                },
                onFailure = {
                    _errorMessage.value = it.message ?: "Failed to register tag"
                }
            )
        }
    }

    fun dismissPendingRegistration() {
        _pendingRegistrationTag.value = null
    }

    fun deleteTag(tag: RegisteredTagEntity) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            _userMessage.value = "Deleted tag \"${tag.displayName}\""
        }
    }

    fun testAction(tag: RegisteredTagEntity) {
        viewModelScope.launch {
            val result = actionExecutor.executeAction(tag.actionType, tag.actionPayload)
            when (result) {
                is ActionResult.Success -> _userMessage.value = result.message
                is ActionResult.Failure -> _errorMessage.value = result.reason
            }
        }
    }

    fun simulateTag(tagData: TagData) {
        nfcManager.simulateTag(tagData)
    }

    fun clearMessages() {
        _userMessage.value = null
        _errorMessage.value = null
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainScreenViewModel(
                tagRepository = container.tagRepository,
                nfcManager = container.nfcManager,
                coordinator = container.coordinator,
                actionExecutor = container.actionExecutor
            ) as T
        }
    }
}
