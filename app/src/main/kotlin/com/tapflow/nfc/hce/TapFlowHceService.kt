package com.tapflow.nfc.hce

import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Android Host Card Emulation (HCE) service.
 * Emits active ISO-DEP contactless smart card signals (NFC Forum Type 4 Tag).
 */
class TapFlowHceService : HostApduService() {

    private val emulator = Type4TagEmulator()

    override fun onCreate() {
        super.onCreate()
        reloadFromPreferences()
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        if (commandApdu == null) return null
        reloadFromPreferences()
        return emulator.processCommand(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
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
