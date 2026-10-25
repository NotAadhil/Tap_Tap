package com.tapflow.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation tabs for TapFlow.
 * Uses clean Material vector icons (no emojis).
 */
enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    TAGS("Tags", Icons.Outlined.Nfc),
    AUTOMATIONS("Automations", Icons.Outlined.AccountTree),
    HISTORY("History", Icons.Outlined.History),
    SETTINGS("Settings", Icons.Outlined.Settings)
}
