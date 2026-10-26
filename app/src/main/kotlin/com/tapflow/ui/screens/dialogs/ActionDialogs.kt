package com.tapflow.ui.screens.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = { Text("Quick Web URL (Direct)") },
                            onClick = {
                                selectedAutomationId = null
                                dropdownExpanded = false
                            }
                        )
                        automations.forEach { auto ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountTree,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = { Text(auto.name) },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Select Action Type", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ActionCategoryHeader("Apps & Web")
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.Apps,
                        title = "Open Application",
                        subtitle = "Launch any installed Android application",
                        onClick = { onSelectType("OPEN_APP") }
                    )
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.Language,
                        title = "Open Web URL",
                        subtitle = "Open a website in the default browser",
                        onClick = { onSelectType("OPEN_URL") }
                    )
                }

                item {
                    ActionCategoryHeader("Device Hardware & Settings")
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.FlashlightOn,
                        title = "Toggle Flashlight",
                        subtitle = "Turn camera torch light ON or OFF",
                        onClick = { onSelectType("TOGGLE_FLASHLIGHT") }
                    )
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.Settings,
                        title = "Device Settings",
                        subtitle = "Open Wi-Fi, Bluetooth, NFC, Sound or Battery",
                        onClick = { onSelectType("OPEN_SETTINGS") }
                    )
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Set Ringer Mode",
                        subtitle = "Switch audio to Silent, Vibrate, or Normal",
                        onClick = { onSelectType("SET_RINGER") }
                    )
                }

                item {
                    ActionCategoryHeader("Data & Timing")
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.ContentCopy,
                        title = "Copy to Clipboard",
                        subtitle = "Copy text or Wi-Fi password to clipboard",
                        onClick = { onSelectType("COPY_TO_CLIPBOARD") }
                    )
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Compose SMS",
                        subtitle = "Pre-fill text message to a contact",
                        onClick = { onSelectType("COMPOSE_SMS") }
                    )
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.HourglassEmpty,
                        title = "Wait / Delay",
                        subtitle = "Pause execution for a few seconds between actions",
                        onClick = { onSelectType("DELAY") }
                    )
                }

                item {
                    ActionCategoryHeader("Notifications & Feedback")
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.Notifications,
                        title = "Show Notification",
                        subtitle = "Display a system notification in the tray",
                        onClick = { onSelectType("SHOW_NOTIFICATION") }
                    )
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.VolumeUp,
                        title = "Play Sound",
                        subtitle = "Play a confirmation audio beep tone",
                        onClick = { onSelectType("PLAY_SOUND") }
                    )
                }
                item {
                    ActionTypeOptionCard(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Show Message",
                        subtitle = "Display a brief in-app confirmation message",
                        onClick = { onSelectType("SHOW_MESSAGE") }
                    )
                }
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
private fun ActionCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
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

    var selectedSetting by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.OpenSettings) existingAction.settingType else "WIFI"
        )
    }

    var selectedRinger by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.SetRingerMode) existingAction.ringerMode else "VIBRATE"
        )
    }

    var smsPhone by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.ComposeSms) existingAction.phoneNumber else ""
        )
    }

    var smsBody by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.ComposeSms) existingAction.message else ""
        )
    }

    var delaySeconds by remember {
        mutableStateOf(
            if (existingAction is AutomationAction.DelayAction) (existingAction.durationMs / 1000L).coerceAtLeast(1L).toString() else "1"
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
                    "OPEN_SETTINGS" -> "Configure Device Settings"
                    "SET_RINGER" -> "Configure Ringer Mode"
                    "COMPOSE_SMS" -> "Configure SMS"
                    "DELAY" -> "Configure Pause Delay"
                    else -> "Configure Action"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (actionType == "OPEN_APP") {
                Column(modifier = Modifier.fillMaxWidth()) {
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (actionType) {
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

                    "OPEN_SETTINGS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Select Settings Screen:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            val settings = listOf(
                                "WIFI" to "Wi-Fi Settings",
                                "BLUETOOTH" to "Bluetooth Settings",
                                "NFC" to "NFC Settings",
                                "SOUND" to "Sound & Volume Settings",
                                "DISPLAY" to "Display & Brightness Settings",
                                "BATTERY" to "Battery & Power Settings"
                            )
                            settings.forEach { (type, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSetting = type }
                                        .padding(vertical = 4.dp)
                                ) {
                                    androidx.compose.material3.RadioButton(
                                        selected = selectedSetting == type,
                                        onClick = { selectedSetting = type }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    "SET_RINGER" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Set Ringer Mode:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            val modes = listOf(
                                "SILENT" to "Silent Mode",
                                "VIBRATE" to "Vibrate Only",
                                "NORMAL" to "Normal (Sound ON)"
                            )
                            modes.forEach { (mode, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedRinger = mode }
                                        .padding(vertical = 4.dp)
                                ) {
                                    androidx.compose.material3.RadioButton(
                                        selected = selectedRinger == mode,
                                        onClick = { selectedRinger = mode }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }

                    "COMPOSE_SMS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = smsPhone,
                                onValueChange = { smsPhone = it },
                                label = { Text("Recipient Phone Number") },
                                placeholder = { Text("+1234567890") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = smsBody,
                                onValueChange = { smsBody = it },
                                label = { Text("Message Body (Optional)") },
                                placeholder = { Text("e.g. Arrived home safely!") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    "DELAY" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Pause execution for a few seconds:", fontSize = 13.sp)
                            OutlinedTextField(
                                value = delaySeconds,
                                onValueChange = { delaySeconds = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Delay (Seconds)") },
                                placeholder = { Text("1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
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
                            var cleanUrl = url.trim()
                            if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
                                cleanUrl = "https://$cleanUrl"
                            }
                            val err = ActionValidator.validateUrl(cleanUrl)
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
                                    url = cleanUrl
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
                        "OPEN_SETTINGS" -> {
                            onSave(
                                AutomationAction.OpenSettings(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    settingType = selectedSetting
                                )
                            )
                        }
                        "SET_RINGER" -> {
                            onSave(
                                AutomationAction.SetRingerMode(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    ringerMode = selectedRinger
                                )
                            )
                        }
                        "COMPOSE_SMS" -> {
                            if (smsPhone.isBlank() && smsBody.isBlank()) {
                                errorMessage = "Enter a phone number or message"
                                return@Button
                            }
                            onSave(
                                AutomationAction.ComposeSms(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    phoneNumber = smsPhone.trim(),
                                    message = smsBody.trim()
                                )
                            )
                        }
                        "DELAY" -> {
                            val secs = delaySeconds.toLongOrNull() ?: 1L
                            onSave(
                                AutomationAction.DelayAction(
                                    id = existingAction?.id ?: 0,
                                    automationId = existingAction?.automationId ?: 0,
                                    sortOrder = existingAction?.sortOrder ?: 0,
                                    isEnabled = existingAction?.isEnabled ?: true,
                                    durationMs = secs * 1000L
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

/**
 * Dialog for configuring the emitted Virtual NFC Type 4 Beacon signal.
 */
@Composable
fun VirtualNfcConfigDialog(
    currentType: String,
    currentContent: String,
    onDismiss: () -> Unit,
    onSave: (type: String, content: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var contentText by remember { mutableStateOf(currentContent) }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                            imageVector = Icons.Outlined.Sensors,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Virtual NFC Beacon", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Broadcast an active NFC Type 4 Smart Card signal from your phone. Any phone tapping this device will read this payload directly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Signal Payload Type",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "URL",
                        onClick = {
                            selectedType = "URL"
                            if (contentText.isBlank() || !contentText.startsWith("http")) {
                                contentText = "https://"
                            }
                            errorText = null
                        },
                        label = { Text("Web URL", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedType == "TEXT",
                        onClick = {
                            selectedType = "TEXT"
                            if (contentText.startsWith("http")) contentText = ""
                            errorText = null
                        },
                        label = { Text("Text", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedType == "TAPFLOW",
                        onClick = {
                            selectedType = "TAPFLOW"
                            if (contentText.isBlank() || contentText.startsWith("http")) {
                                contentText = "study_mode"
                            }
                            errorText = null
                        },
                        label = { Text("TapFlow Beacon", fontSize = 12.sp) }
                    )
                }

                when (selectedType) {
                    "URL" -> {
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = {
                                contentText = it
                                errorText = null
                            },
                            label = { Text("Website or Video URL") },
                            placeholder = { Text("https://example.com") },
                            singleLine = true,
                            isError = errorText != null,
                            supportingText = errorText?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "When another phone taps this phone, their browser opens this link immediately.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    "TEXT" -> {
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = {
                                contentText = it
                                errorText = null
                            },
                            label = { Text("Note or Credentials") },
                            placeholder = { Text("Enter text, Wi-Fi password, or contact note...") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 140.dp)
                        )
                    }
                    "TAPFLOW" -> {
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = {
                                contentText = it
                                errorText = null
                            },
                            label = { Text("TapFlow Trigger Token") },
                            placeholder = { Text("e.g. study_mode or desk_switch") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Broadcasts an application/vnd.tapflow NDEF record that triggers automations on other TapFlow devices.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedType == "URL") {
                        val validation = ActionValidator.validateUrl(contentText)
                        if (validation != null) {
                            errorText = validation
                            return@Button
                        }
                    }
                    if (contentText.isBlank()) {
                        errorText = "Content cannot be empty"
                        return@Button
                    }
                    onSave(selectedType, contentText.trim())
                }
            ) {
                Text("Save Signal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
