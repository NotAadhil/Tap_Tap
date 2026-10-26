package com.tapflow.ui.screens.dialogs

import android.nfc.NdefMessage
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapflow.automation.InstalledAppInfo
import com.tapflow.nfc.writer.NdefPayloadBuilder

/**
 * Frosted Glass Tag Tools modal for Writing, Cloning, and Locking physical NFC tags.
 */
@Composable
fun TagToolsDialog(
    selectedTab: Int,
    isWaitingForTag: Boolean,
    isSuccess: Boolean,
    statusMessage: String?,
    clonedPayload: NdefMessage?,
    installedApps: List<InstalledAppInfo>,
    onSelectTab: (Int) -> Unit,
    onStartWrite: (NdefMessage) -> Unit,
    onStartCloneRead: () -> Unit,
    onStartCloneWrite: () -> Unit,
    onStartLock: () -> Unit,
    onCancelOperation: () -> Unit,
    onDismiss: () -> Unit
) {
    // Write form states
    var writeType by remember { mutableStateOf("URL") } // URL, WIFI, TEXT, APP
    var urlText by remember { mutableStateOf("https://") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var appSearchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var writeValidationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
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
                Spacer(modifier = Modifier.width(10.dp))
                Text("NFC Tag Tools", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        text = { Text("Write Tag") },
                        icon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        text = { Text("Clone Tag") },
                        icon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { onSelectTab(2) },
                        text = { Text("Lock Tag") },
                        icon = { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Banner
                if (isWaitingForTag) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusMessage ?: "Waiting for tag...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onCancelOperation,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp)
                            }
                        }
                    }
                } else if (!statusMessage.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = statusMessage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSuccess) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Tab 0: WRITE TAG
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Payload Format:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = writeType == "URL",
                                    onClick = { writeType = "URL" },
                                    label = { Text("URL") },
                                    leadingIcon = { Icon(Icons.Outlined.Language, null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = writeType == "WIFI",
                                    onClick = { writeType = "WIFI" },
                                    label = { Text("Wi-Fi") },
                                    leadingIcon = { Icon(Icons.Outlined.Wifi, null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = writeType == "TEXT",
                                    onClick = { writeType = "TEXT" },
                                    label = { Text("Text") },
                                    leadingIcon = { Icon(Icons.Outlined.Notes, null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = writeType == "APP",
                                    onClick = { writeType = "APP" },
                                    label = { Text("App") },
                                    leadingIcon = { Icon(Icons.Outlined.Apps, null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            when (writeType) {
                                "URL" -> {
                                    OutlinedTextField(
                                        value = urlText,
                                        onValueChange = {
                                            urlText = it
                                            writeValidationError = null
                                        },
                                        label = { Text("Destination URL") },
                                        placeholder = { Text("https://example.com") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "WIFI" -> {
                                    OutlinedTextField(
                                        value = wifiSsid,
                                        onValueChange = {
                                            wifiSsid = it
                                            writeValidationError = null
                                        },
                                        label = { Text("Network Name (SSID)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = wifiPassword,
                                        onValueChange = { wifiPassword = it },
                                        label = { Text("Wi-Fi Password") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "TEXT" -> {
                                    OutlinedTextField(
                                        value = noteText,
                                        onValueChange = {
                                            noteText = it
                                            writeValidationError = null
                                        },
                                        label = { Text("Plain Text Note / Secret") },
                                        placeholder = { Text("Type custom note...") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "APP" -> {
                                    OutlinedTextField(
                                        value = appSearchQuery,
                                        onValueChange = { appSearchQuery = it },
                                        label = { Text("Search App") },
                                        placeholder = { Text("Filter apps...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val filtered = installedApps.filter {
                                        it.appName.contains(appSearchQuery, ignoreCase = true) ||
                                        it.packageName.contains(appSearchQuery, ignoreCase = true)
                                    }
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 140.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(filtered.take(15)) { app ->
                                            val isSel = selectedApp?.packageName == app.packageName
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedApp = app },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surface
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSel) Icons.Outlined.Check else Icons.Outlined.Apps,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(app.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (writeValidationError != null) {
                                Text(
                                    text = writeValidationError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    when (writeType) {
                                        "URL" -> {
                                            if (urlText.isBlank() || urlText == "https://") {
                                                writeValidationError = "Please enter a valid URL."
                                                return@Button
                                            }
                                            val msg = NdefPayloadBuilder.buildUriRecord(urlText)
                                            onStartWrite(msg)
                                        }
                                        "WIFI" -> {
                                            if (wifiSsid.isBlank()) {
                                                writeValidationError = "Network SSID is required."
                                                return@Button
                                            }
                                            val msg = NdefPayloadBuilder.buildWifiRecord(wifiSsid.trim(), wifiPassword)
                                            onStartWrite(msg)
                                        }
                                        "TEXT" -> {
                                            if (noteText.isBlank()) {
                                                writeValidationError = "Text cannot be empty."
                                                return@Button
                                            }
                                            val msg = NdefPayloadBuilder.buildTextRecord(noteText.trim())
                                            onStartWrite(msg)
                                        }
                                        "APP" -> {
                                            val app = selectedApp
                                            if (app == null) {
                                                writeValidationError = "Select an application to launch."
                                                return@Button
                                            }
                                            val msg = NdefPayloadBuilder.buildAppLaunchRecord(app.packageName)
                                            onStartWrite(msg)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Prepare & Write to Tag")
                            }
                        }
                    }

                    1 -> {
                        // Tab 1: CLONE TAG
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Duplicate any existing NFC tag onto another blank sticker with 100% payload fidelity.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (clonedPayload == null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Step 1: Read Original Tag", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Hold the source tag you want to clone against your phone.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = onStartCloneRead,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Step 1: Tap to Read Source")
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Step 2: Write Duplicate Tag", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Source data ready (${clonedPayload.toByteArray().size} bytes). Tap your blank destination tag to write the clone.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                Button(
                                    onClick = onStartCloneWrite,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Step 2: Tap to Write Clone")
                                }

                                OutlinedButton(
                                    onClick = onStartCloneRead,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Read Different Source Tag")
                                }
                            }
                        }
                    }

                    2 -> {
                        // Tab 2: LOCK TAG
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Permanent Hardware Lock", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                        Text(
                                            "Making a tag Read-Only cannot be undone on standard NTAG stickers. The tag can never be rewritten.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onStartLock,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tap Tag to Permanently Lock")
                            }
                        }
                    }
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
