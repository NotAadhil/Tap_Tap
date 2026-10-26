package com.tapflow.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.util.Log

/**
 * Universal NFC reader for physical NDEF tags and ISO-DEP Type 4 tags (including active HCE phone beacons).
 */
object Type4TagReader {

    private const val TAG = "Type4TagReader"

    /**
     * Attempts to read an NDEF message from a [Tag].
     * First checks framework [Ndef] tech; if unavailable or failing, performs raw ISO-DEP APDUs.
     */
    fun readType4Tag(tag: Tag): NdefMessage? {
        // Priority 1: Standard Android Ndef tech (handles physical tags and some smart cards)
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage
                ndef.close()
                if (msg != null) {
                    Log.i(TAG, "Read NDEF via Ndef tech: ${msg.records.size} records")
                }
                msg
            } catch (e: Exception) {
                try { ndef.close() } catch (_: Exception) {}
                Log.w(TAG, "Failed reading via Ndef tech: ${e.message}, falling back to IsoDep")
                null
            }
        }

        // Priority 2: Direct ISO-DEP APDU exchange (for Android HCE phones and Type 4 Smart Cards)
        val isoDep = IsoDep.get(tag) ?: return null
        return try {
            isoDep.connect()
            isoDep.timeout = 2000

            // 1. Select AID: D2 76 00 00 85 01 01 (NFC Forum Type 4 Tag v2.0)
            val selectAidV2 = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte()
            )
            var response = isoDep.transceive(selectAidV2)
            if (!isSuccess(response)) {
                // Fallback to V1 AID: D2 76 00 00 85 01 00
                val selectAidV1 = byteArrayOf(
                    0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                    0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x00.toByte()
                )
                response = isoDep.transceive(selectAidV1)
                if (!isSuccess(response)) {
                    // Fallback to TapFlow Proprietary AID
                    val selectAidTapFlow = byteArrayOf(
                        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x08.toByte(),
                        0xF0.toByte(), 0x54.toByte(), 0x61.toByte(), 0x70.toByte(), 0x46.toByte(), 0x6C.toByte(), 0x6F.toByte(), 0x77.toByte()
                    )
                    response = isoDep.transceive(selectAidTapFlow)
                    if (!isSuccess(response)) {
                        isoDep.close()
                        return null
                    }
                }
            }

            // 2. Select CC File (E1 03)
            val selectCc = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
                0xE1.toByte(), 0x03.toByte()
            )
            response = isoDep.transceive(selectCc)
            if (!isSuccess(response)) {
                isoDep.close()
                return null
            }

            // 3. Read CC File (15 bytes)
            val readCc = byteArrayOf(0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0F.toByte())
            response = isoDep.transceive(readCc)
            if (!isSuccess(response) || response.size < 17) {
                isoDep.close()
                return null
            }

            // 4. Select NDEF File (E1 04)
            val selectNdef = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
                0xE1.toByte(), 0x04.toByte()
            )
            response = isoDep.transceive(selectNdef)
            if (!isSuccess(response)) {
                isoDep.close()
                return null
            }

            // 5. Read NDEF Length (first 2 bytes)
            val readLen = byteArrayOf(0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte())
            response = isoDep.transceive(readLen)
            if (!isSuccess(response) || response.size < 4) {
                isoDep.close()
                return null
            }
            val nlen = ((response[0].toInt() and 0xFF) shl 8) or (response[1].toInt() and 0xFF)
            if (nlen <= 0 || nlen > 8192) {
                isoDep.close()
                return null
            }

            // 6. Read NDEF Data
            val readData = byteArrayOf(
                0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x02.toByte(),
                minOf(nlen, 255).toByte()
            )
            response = isoDep.transceive(readData)
            isoDep.close()
            if (!isSuccess(response)) return null

            val actualPayload = response.copyOfRange(0, response.size - 2)
            val msg = NdefMessage(actualPayload)
            Log.i(TAG, "Read NDEF via IsoDep Type 4 Tag: ${msg.records.size} records")
            msg
        } catch (e: Exception) {
            try { isoDep.close() } catch (_: Exception) {}
            Log.i(TAG, "IsoDep read completed/closed: ${e.message}")
            null
        }
    }

    private fun isSuccess(response: ByteArray?): Boolean {
        if (response == null || response.size < 2) return false
        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        return sw1 == 0x90.toByte() && sw2 == 0x00.toByte()
    }
}
