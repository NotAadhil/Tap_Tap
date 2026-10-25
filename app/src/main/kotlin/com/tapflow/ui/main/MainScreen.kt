package com.tapflow.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapflow.data.db.entity.RegisteredTagEntity
import com.tapflow.nfc.ScanEvent
import com.tapflow.nfc.TagIdentifier
import com.tapflow.nfc.model.TagData

@Composable
fun MainScreen(viewModel: MainScreenViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showSimulateDialog by remember { mutableStateOf(false) }

    MainScreenContent(
        state = state,
        onClearMessages = { viewModel.clearMessages() },
        onOpenSimulateDialog = { showSimulateDialog = true },
        onTestAction = { viewModel.testAction(it) },
        onDeleteTag = { viewModel.deleteTag(it) },
        onDismissRegistration = { viewModel.dismissPendingRegistration() },
        onConfirmRegistration = { name, url -> viewModel.registerPendingTag(name, url) },
        showSimulateDialog = showSimulateDialog,
        onDismissSimulate = { showSimulateDialog = false },
        onSimulateTag = { tagData ->
            viewModel.simulateTag(tagData)
            showSimulateDialog = false
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    state: MainUiState,
    onClearMessages: () -> Unit = {},
    onOpenSimulateDialog: () -> Unit = {},
    onTestAction: (RegisteredTagEntity) -> Unit = {},
    onDeleteTag: (RegisteredTagEntity) -> Unit = {},
    onDismissRegistration: () -> Unit = {},
    onConfirmRegistration: (String, String) -> Unit = { _, _ -> },
    showSimulateDialog: Boolean = false,
    onDismissSimulate: () -> Unit = {},
    onSimulateTag: (TagData) -> Unit = {}
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "⚡",
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TapFlow",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onOpenSimulateDialog,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Simulate Tap", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // NFC Hardware Status Banner
            NfcStatusBanner(
                isAvailable = state.isNfcAvailable,
                isEnabled = state.isNfcEnabled
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active Scan / Feedback card
            LiveScanFeedbackCard(
                scanEvent = state.activeScanEvent,
                isNfcReady = state.isNfcAvailable && state.isNfcEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Registered Tags Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registered Tags (${state.registeredTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags List or Empty State
            if (state.registeredTags.isEmpty()) {
                EmptyTagsCard()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.registeredTags, key = { it.tagKey }) { tag ->
                        RegisteredTagCard(
                            tag = tag,
                            onTest = { onTestAction(tag) },
                            onDelete = { onDeleteTag(tag) }
                        )
                    }
                }
            }
        }
    }

    // Tag Registration Dialog
    state.pendingRegistrationTag?.let { pendingTag ->
        RegisterTagDialog(
            tagData = pendingTag,
            onDismiss = onDismissRegistration,
            onConfirm = onConfirmRegistration
        )
    }

    // Simulation Dialog (for emulator / manual testing)
    if (showSimulateDialog) {
        SimulateTagDialog(
            onDismiss = onDismissSimulate,
            onSimulate = onSimulateTag
        )
    }
}

private data class BannerData(
    val bgColor: Color,
    val textColor: Color,
    val text: String,
    val badge: String
)

@Composable
private fun NfcStatusBanner(isAvailable: Boolean, isEnabled: Boolean) {
    val banner = when {
        !isAvailable -> BannerData(
            bgColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = "NFC Hardware: Not detected (Emulator or unsupported device)",
            badge = "ℹ️"
        )
        !isEnabled -> BannerData(
            bgColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer,
            text = "NFC is turned OFF in Android Settings",
            badge = "⚠️"
        )
        else -> BannerData(
            bgColor = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            text = "NFC Ready — Ready to scan physical tags",
            badge = "✅"
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = banner.bgColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = banner.badge, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = banner.text, color = banner.textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LiveScanFeedbackCard(scanEvent: ScanEvent?, isNfcReady: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Scanner Status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            when (scanEvent) {
                null -> {
                    Text(
                        text = if (isNfcReady) "Hold an NFC tag against the back of your phone to scan."
                        else "NFC scanner idle. Use 'Simulate Tap' above if testing on emulator.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                is ScanEvent.Matched -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Matched: ${scanEvent.tag.displayName}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Action: ${scanEvent.tag.actionType} (${scanEvent.tag.actionPayload})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                is ScanEvent.Unregistered -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🆕", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "New Tag Detected (UID: ${scanEvent.tagData.rawUidHex})",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Tech: ${scanEvent.tagData.techSummary}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is ScanEvent.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❌", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = scanEvent.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is ScanEvent.Debounced -> {
                    Text(
                        text = "Ignored rapid tap (${scanEvent.tag.displayName}) within 500ms debounce guard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisteredTagCard(
    tag: RegisteredTagEntity,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tag.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            Text(
                text = "Key: ${tag.tagKey}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${tag.actionType}: ${tag.actionPayload}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onTest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("▶ Test Action", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyTagsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🏷️", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No tags registered yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap a physical NFC tag to your phone or use 'Simulate Tap' above to register your first trigger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RegisterTagDialog(
    tagData: TagData,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://developer.android.com") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New NFC Tag") },
        text = {
            Column {
                Text(
                    text = "UID: ${tagData.rawUidHex}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Technologies: ${tagData.techSummary}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag Name (e.g. Study Mode)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Action URL to Open") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, url) },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("Save Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SimulateTagDialog(
    onDismiss: () -> Unit,
    onSimulate: (TagData) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate NFC Scan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Choose an NFC tag preset to simulate a hardware tap:",
                    fontSize = 12.sp
                )

                Button(
                    onClick = {
                        val bytes = byteArrayOf(0x04, 0xA1.toByte(), 0xB2.toByte(), 0xC3.toByte())
                        val techs = arrayOf("android.nfc.tech.NfcA", "android.nfc.tech.Ndef")
                        onSimulate(TagIdentifier.parse(bytes, techs))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1. Standard NFC-A Tag (04A1B2C3)")
                }

                Button(
                    onClick = {
                        val bytes = byteArrayOf(0x04, 0xE5.toByte(), 0xF6.toByte(), 0x77.toByte())
                        val techs = arrayOf("android.nfc.tech.NfcA", "android.nfc.tech.MifareUltralight")
                        onSimulate(TagIdentifier.parse(bytes, techs))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2. Second Tag (04E5F677)")
                }

                OutlinedButton(
                    onClick = {
                        // Starts with 0x08 -> Random UID
                        val bytes = byteArrayOf(0x08, 0x11, 0x22, 0x33)
                        val techs = arrayOf("android.nfc.tech.NfcA")
                        onSimulate(TagIdentifier.parse(bytes, techs))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("3. Random UID Tag (0x08...) [Must Reject]")
                }

                OutlinedButton(
                    onClick = {
                        // IsoDep alone -> Payment Card
                        val bytes = byteArrayOf(0x55, 0x66, 0x77, 0x88.toByte())
                        val techs = arrayOf("android.nfc.tech.IsoDep")
                        onSimulate(TagIdentifier.parse(bytes, techs))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("4. Payment Card (IsoDep only) [Must Reject]")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
