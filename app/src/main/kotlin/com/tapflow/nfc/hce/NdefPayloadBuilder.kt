package com.tapflow.nfc.hce

import java.nio.charset.StandardCharsets

/**
 * Builds standard NFC Forum NDEF byte payloads for Type 4 tag emulation.
 * Pure Kotlin implementation: operates cleanly across JVM unit tests and Android runtime.
 */
object NdefPayloadBuilder {

    /**
     * Builds an NDEF message containing a URI record (Type 'U').
     */
    fun buildUriNdef(url: String): ByteArray {
        val cleanUrl = url.trim()
        val (prefixByte, uriSuffix) = when {
            cleanUrl.startsWith("http://www.") -> Pair(0x01.toByte(), cleanUrl.removePrefix("http://www."))
            cleanUrl.startsWith("https://www.") -> Pair(0x02.toByte(), cleanUrl.removePrefix("https://www."))
            cleanUrl.startsWith("http://") -> Pair(0x03.toByte(), cleanUrl.removePrefix("http://"))
            cleanUrl.startsWith("https://") -> Pair(0x04.toByte(), cleanUrl.removePrefix("https://"))
            cleanUrl.startsWith("tel:") -> Pair(0x05.toByte(), cleanUrl.removePrefix("tel:"))
            cleanUrl.startsWith("mailto:") -> Pair(0x06.toByte(), cleanUrl.removePrefix("mailto:"))
            else -> Pair(0x04.toByte(), cleanUrl) // default to https://
        }

        val uriBytes = uriSuffix.toByteArray(StandardCharsets.UTF_8)
        val payloadLength = 1 + uriBytes.size

        // NDEF Record: MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x01 (Well-Known) -> 0xD1
        val record = ByteArray(3 + 1 + payloadLength)
        record[0] = 0xD1.toByte() // Flags: MB=1, ME=1, SR=1, TNF=1
        record[1] = 0x01.toByte() // Type Length: 1 byte
        record[2] = payloadLength.toByte() // Payload Length
        record[3] = 0x55.toByte() // Type: 'U' (0x55)
        record[4] = prefixByte
        System.arraycopy(uriBytes, 0, record, 5, uriBytes.size)

        return record
    }

    /**
     * Builds an NDEF message containing a Text record (Type 'T').
     */
    fun buildTextNdef(text: String, languageCode: String = "en"): ByteArray {
        val langBytes = languageCode.toByteArray(StandardCharsets.US_ASCII)
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        val statusByte = (langBytes.size and 0x3F).toByte() // UTF-8 (bit 7 = 0) + lang length
        val payloadLength = 1 + langBytes.size + textBytes.size

        val record = ByteArray(3 + 1 + payloadLength)
        record[0] = 0xD1.toByte() // Flags: MB=1, ME=1, SR=1, TNF=1
        record[1] = 0x01.toByte() // Type Length: 1 byte ('T')
        record[2] = payloadLength.toByte() // Payload Length
        record[3] = 0x54.toByte() // Type: 'T' (0x54)
        record[4] = statusByte
        System.arraycopy(langBytes, 0, record, 5, langBytes.size)
        System.arraycopy(textBytes, 0, record, 5 + langBytes.size, textBytes.size)

        return record
    }

    /**
     * Builds an NDEF message containing a custom TapFlow MIME record (application/vnd.tapflow).
     */
    fun buildTapFlowNdef(payload: String): ByteArray {
        val mimeType = "application/vnd.tapflow".toByteArray(StandardCharsets.US_ASCII)
        val dataBytes = payload.toByteArray(StandardCharsets.UTF_8)

        // NDEF Record: MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x02 (MIME Media) -> 0xD2
        val record = ByteArray(3 + mimeType.size + dataBytes.size)
        record[0] = 0xD2.toByte() // Flags: MB=1, ME=1, SR=1, TNF=2
        record[1] = mimeType.size.toByte()
        record[2] = dataBytes.size.toByte()
        System.arraycopy(mimeType, 0, record, 3, mimeType.size)
        System.arraycopy(dataBytes, 0, record, 3 + mimeType.size, dataBytes.size)

        return record
    }
}
