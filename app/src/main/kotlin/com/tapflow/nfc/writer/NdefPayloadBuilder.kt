package com.tapflow.nfc.writer

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Builds standard NFC Forum NDEF records and messages for writing to physical tags.
 */
object NdefPayloadBuilder {

    /**
     * Builds an NDEF URI record (e.g., https://example.com).
     */
    fun buildUriRecord(url: String): NdefMessage {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            cleanUrl = "https://$cleanUrl"
        }
        val record = NdefRecord.createUri(Uri.parse(cleanUrl))
        return NdefMessage(arrayOf(record))
    }

    /**
     * Builds an NDEF Text record (RFC standard RTD_TEXT).
     */
    fun buildTextRecord(text: String, languageCode: String = "en"): NdefMessage {
        val langBytes = languageCode.lowercase(Locale.ROOT).toByteArray(StandardCharsets.US_ASCII)
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        val status = (langBytes.size and 0x3F).toByte()

        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = status
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        val record = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
        return NdefMessage(arrayOf(record))
    }

    /**
     * Builds an NDEF record for connecting to Wi-Fi networks.
     * Uses the universal `WIFI:S:<SSID>;T:<AUTH>;P:<PASSWORD>;;` payload
     * recognized natively across Android and iOS devices.
     */
    fun buildWifiRecord(ssid: String, password: String = "", authType: String = "WPA"): NdefMessage {
        val cleanAuth = when (authType.uppercase(Locale.ROOT)) {
            "WEP" -> "WEP"
            "NOPASS", "OPEN", "NONE" -> "nopass"
            else -> "WPA"
        }
        val wifiConfig = "WIFI:S:$ssid;T:$cleanAuth;P:$password;;"
        return buildTextRecord(wifiConfig)
    }

    /**
     * Builds an Android Application Record (AAR) that directly opens an installed app
     * when the tag is tapped on any Android device.
     */
    fun buildAppLaunchRecord(packageName: String): NdefMessage {
        val record = NdefRecord.createApplicationRecord(packageName.trim())
        return NdefMessage(arrayOf(record))
    }

    /**
     * Builds a custom TapFlow automation trigger NDEF record.
     */
    fun buildTapFlowTriggerRecord(automationId: Long, name: String): NdefMessage {
        val mimeType = "application/vnd.tapflow"
        val payload = "automationId=$automationId;name=$name".toByteArray(StandardCharsets.UTF_8)
        val record = NdefRecord.createMime(mimeType, payload)
        return NdefMessage(arrayOf(record))
    }
}
