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

    override fun onPostResume() {
        super.onPostResume()
        updateNfcMode()
    }

    override fun onPause() {
        super.onPause()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        val isHceEnabled = container.preferences.getBoolean("hce_emit_enabled", false)
        // If HCE beacon is actively emitting, do not aggressively unset preferred service
        // so that incoming RF transactions maintain routing affinity.
        if (adapter != null && !isHceEnabled) {
            CardEmulation.getInstance(adapter).unsetPreferredService(this)
        }
        container.nfcManager.stopReaderMode(this)
    }

    private fun updateNfcMode() {
        val adapter = NfcAdapter.getDefaultAdapter(this) ?: return
        val cardEmulation = CardEmulation.getInstance(adapter)
        val hceComponent = ComponentName(this, TapFlowHceService::class.java)
        val isHceEnabled = container.preferences.getBoolean("hce_emit_enabled", false)

        val aids = arrayListOf("D2760000850101", "D2760000850100", "F0546170466C6F77")

        if (isHceEnabled) {
            // STOP reader mode so the phone's NFC hardware ceases polling and enters LISTEN/CARD mode!
            container.nfcManager.stopReaderMode(this)

            // Dynamic AID registration overrides Google Pay / other services in Android's RegisteredAidCache
            try {
                val registered = cardEmulation.registerAidsForService(hceComponent, CardEmulation.CATEGORY_OTHER, aids)
                android.util.Log.i("MainActivity", "registerAidsForService result: $registered")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to register aids: ${e.message}")
            }

            // Prioritize TapFlow HCE service for foreground AID routing
            try {
                val preferred = cardEmulation.setPreferredService(this, hceComponent)
                android.util.Log.i("MainActivity", "setPreferredService result: $preferred")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to set preferred service: ${e.message}")
            }
        } else {
            try {
                cardEmulation.removeAidsForService(hceComponent, CardEmulation.CATEGORY_OTHER)
                cardEmulation.unsetPreferredService(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to unregister aids: ${e.message}")
            }
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
