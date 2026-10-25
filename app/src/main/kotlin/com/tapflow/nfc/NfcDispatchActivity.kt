package com.tapflow.nfc

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import com.tapflow.AppContainer
import com.tapflow.MainActivity
import com.tapflow.TapFlowApp

/**
 * Invisible, transparent trampoline activity that handles all background/lockscreen NFC tag intents.
 * Because this activity has no visible window and no animations, it intercepts NFC intents without
 * flickering the screen, stealing task focus, or bringing the main TapFlow UI over active apps (like YouTube).
 */
class NfcDispatchActivity : ComponentActivity() {

    private val container: AppContainer by lazy {
        (application as TapFlowApp).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockscreenVisibility()
        processNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processNfcIntent(intent)
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

    private fun processNfcIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }

        val action = intent.action ?: ""
        if (action !in listOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED
            )
        ) {
            finish()
            return
        }

        // Unlock keyguard if needed so launched apps (YouTube, etc.) can appear
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

        if (tag == null) {
            finish()
            return
        }

        // Dispatch tag through coordinator
        container.nfcManager.handleTag(tag) { event ->
            runOnUiThread {
                when (event) {
                    is ScanEvent.Matched,
                    is ScanEvent.Debounced,
                    is ScanEvent.Error -> {
                        // Silent completion: keep TapFlow invisible so user's active/launched app stays on top
                        finish()
                    }
                    is ScanEvent.Unregistered -> {
                        // For an unregistered tag, open MainActivity so the user can register it
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(MainActivity.EXTRA_UNREGISTERED_TAG, event.tagData)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                }
            }
        }
    }
}
