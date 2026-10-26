package com.tapflow.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.StandardCharsets

object NdefPayloadParser {

    data class ExtractedNdef(
        val uri: String? = null,
        val text: String? = null
    )

    /**
     * Extracts URI or text content from an [NdefMessage].
     */
    fun extractContent(message: NdefMessage?): ExtractedNdef {
        if (message == null || message.records.isEmpty()) return ExtractedNdef()

        for (record in message.records) {
            // Check URI Record
            val uri = try {
                record.toUri()?.toString()
            } catch (_: Exception) {
                null
            }
            if (!uri.isNullOrBlank()) {
                return ExtractedNdef(uri = uri)
            }

            // Check Text Record (RTD_TEXT)
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                val payload = record.payload
                if (payload != null && payload.isNotEmpty()) {
                    val status = payload[0].toInt() and 0xFF
                    val isUtf16 = (status and 0x80) != 0
                    val langLength = status and 0x3F
                    val charset = if (isUtf16) StandardCharsets.UTF_16 else StandardCharsets.UTF_8
                    val text = if (payload.size > 1 + langLength) {
                        String(payload, 1 + langLength, payload.size - 1 - langLength, charset)
                    } else ""
                    if (text.isNotBlank()) {
                        return ExtractedNdef(text = text)
                    }
                }
            }

            // Check MIME Media or TapFlow proprietary payload
            if (record.tnf == NdefRecord.TNF_MIME_MEDIA) {
                val payload = record.payload
                if (payload != null && payload.isNotEmpty()) {
                    val payloadStr = String(payload, StandardCharsets.UTF_8)
                    if (payloadStr.isNotBlank()) {
                        return ExtractedNdef(text = payloadStr)
                    }
                }
            }
        }

        return ExtractedNdef()
    }
}
