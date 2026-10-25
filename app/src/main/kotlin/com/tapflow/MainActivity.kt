package com.tapflow

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tapflow.ui.theme.TapFlowTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy {
        (application as TapFlowApp).container
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // System permission response handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable show when locked and turn screen on for NFC lockscreen triggers
        setupLockscreenVisibility()

        // Handle cold-start launch via NFC tag only on fresh launch (not configuration change/rotation)
        if (savedInstanceState == null) {
            handleNfcIntent(intent)
        }

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

    private fun setupLockscreenVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private var isActivityResumed: Boolean = false

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        container.nfcManager.startReaderMode(this)
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        container.nfcManager.stopReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action in listOf(
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED
            )
        ) {
            val wasInBackground = !isActivityResumed

            // Dismiss keyguard if locked so the user can interact with launched apps
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (keyguardManager?.isKeyguardLocked == true) {
                keyguardManager.requestDismissKeyguard(this, null)
            }

            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let {
                container.nfcManager.handleTag(it) { event ->
                    if (wasInBackground) {
                        when (event) {
                            is com.tapflow.nfc.ScanEvent.Matched,
                            is com.tapflow.nfc.ScanEvent.Debounced,
                            is com.tapflow.nfc.ScanEvent.Error -> {
                                runOnUiThread {
                                    moveTaskToBack(true)
                                }
                            }
                            is com.tapflow.nfc.ScanEvent.Unregistered -> {
                                // Keep activity in foreground so user can register the new tag
                            }
                        }
                    }
                }
                // Consume the action to prevent re-processing on subsequent state restoration
                intent.action = null
            }
        }
    }
}
