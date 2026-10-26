package com.tapflow.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.tapflow.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapflow.automation.AutomationAction
import com.tapflow.data.db.entity.AutomationEntity
import com.tapflow.data.db.entity.RegisteredTagEntity
import com.tapflow.nfc.model.TagData
import com.tapflow.ui.screens.automations.AutomationBuilderDialog
import com.tapflow.ui.screens.automations.AutomationScreenContent
import com.tapflow.ui.screens.dialogs.ActionConfigDialog
import com.tapflow.ui.screens.dialogs.ActionPickerDialog
import com.tapflow.ui.screens.dialogs.RegisterTagDialog
import com.tapflow.ui.screens.dialogs.SimulateTagDialog
import com.tapflow.ui.screens.dialogs.TagToolsDialog
import com.tapflow.ui.screens.dialogs.VirtualNfcConfigDialog
import com.tapflow.ui.screens.history.HistoryScreenContent
import com.tapflow.ui.screens.home.HomeScreenContent
import com.tapflow.ui.screens.settings.SettingsScreenContent
import com.tapflow.ui.screens.tags.TagDetailDialog
import com.tapflow.ui.screens.tags.TagListScreenContent

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showSimulateDialog by remember { mutableStateOf(false) }

    MainScreenContent(
        state = state,
        onSelectTab = { viewModel.selectTab(it) },
        onOpenSimulateDialog = { showSimulateDialog = true },
        onDismissSimulateDialog = { showSimulateDialog = false },
        onSimulateTag = {
            viewModel.simulateTag(it)
            showSimulateDialog = false
        },
        onDismissRegistration = { viewModel.dismissPendingRegistration() },
        onConfirmRegistration = { name, autoId, url -> viewModel.registerPendingTag(name, autoId, url) },
        onSelectTagForDetail = { viewModel.openTagDetail(it) },
        onCloseTagDetail = { viewModel.closeTagDetail() },
        onSaveTagName = { key, name -> viewModel.updateTagName(key, name) },
        onToggleTagEnabled = { key, enabled -> viewModel.toggleTagEnabled(key, enabled) },
        onAssignAutomationToTag = { key, id -> viewModel.assignAutomationToTag(key, id) },
        onDeleteTag = { viewModel.deleteTag(it) },
        onNewAutomation = { viewModel.openNewAutomationBuilder() },
        onEditAutomation = { viewModel.openEditAutomationBuilder(it) },
        onDeleteAutomation = { viewModel.deleteAutomation(it) },
        onCloseAutomationBuilder = { viewModel.closeAutomationBuilder() },
        onOpenActionPicker = { viewModel.openActionPicker() },
        onCloseActionPicker = { viewModel.closeActionPicker() },
        onSelectActionTypeToConfigure = { viewModel.selectActionTypeToConfigure(it) },
        onOpenEditAction = { viewModel.openEditAction(it) },
        onCloseActionConfig = { viewModel.closeActionConfig() },
        onSaveConfiguredAction = { viewModel.saveConfiguredAction(it) },
        onMoveActionUp = { viewModel.moveActionUp(it) },
        onMoveActionDown = { viewModel.moveActionDown(it) },
        onToggleActionEnabled = { idx, enabled -> viewModel.toggleActionEnabled(idx, enabled) },
        onRemoveAction = { viewModel.removeAction(it) },
        onTestCurrentAutomation = { viewModel.testCurrentAutomation() },
        onSaveCurrentAutomation = { viewModel.saveCurrentAutomation(it) },
        onClearHistory = { viewModel.clearExecutionHistory() },
        onToggleHapticFeedback = { viewModel.toggleHapticFeedback(it) },
        onSetCooldownSeconds = { viewModel.setCooldownSeconds(it) },
        onTestTag = { viewModel.testTag(it) },
        onTestAutomation = { viewModel.testAutomationById(it.id) },
        onDuplicateAutomation = { viewModel.duplicateAutomation(it) },
        onExportBackup = { viewModel.exportBackupToJson() },
        onImportBackup = { viewModel.importBackupFromJson(it) },
        onRequestNotificationPermission = onRequestNotificationPermission,
        onClearMessages = { viewModel.clearMessages() },
        showSimulateDialog = showSimulateDialog,
        onToggleHce = { viewModel.toggleHceEmitting(it) },
        onOpenHceConfig = { viewModel.openHceConfigDialog() },
        onCloseHceConfig = { viewModel.closeHceConfigDialog() },
        onSaveHceConfig = { type, content -> viewModel.updateHcePayload(type, content) },
        onOpenTagTools = { viewModel.openTagTools(it) },
        onCloseTagTools = { viewModel.closeTagTools() },
        onSelectTagToolsTab = { viewModel.setTagToolsTab(it) },
        onStartTagWrite = { viewModel.startTagWrite(it) },
        onStartCloneRead = { viewModel.startTagCloneRead() },
        onStartCloneWrite = { viewModel.startTagCloneWrite() },
        onStartLockTag = { viewModel.startTagLock() },
        onCancelTagOperation = { viewModel.cancelTagOperation() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    state: MainUiState,
    onSelectTab: (AppTab) -> Unit = {},
    onOpenSimulateDialog: () -> Unit = {},
    onDismissSimulateDialog: () -> Unit = {},
    onSimulateTag: (TagData) -> Unit = {},
    onDismissRegistration: () -> Unit = {},
    onConfirmRegistration: (String, Long?, String) -> Unit = { _, _, _ -> },
    onSelectTagForDetail: (RegisteredTagEntity) -> Unit = {},
    onCloseTagDetail: () -> Unit = {},
    onSaveTagName: (String, String) -> Unit = { _, _ -> },
    onToggleTagEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onAssignAutomationToTag: (String, Long?) -> Unit = { _, _ -> },
    onDeleteTag: (RegisteredTagEntity) -> Unit = {},
    onNewAutomation: () -> Unit = {},
    onEditAutomation: (AutomationEntity) -> Unit = {},
    onDeleteAutomation: (AutomationEntity) -> Unit = {},
    onCloseAutomationBuilder: () -> Unit = {},
    onOpenActionPicker: () -> Unit = {},
    onCloseActionPicker: () -> Unit = {},
    onSelectActionTypeToConfigure: (String) -> Unit = {},
    onOpenEditAction: (AutomationAction) -> Unit = {},
    onCloseActionConfig: () -> Unit = {},
    onSaveConfiguredAction: (AutomationAction) -> Unit = {},
    onMoveActionUp: (Int) -> Unit = {},
    onMoveActionDown: (Int) -> Unit = {},
    onToggleActionEnabled: (Int, Boolean) -> Unit = { _, _ -> },
    onRemoveAction: (Int) -> Unit = {},
    onTestCurrentAutomation: () -> Unit = {},
    onSaveCurrentAutomation: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onToggleHapticFeedback: (Boolean) -> Unit = {},
    onSetCooldownSeconds: (Long) -> Unit = {},
    onTestTag: (RegisteredTagEntity) -> Unit = {},
    onTestAutomation: (AutomationEntity) -> Unit = {},
    onDuplicateAutomation: (AutomationEntity) -> Unit = {},
    onExportBackup: () -> String = { "" },
    onImportBackup: (String) -> Unit = {} ,
    onRequestNotificationPermission: () -> Unit = {},
    onClearMessages: () -> Unit = {},
    showSimulateDialog: Boolean = false,
    onToggleHce: (Boolean) -> Unit = {},
    onOpenHceConfig: () -> Unit = {},
    onCloseHceConfig: () -> Unit = {},
    onSaveHceConfig: (String, String) -> Unit = { _, _ -> },
    onOpenTagTools: (Int) -> Unit = {},
    onCloseTagTools: () -> Unit = {},
    onSelectTagToolsTab: (Int) -> Unit = {},
    onStartTagWrite: (android.nfc.NdefMessage) -> Unit = {},
    onStartCloneRead: () -> Unit = {},
    onStartCloneWrite: () -> Unit = {},
    onStartLockTag: () -> Unit = {},
    onCancelTagOperation: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage, state.errorMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tapflow_mark),
                                    contentDescription = "TapFlow",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TapFlow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = "Artisan NFC Automation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onOpenSimulateDialog,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Simulate", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = state.currentTab == tab,
                        onClick = { onSelectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(tab.title, fontSize = 11.sp) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.currentTab == AppTab.AUTOMATIONS) {
                FloatingActionButton(onClick = onNewAutomation) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New Automation",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.currentTab) {
                AppTab.HOME -> {
                    HomeScreenContent(
                        isNfcAvailable = state.isNfcAvailable,
                        isNfcEnabled = state.isNfcEnabled,
                        registeredTags = state.registeredTags,
                        automations = state.automations,
                        executionLogs = state.executionLogs,
                        activeScanEvent = state.activeScanEvent,
                        onNavigateTab = onSelectTab,
                        onOpenSimulate = onOpenSimulateDialog,
                        onOpenTagDetail = onSelectTagForDetail,
                        onTestTag = onTestTag,
                        isHceEmittingEnabled = state.isHceEmittingEnabled,
                        hcePayloadType = state.hcePayloadType,
                        hcePayloadContent = state.hcePayloadContent,
                        onToggleHce = onToggleHce,
                        onOpenHceConfig = onOpenHceConfig
                    )
                }
                AppTab.TAGS -> {
                    TagListScreenContent(
                        tags = state.registeredTags,
                        automations = state.automations,
                        onSelectTag = onSelectTagForDetail,
                        onToggleEnabled = onToggleTagEnabled,
                        onDeleteTag = onDeleteTag,
                        onTestTag = onTestTag,
                        onOpenTagTools = onOpenTagTools
                    )
                }
                AppTab.AUTOMATIONS -> {
                    AutomationScreenContent(
                        automations = state.automations,
                        onNewAutomation = onNewAutomation,
                        onEditAutomation = onEditAutomation,
                        onDeleteAutomation = onDeleteAutomation,
                        onTestAutomation = onTestAutomation,
                        onDuplicateAutomation = onDuplicateAutomation
                    )
                }
                AppTab.HISTORY -> {
                    HistoryScreenContent(
                        logs = state.executionLogs,
                        onClearHistory = onClearHistory
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreenContent(
                        isNfcAvailable = state.isNfcAvailable,
                        isNfcEnabled = state.isNfcEnabled,
                        isHapticFeedbackEnabled = state.isHapticFeedbackEnabled,
                        cooldownSeconds = state.cooldownSeconds,
                        onToggleHapticFeedback = onToggleHapticFeedback,
                        onSetCooldownSeconds = onSetCooldownSeconds,
                        onClearHistory = onClearHistory,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup,
                        isHceEmittingEnabled = state.isHceEmittingEnabled,
                        hcePayloadType = state.hcePayloadType,
                        hcePayloadContent = state.hcePayloadContent,
                        onToggleHce = onToggleHce,
                        onOpenHceConfig = onOpenHceConfig
                    )
                }
            }
        }
    }

    // Modal: Register Tag Dialog
    state.pendingRegistrationTag?.let { pendingTag ->
        RegisterTagDialog(
            tagData = pendingTag,
            automations = state.automations,
            onDismiss = onDismissRegistration,
            onConfirm = onConfirmRegistration
        )
    }

    // Modal: Tag Detail Dialog
    state.selectedTagForDetail?.let { tag ->
        TagDetailDialog(
            tag = tag,
            automations = state.automations,
            onDismiss = onCloseTagDetail,
            onSaveName = { newName -> onSaveTagName(tag.tagKey, newName) },
            onToggleEnabled = { enabled -> onToggleTagEnabled(tag.tagKey, enabled) },
            onAssignAutomation = { autoId -> onAssignAutomationToTag(tag.tagKey, autoId) },
            onDelete = { onDeleteTag(tag) }
        )
    }

    // Modal: Automation Builder Dialog
    if (state.showAutomationBuilder && state.editingAutomation != null) {
        AutomationBuilderDialog(
            automation = state.editingAutomation,
            actions = state.editingActions,
            onDismiss = onCloseAutomationBuilder,
            onAddAction = onOpenActionPicker,
            onEditAction = onOpenEditAction,
            onMoveUp = onMoveActionUp,
            onMoveDown = onMoveActionDown,
            onToggleAction = onToggleActionEnabled,
            onRemoveAction = onRemoveAction,
            onTestRun = onTestCurrentAutomation,
            onSave = onSaveCurrentAutomation
        )
    }

    // Modal: Action Picker Dialog
    if (state.showActionPicker) {
        ActionPickerDialog(
            onDismiss = onCloseActionPicker,
            onSelectType = onSelectActionTypeToConfigure
        )
    }

    // Modal: Action Config Dialog
    if (state.showActionConfig && state.actionTypeToConfigure != null) {
        ActionConfigDialog(
            actionType = state.actionTypeToConfigure,
            installedApps = state.installedApps,
            existingAction = state.actionToEdit,
            onDismiss = onCloseActionConfig,
            onSave = onSaveConfiguredAction
        )
    }

    // Modal: NFC Simulation Dialog
    if (showSimulateDialog) {
        SimulateTagDialog(
            onDismiss = onDismissSimulateDialog,
            onSimulate = onSimulateTag
        )
    }

    // Modal: Virtual NFC Beacon Config Dialog
    if (state.showHceConfigDialog) {
        VirtualNfcConfigDialog(
            currentType = state.hcePayloadType,
            currentContent = state.hcePayloadContent,
            onDismiss = onCloseHceConfig,
            onSave = onSaveHceConfig
        )
    }

    // Modal: Tag Tools (Write, Clone, Lock) Dialog
    if (state.tagTools.isOpen) {
        TagToolsDialog(
            selectedTab = state.tagTools.selectedTab,
            isWaitingForTag = state.tagTools.isWaitingForTag,
            isSuccess = state.tagTools.isSuccess,
            statusMessage = state.tagTools.statusMessage,
            clonedPayload = state.tagTools.clonedPayload,
            installedApps = state.installedApps,
            onSelectTab = onSelectTagToolsTab,
            onStartWrite = onStartTagWrite,
            onStartCloneRead = onStartCloneRead,
            onStartCloneWrite = onStartCloneWrite,
            onStartLock = onStartLockTag,
            onCancelOperation = onCancelTagOperation,
            onDismiss = onCloseTagTools
        )
    }
}
