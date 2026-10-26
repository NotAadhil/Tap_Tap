package com.tapflow

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tapflow.nfc.hce.TapFlowHceService
import com.tapflow.nfc.model.TagData
import com.tapflow.ui.theme.TapFlowTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_UNREGISTERED_TAG = "extra_unregistered_tag"
    }

    private val container: AppContainer by lazy {
        (application as TapFlowApp).container
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // System permission response handled
        }

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "hce_emit_enabled") {
            runOnUiThread {
                updateNfcMode()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        container.preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        handleUnregisteredTagIntent(intent)

        setContent {
            TapFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        container = container,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        container.preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onResume() {
        super.onResume()
        updateNfcMode()
    }

    override fun onPause() {
        super.onPause()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter != null) {
            CardEmulation.getInstance(adapter).unsetPreferredService(this)
        }
        container.nfcManager.stopReaderMode(this)
    }

    private fun updateNfcMode() {
        val adapter = NfcAdapter.getDefaultAdapter(this) ?: return
        val cardEmulation = CardEmulation.getInstance(adapter)
        val hceComponent = ComponentName(this, TapFlowHceService::class.java)
        val isHceEnabled = container.preferences.getBoolean("hce_emit_enabled", false)

        if (isHceEnabled) {
            // STOP reader mode so the phone's NFC hardware ceases polling and enters LISTEN/CARD mode!
            container.nfcManager.stopReaderMode(this)
            // Prioritize TapFlow HCE service for foreground AID routing
            cardEmulation.setPreferredService(this, hceComponent)
        } else {
            // Unset HCE priority and resume standard tag reader mode
            cardEmulation.unsetPreferredService(this)
            container.nfcManager.startReaderMode(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUnregisteredTagIntent(intent)
    }

    private fun handleUnregisteredTagIntent(intent: Intent?) {
        if (intent == null) return
        val tagData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_UNREGISTERED_TAG, TagData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_UNREGISTERED_TAG) as? TagData
        }

        if (tagData != null) {
            container.coordinator.onTagDetected(tagData)
            intent.removeExtra(EXTRA_UNREGISTERED_TAG)
        }
    }
}
