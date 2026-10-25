package com.tapflow.ui.main

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapflow.AppContainer
import com.tapflow.automation.ActionResult
import com.tapflow.automation.ActionValidator
import com.tapflow.automation.AutomationAction
import com.tapflow.automation.AutomationExecutor
import com.tapflow.automation.InstalledAppInfo
import com.tapflow.data.db.entity.AutomationEntity
import com.tapflow.data.db.entity.ExecutionLogEntity
import com.tapflow.data.db.entity.RegisteredTagEntity
import com.tapflow.data.repository.AutomationRepository
import com.tapflow.data.repository.ExecutionLogRepository
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
    val currentTab: AppTab = AppTab.HOME,
    val isNfcAvailable: Boolean = true,
    val isNfcEnabled: Boolean = true,
    val registeredTags: List<RegisteredTagEntity> = emptyList(),
    val automations: List<AutomationEntity> = emptyList(),
    val executionLogs: List<ExecutionLogEntity> = emptyList(),
    val activeScanEvent: ScanEvent? = null,
    val pendingRegistrationTag: TagData? = null,
    val selectedTagForDetail: RegisteredTagEntity? = null,
    val editingAutomation: AutomationEntity? = null,
    val editingActions: List<AutomationAction> = emptyList(),
    val showAutomationBuilder: Boolean = false,
    val showActionPicker: Boolean = false,
    val showActionConfig: Boolean = false,
    val actionTypeToConfigure: String? = null,
    val actionToEdit: AutomationAction? = null,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val userMessage: String? = null,
    val errorMessage: String? = null,
    val isHapticFeedbackEnabled: Boolean = false,
    val cooldownSeconds: Long = 5L
)

class MainScreenViewModel(
    private val tagRepository: TagRepository,
    private val automationRepository: AutomationRepository,
    private val executionLogRepository: ExecutionLogRepository,
    private val automationExecutor: AutomationExecutor,
    private val nfcManager: NfcManager,
    private val coordinator: NfcDispatchCoordinator,
    private val context: Context,
    private val preferences: SharedPreferences
) : ViewModel() {

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    private val _pendingRegistrationTag = MutableStateFlow<TagData?>(null)
    private val _currentScanEvent = MutableStateFlow<ScanEvent?>(null)
    private val _selectedTagForDetail = MutableStateFlow<RegisteredTagEntity?>(null)

    // Automation Builder states
    private val _editingAutomation = MutableStateFlow<AutomationEntity?>(null)
    private val _editingActions = MutableStateFlow<List<AutomationAction>>(emptyList())
    private val _showAutomationBuilder = MutableStateFlow(false)
    private val _showActionPicker = MutableStateFlow(false)
    private val _showActionConfig = MutableStateFlow(false)
    private val _actionTypeToConfigure = MutableStateFlow<String?>(null)
    private val _actionToEdit = MutableStateFlow<AutomationAction?>(null)

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isHapticFeedbackEnabled = MutableStateFlow(
        preferences.getBoolean("haptic_feedback_enabled", false)
    )
    private val _cooldownSeconds = MutableStateFlow(
        preferences.getLong("tag_cooldown_ms", 5000L) / 1000L
    )

    val uiState: StateFlow<MainUiState> = combine(
        combine(
            _currentTab,
            tagRepository.allTagsFlow,
            automationRepository.getAllAutomationsFlow(),
            executionLogRepository.getRecentLogsFlow()
        ) { tab, tags, autos, logs ->
            Tuple4(tab, tags, autos, logs)
        },
        combine(
            _currentScanEvent,
            _pendingRegistrationTag,
            _selectedTagForDetail,
            _editingAutomation,
            _editingActions
        ) { scan, pending, detail, editAuto, actions ->
            Tuple5(scan, pending, detail, editAuto, actions)
        },
        combine(
            _showAutomationBuilder,
            _showActionPicker,
            _showActionConfig,
            _actionTypeToConfigure,
            _actionToEdit
        ) { showBuilder, showPicker, showConfig, actionType, actionEdit ->
            Tuple5(showBuilder, showPicker, showConfig, actionType, actionEdit)
        },
        combine(
            _installedApps,
            _userMessage,
            _errorMessage,
            _isHapticFeedbackEnabled,
            _cooldownSeconds
        ) { apps, msg, err, haptic, cooldown ->
            Tuple5(apps, msg, err, haptic, cooldown)
        }
    ) { (tab, tags, autos, logs), (scan, pending, detail, editAuto, actions), (showBuilder, showPicker, showConfig, actionType, actionEdit), (apps, msg, err, haptic, cooldown) ->
        MainUiState(
            currentTab = tab,
            isNfcAvailable = nfcManager.isNfcAvailable,
            isNfcEnabled = nfcManager.isNfcEnabled,
            registeredTags = tags,
            automations = autos,
            executionLogs = logs,
            activeScanEvent = scan,
            pendingRegistrationTag = pending,
            selectedTagForDetail = detail,
            editingAutomation = editAuto,
            editingActions = actions,
            showAutomationBuilder = showBuilder,
            showActionPicker = showPicker,
            showActionConfig = showConfig,
            actionTypeToConfigure = actionType,
            actionToEdit = actionEdit,
            installedApps = apps,
            userMessage = msg,
            errorMessage = err,
            isHapticFeedbackEnabled = haptic,
            cooldownSeconds = cooldown
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainUiState(
            isNfcAvailable = nfcManager.isNfcAvailable,
            isNfcEnabled = nfcManager.isNfcEnabled,
            isHapticFeedbackEnabled = preferences.getBoolean("haptic_feedback_enabled", false),
            cooldownSeconds = preferences.getLong("tag_cooldown_ms", 5000L) / 1000L
        )
    )

    init {
        // Collect NFC scan events
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
                            is ActionResult.Skipped -> "Skipped: ${r.reason}"
                        }
                        _userMessage.value = "Triggered ${event.tag.displayName}: $resultStr"
                    }
                    is ScanEvent.Debounced -> {
                        _userMessage.value = "Ignored rapid re-scan (${event.tag.displayName})"
                    }
                }
            }
        }

        // Collect message events from AutomationExecutor (ShowMessage)
        viewModelScope.launch {
            automationExecutor.messageEvents.collect { msg ->
                _userMessage.value = msg
            }
        }

        // Query installed launcher applications in background
        viewModelScope.launch {
            _installedApps.value = InstalledAppInfo.queryLauncherApps(context)
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- Tag Operations ---

    fun registerPendingTag(displayName: String, automationId: Long?, url: String) {
        val pending = _pendingRegistrationTag.value ?: return

        val nameError = ActionValidator.validateTagName(displayName)
        if (nameError != null) {
            _errorMessage.value = nameError
            return
        }

        if (automationId == null && url.isNotBlank()) {
            val urlError = ActionValidator.validateUrl(url)
            if (urlError != null) {
                _errorMessage.value = urlError
                return
            }
        }

        viewModelScope.launch {
            val result = tagRepository.registerTag(
                tagKey = pending.tagKey,
                displayName = displayName,
                rawUidHex = pending.rawUidHex,
                techList = pending.techList.joinToString(","),
                automationId = automationId,
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

    fun openTagDetail(tag: RegisteredTagEntity) {
        _selectedTagForDetail.value = tag
    }

    fun closeTagDetail() {
        _selectedTagForDetail.value = null
    }

    fun updateTagName(tagKey: String, newName: String) {
        val nameError = ActionValidator.validateTagName(newName)
        if (nameError != null) {
            _errorMessage.value = nameError
            return
        }
        viewModelScope.launch {
            tagRepository.updateDisplayName(tagKey, newName)
            _selectedTagForDetail.value = _selectedTagForDetail.value?.copy(displayName = newName)
            _userMessage.value = "Renamed tag to \"$newName\""
        }
    }

    fun toggleTagEnabled(tagKey: String, isEnabled: Boolean) {
        viewModelScope.launch {
            tagRepository.updateEnabled(tagKey, isEnabled)
            _selectedTagForDetail.value = _selectedTagForDetail.value?.copy(isEnabled = isEnabled)
            _userMessage.value = if (isEnabled) "Tag enabled" else "Tag disabled"
        }
    }

    fun assignAutomationToTag(tagKey: String, automationId: Long?) {
        viewModelScope.launch {
            tagRepository.updateAutomationId(tagKey, automationId)
            _selectedTagForDetail.value = _selectedTagForDetail.value?.copy(automationId = automationId)
            _userMessage.value = if (automationId != null) "Automation linked!" else "Automation unlinked"
        }
    }

    fun deleteTag(tag: RegisteredTagEntity) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            if (_selectedTagForDetail.value?.tagKey == tag.tagKey) {
                _selectedTagForDetail.value = null
            }
            _userMessage.value = "Deleted tag \"${tag.displayName}\""
        }
    }

    // --- Automation Builder Operations ---

    fun openNewAutomationBuilder() {
        _editingAutomation.value = AutomationEntity(name = "New Automation")
        _editingActions.value = emptyList()
        _showAutomationBuilder.value = true
    }

    fun openEditAutomationBuilder(automation: AutomationEntity) {
        viewModelScope.launch {
            _editingAutomation.value = automation
            _editingActions.value = automationRepository.getActions(automation.id)
            _showAutomationBuilder.value = true
        }
    }

    fun closeAutomationBuilder() {
        _showAutomationBuilder.value = false
        _editingAutomation.value = null
        _editingActions.value = emptyList()
    }

    fun openActionPicker() {
        _showActionPicker.value = true
    }

    fun closeActionPicker() {
        _showActionPicker.value = false
    }

    fun selectActionTypeToConfigure(type: String) {
        _showActionPicker.value = false
        _actionTypeToConfigure.value = type
        _actionToEdit.value = null
        _showActionConfig.value = true
    }

    fun openEditAction(action: AutomationAction) {
        _actionToEdit.value = action
        _actionTypeToConfigure.value = when (action) {
            is AutomationAction.OpenApp -> "OPEN_APP"
            is AutomationAction.OpenUrl -> "OPEN_URL"
            is AutomationAction.ToggleFlashlight -> "TOGGLE_FLASHLIGHT"
            is AutomationAction.CopyToClipboard -> "COPY_TO_CLIPBOARD"
            is AutomationAction.ShowNotification -> "SHOW_NOTIFICATION"
            is AutomationAction.PlaySound -> "PLAY_SOUND"
            is AutomationAction.ShowMessage -> "SHOW_MESSAGE"
        }
        _showActionConfig.value = true
    }

    fun closeActionConfig() {
        _showActionConfig.value = false
        _actionTypeToConfigure.value = null
        _actionToEdit.value = null
    }

    fun saveConfiguredAction(action: AutomationAction) {
        val currentList = _editingActions.value.toMutableList()
        val editTarget = _actionToEdit.value
        if (editTarget != null) {
            val index = currentList.indexOfFirst { it.id == editTarget.id && it.sortOrder == editTarget.sortOrder }
            if (index != -1) {
                currentList[index] = action
            } else {
                currentList.add(action)
            }
        } else {
            currentList.add(action)
        }
        _editingActions.value = currentList
        closeActionConfig()
    }

    fun moveActionUp(index: Int) {
        if (index <= 0) return
        val list = _editingActions.value.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        _editingActions.value = list.mapIndexed { i, action -> action.withSortOrder(i) }
    }

    fun moveActionDown(index: Int) {
        val list = _editingActions.value.toMutableList()
        if (index >= list.size - 1) return
        val item = list.removeAt(index)
        list.add(index + 1, item)
        _editingActions.value = list.mapIndexed { i, action -> action.withSortOrder(i) }
    }

    fun toggleActionEnabled(index: Int, isEnabled: Boolean) {
        val list = _editingActions.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].withEnabled(isEnabled)
            _editingActions.value = list
        }
    }

    fun removeAction(index: Int) {
        val list = _editingActions.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _editingActions.value = list.mapIndexed { i, action -> action.withSortOrder(i) }
        }
    }

    fun saveCurrentAutomation(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Enter an automation name"
            return
        }
        val current = _editingAutomation.value ?: return
        val actions = _editingActions.value

        viewModelScope.launch {
            val updatedAutomation = current.copy(name = name.trim())
            automationRepository.saveAutomation(updatedAutomation, actions)
            _userMessage.value = "Saved automation \"${updatedAutomation.name}\""
            closeAutomationBuilder()
        }
    }

    fun testCurrentAutomation() {
        val actions = _editingActions.value
        if (actions.isEmpty()) {
            _userMessage.value = "Add at least one action to test"
            return
        }
        viewModelScope.launch {
            val results = automationExecutor.execute("TEST_PREVIEW", actions)
            val successCount = results.count { it is ActionResult.Success }
            _userMessage.value = "Executed test: $successCount/${actions.size} actions succeeded"
        }
    }

    fun testAutomationById(automationId: Long) {
        viewModelScope.launch {
            val actions = automationRepository.getActions(automationId)
            if (actions.isEmpty()) {
                _userMessage.value = "This automation has no actions."
                return@launch
            }
            val results = automationExecutor.execute("TEST_ID_$automationId", actions)
            val successCount = results.count { it is ActionResult.Success }
            _userMessage.value = "Executed test: $successCount/${actions.size} actions succeeded"
        }
    }

    fun testTag(tag: RegisteredTagEntity) {
        viewModelScope.launch {
            if (tag.automationId != null) {
                testAutomationById(tag.automationId)
            } else if (tag.actionPayload.isNotBlank()) {
                val singleAction = AutomationAction.OpenUrl(url = tag.actionPayload)
                automationExecutor.execute("TEST_TAG_${tag.tagKey}", listOf(singleAction))
                _userMessage.value = "Tested action for '${tag.displayName}'"
            } else {
                _userMessage.value = "Tag has no action assigned yet."
            }
        }
    }

    fun deleteAutomation(automation: AutomationEntity) {
        viewModelScope.launch {
            automationRepository.deleteAutomation(automation.id)
            _userMessage.value = "Deleted automation \"${automation.name}\""
        }
    }

    // --- History Operations ---

    fun clearExecutionHistory() {
        viewModelScope.launch {
            executionLogRepository.clearHistory()
            _userMessage.value = "Execution history cleared"
        }
    }

    // --- Settings Operations ---

    fun toggleHapticFeedback(enabled: Boolean) {
        preferences.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
        _isHapticFeedbackEnabled.value = enabled
    }

    fun setCooldownSeconds(seconds: Long) {
        preferences.edit().putLong("tag_cooldown_ms", seconds * 1000L).apply()
        _cooldownSeconds.value = seconds
        _userMessage.value = "Scan cooldown set to ${seconds}s"
    }

    // --- Simulator & Testing ---

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
                automationRepository = container.automationRepository,
                executionLogRepository = container.executionLogRepository,
                automationExecutor = container.automationExecutor,
                nfcManager = container.nfcManager,
                coordinator = container.coordinator,
                context = container.context,
                preferences = container.preferences
            ) as T
        }
    }
}

// Helper tuples for combine()
private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
