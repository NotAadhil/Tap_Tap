package com.tapflow.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapflow.data.db.entity.AutomationEntity
import com.tapflow.data.db.entity.ExecutionLogEntity
import com.tapflow.data.db.entity.RegisteredTagEntity
import com.tapflow.nfc.ScanEvent
import com.tapflow.ui.main.AppTab

@Composable
fun HomeScreenContent(
    isNfcAvailable: Boolean,
    isNfcEnabled: Boolean,
    registeredTags: List<RegisteredTagEntity>,
    automations: List<AutomationEntity>,
    executionLogs: List<ExecutionLogEntity>,
    activeScanEvent: ScanEvent?,
    onNavigateTab: (AppTab) -> Unit,
    onOpenSimulate: () -> Unit,
    onOpenTagDetail: (RegisteredTagEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NFC Status Card
        item {
            NfcHardwareStatusCard(
                isAvailable = isNfcAvailable,
                isEnabled = isNfcEnabled,
                onOpenSimulate = onOpenSimulate
            )
        }

        // Live Scan Feedback Banner
        item {
            HomeLiveScanCard(
                scanEvent = activeScanEvent,
                isNfcReady = isNfcAvailable && isNfcEnabled
            )
        }

        // Metric Statistics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Registered Tags",
                    value = registeredTags.size.toString(),
                    icon = "🏷️",
                    onClick = { onNavigateTab(AppTab.TAGS) }
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Automations",
                    value = automations.size.toString(),
                    icon = "⚙️",
                    onClick = { onNavigateTab(AppTab.AUTOMATIONS) }
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Executions",
                    value = executionLogs.size.toString(),
                    icon = "📜",
                    onClick = { onNavigateTab(AppTab.HISTORY) }
                )
            }
        }

        // Quick Actions Section
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onNavigateTab(AppTab.TAGS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manage Tags", fontSize = 13.sp)
                }
                FilledTonalButton(
                    onClick = { onNavigateTab(AppTab.AUTOMATIONS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Build Automation", fontSize = 13.sp)
                }
            }
        }

        // Recent Registered Tags Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active NFC Triggers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (registeredTags.isNotEmpty()) {
                    Text(
                        text = "View all",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateTab(AppTab.TAGS) }
                    )
                }
            }
        }

        if (registeredTags.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👋", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tags registered yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap any NFC tag to your phone (or tap 'Simulate Tap' above) to begin.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(registeredTags.take(3), key = { it.tagKey }) { tag ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTagDetail(tag) },
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
                            shape = CircleShape,
                            color = if (tag.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (tag.isEnabled) "⚡" else "⏸️", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tag.displayName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "UID: ${tag.rawUidHex} • ${tag.techList}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NfcHardwareStatusCard(
    isAvailable: Boolean,
    isEnabled: Boolean,
    onOpenSimulate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!isAvailable || !isEnabled) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!isAvailable) "❌" else if (!isEnabled) "⚠️" else "🟢",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (!isAvailable) "NFC Not Available" else if (!isEnabled) "NFC Disabled in Settings" else "NFC Ready & Listening",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = if (isAvailable && isEnabled) "Tap an NFC tag to trigger actions" else "Use Simulator or enable NFC in system settings",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenSimulate,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("Simulate", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HomeLiveScanCard(
    scanEvent: ScanEvent?,
    isNfcReady: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (scanEvent) {
                is ScanEvent.Matched -> Color(0xFFE8F5E9)
                is ScanEvent.Unregistered -> Color(0xFFE3F2FD)
                is ScanEvent.Error -> Color(0xFFFFEBEE)
                is ScanEvent.Debounced -> Color(0xFFFFF8E1)
                null -> MaterialTheme.colorScheme.surface
            }
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
                shape = CircleShape,
                color = when (scanEvent) {
                    is ScanEvent.Matched -> Color(0xFF2E7D32)
                    is ScanEvent.Unregistered -> Color(0xFF1976D2)
                    is ScanEvent.Error -> Color(0xFFC62828)
                    is ScanEvent.Debounced -> Color(0xFFF57F17)
                    null -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = when (scanEvent) {
                            is ScanEvent.Matched -> "✓"
                            is ScanEvent.Unregistered -> "+"
                            is ScanEvent.Error -> "!"
                            is ScanEvent.Debounced -> "⏱"
                            null -> "📡"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = when (scanEvent) {
                        is ScanEvent.Matched -> "Recognized: ${scanEvent.tag.displayName}"
                        is ScanEvent.Unregistered -> "Unregistered Tag Detected"
                        is ScanEvent.Error -> "Scan Warning"
                        is ScanEvent.Debounced -> "Debounce Guard Active"
                        null -> if (isNfcReady) "Ready to Scan" else "NFC Standby"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = when (scanEvent) {
                        is ScanEvent.Matched -> "Automation sequence executed successfully"
                        is ScanEvent.Unregistered -> "UID: ${scanEvent.tagData.rawUidHex} • Tap to register"
                        is ScanEvent.Error -> scanEvent.message
                        is ScanEvent.Debounced -> "Rapid double-tap ignored (< 2s)"
                        null -> "Hold any physical NFC card or tag against the phone"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
