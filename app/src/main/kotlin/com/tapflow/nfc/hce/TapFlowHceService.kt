package com.tapflow.nfc.hce

import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android Host Card Emulation (HCE) service.
 * Emits active ISO-DEP contactless smart card signals (NFC Forum Type 4 Tag).
 */
class TapFlowHceService : HostApduService() {

    private val emulator = Type4TagEmulator()

    companion object {
        private const val TAG = "TapFlowHceService"
        private val _lastTransmissionTimestamp = MutableStateFlow<Long?>(null)
        val lastTransmissionTimestamp = _lastTransmissionTimestamp.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        reloadFromPreferences()
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        if (commandApdu == null) return null
        reloadFromPreferences()
        val response = emulator.processCommand(commandApdu)
        _lastTransmissionTimestamp.value = System.currentTimeMillis()
        Log.d(TAG, "APDU In: ${commandApdu.size}b, Out: ${response.size}b")
        return response
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: reason=$reason")
        emulator.onDeactivated()
    }

    private fun reloadFromPreferences() {
        val prefs = applicationContext.getSharedPreferences("tapflow_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("hce_emit_enabled", false)
        val type = prefs.getString("hce_payload_type", "URL") ?: "URL"
        val content = prefs.getString("hce_payload_content", "https://github.com/NotAadhil/Tap_Tap")
            ?: "https://github.com/NotAadhil/Tap_Tap"

        emulator.isEnabled = isEnabled
        emulator.ndefPayload = when (type) {
            "URL" -> NdefPayloadBuilder.buildUriNdef(content)
            "TEXT" -> NdefPayloadBuilder.buildTextNdef(content)
            "TAPFLOW" -> NdefPayloadBuilder.buildTapFlowNdef(content)
            else -> NdefPayloadBuilder.buildUriNdef(content)
        }
    }
}
