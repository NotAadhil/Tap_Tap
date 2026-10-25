package com.tapflow.ui.screens.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapflow.automation.ActionValidator
import com.tapflow.automation.AutomationAction
import com.tapflow.automation.InstalledAppInfo
import com.tapflow.data.db.entity.AutomationEntity
import com.tapflow.nfc.TagIdentifier
import com.tapflow.nfc.model.TagData

/**
 * Dialog for registering a newly scanned NFC tag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterTagDialog(
    tagData: TagData,
    automations: List<AutomationEntity>,
    onDismiss: () -> Unit,
    onConfirm: (displayName: String, automationId: Long?, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedAutomationId by remember { mutableStateOf<Long?>(null) }
    var quickUrl by remember { mutableStateOf("https://") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Nfc,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Register NFC Tag", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Detected Tag Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Hardware UID: ${tagData.rawUidHex}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Technology: ${tagData.techList.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Tag Display Name") },
                    placeholder = { Text("e.g. Study Mode, Desk Trigger") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Automation Assignment Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentSelectionName = automations.find { it.id == selectedAutomationId }?.name
                        ?: if (selectedAutomationId == null) "Quick URL Action (Single)" else "None"

                    OutlinedTextField(
                        value = currentSelectionName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign Automation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚡ Quick URL Action (Single)") },
                            onClick = {
                                selectedAutomationId = null
                                dropdownExpanded = false
                            }
                        )
                        automations.forEach { auto ->
                            DropdownMenuItem(
                                text = { Text("⚙️ ${auto.name}") },
                                onClick = {
                                    selectedAutomationId = auto.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // If no multi-action automation chosen, show quick URL input
                if (selectedAutomationId == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = quickUrl,
                        onValueChange = {
                            quickUrl = it
                            urlError = null
                        },
                        label = { Text("Target Web URL") },
                        placeholder = { Text("https://example.com") },
                        isError = urlError != null,
                        supportingText = urlError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validationError = ActionValidator.validateTagName(name)
                    if (validationError != null) {
                        nameError = validationError
                        return@Button
                    }
                    if (selectedAutomationId == null) {
                        val validUrl = ActionValidator.validateUrl(quickUrl)
                        if (validUrl != null) {
                            urlError = validUrl
                            return@Button
                        }
                    }
                    onConfirm(name, selectedAutomationId, quickUrl)
                }
            ) {
                Text("Register Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Action Picker dialog displaying the 4 core action types.
 */
@Composable
fun ActionPickerDialog(
    onDismiss: () -> Unit,
    onSelectType: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Action Type", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionTypeOptionCard(
                    icon = Icons.Outlined.Apps,
                    title = "Open Application",
                    subtitle = "Launch any installed Android application",
                    onClick = { onSelectType("OPEN_APP") }
                )
                ActionTypeOptionCard(
                    icon = Icons.Outlined.Language,
                    title = "Open Web URL",
                    subtitle = "Open a website in the default browser",
                    onClick = { onSelectType("OPEN_URL") }
                )
                ActionTypeOptionCard(
                    icon = Icons.Outlined.FlashlightOn,
                    title = "Toggle Flashlight",
                    subtitle = "Turn camera torch light ON or OFF",
                    onClick = { onSelectType("TOGGLE_FLASHLIGHT") }
                )
                ActionTypeOptionCard(
                    icon = Icons.Outlined.ContentCopy,
                    title = "Copy to Clipboard",
                    subtitle = "Copy text or Wi-Fi password to clipboard",
                    onClick = { onSelectType("COPY_TO_CLIPBOARD") }
                )
                ActionTypeOptionCard(
                    icon = Icons.Outlined.Notifications,
                    title = "Show Notification",
                    subtitle = "Display a system notification in the tray",
                    onClick = { onSelectType("SHOW_NOTIFICATION") }
                )
                ActionTypeOptionCard(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Play Sound",
                    subtitle = "Play a confirmation audio beep tone",
                    onClick = { onSelectType("PLAY_SOUND") }
                )
                ActionTypeOptionCard(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Show Message",
                    subtitle = "Display a brief in-app confirmation message",
                    onClick = { onSelectType("SHOW_MESSAGE") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ActionTypeOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Action Configuration dialog for configuring a selected action.
 */
@Composable
fun ActionConfigDialog(
    actionType: String,
    installedApps: List<InstalledAppInfo>,
    existingAction: AutomationAction?,
    onDismiss: () -> Unit,
    onSave: (AutomationAction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.OpenApp) {
                InstalledAppInfo(existingAction.packageName, existingAction.appDisplayName)
            } else null
        )
    }

    var url by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.OpenUrl) existingAction.url else "https://"
        )
    }

    var notifTitle by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.ShowNotification) existingAction.title else ""
        )
    }
    var notifBody by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.ShowNotification) existingAction.body else ""
        )
    }

    var messageText by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.ShowMessage) existingAction.message else ""
        )
    }

    var flashlightEnable by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.ToggleFlashlight) existingAction.enable else true
        )
    }

    var clipText by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.CopyToClipboard) existingAction.text else ""
        )
    }

    var clipLabel by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.CopyToClipboard) existingAction.label else "TapFlow"
        )
    }

    var soundTone by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.PlaySound) existingAction.toneName else "Confirmation Beep"
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (actionType) {
                    "OPEN_APP" -> "Configure Open App"
                    "OPEN_URL" -> "Configure Open URL"
                    "SHOW_NOTIFICATION" -> "Configure Notification"
                    "SHOW_MESSAGE" -> "Configure Message"
                    "TOGGLE_FLASHLIGHT" -> "Configure Flashlight"
                    "COPY_TO_CLIPBOARD" -> "Configure Clipboard"
                    "PLAY_SOUND" -> "Configure Audio Sound"
                    else -> "Configure Action"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (actionType) {
                    "OPEN_APP" -> {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Installed Apps") },
                            placeholder = { Text("Type app name...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val filtered = installedApps.filter {
                            it.appName.contains(searchQuery, ignoreCase = true) ||
                            it.packageName.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filtered, key = { it.packageName }) { app ->
                                val isSelected = selectedApp?.packageName == app.packageName
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedApp = app },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Outlined.Check else Icons.Outlined.Apps,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(app.appName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Text(
                                                app.packageName,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "OPEN_URL" -> {
                        OutlinedTextField(
                            value = url,
                            onValueChange = {
                                url = it
                                errorMessage = null
                            },
                            label = { Text("Destination URL") },
                            placeholder = { Text("https://example.com") },
                            isError = errorMessage != null,
                            supportingText = errorMessage?.let { { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "SHOW_NOTIFICATION" -> {
                        OutlinedTextField(
                            value = notifTitle,
                            onValueChange = {
                                notifTitle = it
                                errorMessage = null
                            },
                            label = { Text("Notification Title") },
                            placeholder = { Text("e.g. Focus Mode Active") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = notifBody,
                            onValueChange = { notifBody = it },
                            label = { Text("Notification Body (Optional)") },
                            placeholder = { Text("e.g. Started your focus timer.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "SHOW_MESSAGE" -> {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = {
                                messageText = it
                                errorMessage = null
                            },
                            label = { Text("In-App Message Text") },
                            placeholder = { Text("e.g. Study Mode Triggered!") },
                            isError = errorMessage != null,
                            supportingText = errorMessage?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "TOGGLE_FLASHLIGHT" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Flashlight State on Tag Scan:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { flashlightEnable = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = flashlightEnable,
                                    onClick = { flashlightEnable = true }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Turn Flashlight ON (Torch)")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { flashlightEnable = false }
                                    .padding(vertical = 4.dp)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = !flashlightEnable,
                                    onClick = { flashlightEnable = false }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Turn Flashlight OFF")
                            }
                        }
                    }

                    "COPY_TO_CLIPBOARD" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = clipText,
                                onValueChange = {
                                    clipText = it
                                    errorMessage = null
                                },
                                label = { Text("Text to Copy") },
                                placeholder = { Text("e.g. MyWiFiPassword123 or promo code") },
                                isError = errorMessage != null,
                                supportingText = errorMessage?.let { { Text(it) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = clipLabel,
                                onValueChange = { clipLabel = it },
                                label = { Text("Label (Optional)") },
                                placeholder = { Text("e.g. Home Wi-Fi") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    "PLAY_SOUND" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select Sound Tone:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            val tones = listOf("Confirmation Beep", "High Chime", "Alert Tone")
                            tones.forEach { tone ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { soundTone = tone }
                                        .padding(vertical = 4.dp)
                                ) {
                                    androidx.compose.material3.RadioButton(
                                        selected = soundTone == tone,
                                        onClick = { soundTone = tone }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(tone)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (actionType) {
                        "OPEN_APP" -> {
                            val app = selectedApp
                            if (app == null) {
                                errorMessage = "Please select an app"
                                return@Button
                            }
                            onSave(
                                AutomationAction.OpenApp(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    packageName = app.packageName,
                                    appDisplayName = app.appName
                                )
                            )
                        }
                        "OPEN_URL" -> {
                            val err = ActionValidator.validateUrl(url)
                            if (err != null) {
                                errorMessage = err
                                return@Button
                            }
                            onSave(
                                AutomationAction.OpenUrl(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    url = url.trim()
                                )
                            )
                        }
                        "SHOW_NOTIFICATION" -> {
                            if (notifTitle.isBlank()) {
                                errorMessage = "Title cannot be empty"
                                return@Button
                            }
                            onSave(
                                AutomationAction.ShowNotification(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    title = notifTitle.trim(),
                                    body = notifBody.trim()
                                )
                            )
                        }
                        "SHOW_MESSAGE" -> {
                            if (messageText.isBlank()) {
                                errorMessage = "Message cannot be empty"
                                return@Button
                            }
                            onSave(
                                AutomationAction.ShowMessage(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    message = messageText.trim()
                                )
                            )
                        }
                        "TOGGLE_FLASHLIGHT" -> {
                            onSave(
                                AutomationAction.ToggleFlashlight(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    enable = flashlightEnable
                                )
                            )
                        }
                        "COPY_TO_CLIPBOARD" -> {
                            if (clipText.isBlank()) {
                                errorMessage = "Enter text to copy"
                                return@Button
                            }
                            onSave(
                                AutomationAction.CopyToClipboard(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    text = clipText.trim(),
                                    label = clipLabel.trim()
                                )
                            )
                        }
                        "PLAY_SOUND" -> {
                            onSave(
                                AutomationAction.PlaySound(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    toneName = soundTone
                                )
                            )
                        }
                    }
                }
            ) {
                Text("Save Action")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Interactive dialog allowing manual simulation of NFC tag taps.
 */
@Composable
fun SimulateTagDialog(
    onDismiss: () -> Unit,
    onSimulate: (TagData) -> Unit
) {
    var customUid by remember { mutableStateOf("04A1B2C3D4E5F6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Simulate NFC Scan", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pick a test tag profile or enter custom bytes to test without physical tags.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = {
                        val uid = byteArrayOf(0x04, 0xA1.toByte(), 0xB2.toByte(), 0xC3.toByte(), 0xD4.toByte(), 0xE5.toByte(), 0xF6.toByte())
                        onSimulate(TagIdentifier.identify(uid, arrayOf("android.nfc.tech.NfcA", "android.nfc.tech.Ndef")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Standard Tag (NTAG215 • 7-byte)")
                }

                OutlinedButton(
                    onClick = {
                        val uid = byteArrayOf(0x04, 0x11, 0x22, 0x33)
                        onSimulate(TagIdentifier.identify(uid, arrayOf("android.nfc.tech.NfcA", "android.nfc.tech.MifareUltralight")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mifare Ultralight (4-byte)")
                }

                OutlinedButton(
                    onClick = {
                        val uid = byteArrayOf(0x08, 0x99.toByte(), 0x88.toByte(), 0x77.toByte())
                        onSimulate(TagIdentifier.identify(uid, arrayOf("android.nfc.tech.NfcA")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Randomized UID (0x08 prefix • Reject)")
                }

                OutlinedButton(
                    onClick = {
                        val uid = byteArrayOf(0x12, 0x34, 0x56, 0x78)
                        onSimulate(TagIdentifier.identify(uid, arrayOf("android.nfc.tech.IsoDep")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Payment Card (IsoDep only • Reject)")
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = customUid,
                    onValueChange = { customUid = it },
                    label = { Text("Custom Hex UID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val cleaned = customUid.trim().replace(":", "").replace(" ", "")
                        val bytes = cleaned.chunked(2).mapNotNull {
                            it.toIntOrNull(16)?.toByte()
                        }.toByteArray()

                        if (bytes.isNotEmpty()) {
                            onSimulate(TagIdentifier.identify(bytes, arrayOf("android.nfc.tech.NfcA", "android.nfc.tech.Ndef")))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate Custom UID")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
