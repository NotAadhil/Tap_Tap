package com.tapflow

import android.Manifest
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

    override fun onResume() {
        super.onResume()
        container.nfcManager.startReaderMode(this)
    }

    override fun onPause() {
        super.onPause()
        container.nfcManager.stopReaderMode(this)
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
