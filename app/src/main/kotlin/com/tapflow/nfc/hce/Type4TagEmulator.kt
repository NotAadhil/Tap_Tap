package com.tapflow.nfc.hce

/**
 * NFC Forum Type 4 Tag ISO-DEP APDU protocol engine.
 * Emulates an active ISO 14443-4 contactless smart card responder.
 */
class Type4TagEmulator(
    var isEnabled: Boolean = true,
    var ndefPayload: ByteArray = byteArrayOf()
) {
    private var selectedFile: FileType = FileType.NONE

    enum class FileType {
        NONE,
        CC,
        NDEF
    }

    companion object {
        val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        val STATUS_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val STATUS_CMD_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        val STATUS_WRONG_OFFSET = byteArrayOf(0x6B.toByte(), 0x00.toByte())

        // NFC Forum Type 4 Tag AID v2.0: D2 76 00 00 85 01 01
        val AID_NDEF_TYPE4_V2 = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // NFC Forum Type 4 Tag AID v1.0 / DESFire: D2 76 00 00 85 01 00
        val AID_NDEF_TYPE4_V1 = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x00.toByte()
        )

        // TapFlow Custom AID: F0 54 61 70 46 6C 6F 77
        val AID_TAPFLOW = byteArrayOf(
            0xF0.toByte(), 0x54.toByte(), 0x61.toByte(), 0x70.toByte(),
            0x46.toByte(), 0x6C.toByte(), 0x6F.toByte(), 0x77.toByte()
        )

        // Capability Container (CC) File ID: E1 03
        val FILE_ID_CC = byteArrayOf(0xE1.toByte(), 0x03.toByte())

        // NDEF File ID: E1 04
        val FILE_ID_NDEF = byteArrayOf(0xE1.toByte(), 0x04.toByte())

        // Capability Container File Content (15 bytes)
        // MLe=127 bytes, MLc=127 bytes, Max NDEF Size=2048 bytes, Read=0x00, Write=0x00
        val CC_FILE = byteArrayOf(
            0x00.toByte(), 0x0F.toByte(), // CCLEN: 15 bytes
            0x20.toByte(),               // Mapping Version 2.0
            0x00.toByte(), 0x7F.toByte(), // MLe: 127 bytes
            0x00.toByte(), 0x7F.toByte(), // MLc: 127 bytes
            0x04.toByte(),               // T: NDEF File Control TLV
            0x06.toByte(),               // L: 6 bytes
            0xE1.toByte(), 0x04.toByte(), // NDEF File ID
            0x08.toByte(), 0x00.toByte(), // Max NDEF File Size (2048 bytes)
            0x00.toByte(),               // Read access without security
            0x00.toByte()                // Write access granted (free write / read)
        )
    }

    fun processCommand(commandApdu: ByteArray): ByteArray {
        if (!isEnabled) {
            return STATUS_FAILED
        }
        if (commandApdu.size < 4) {
            return STATUS_FAILED
        }

        val ins = commandApdu[1]
        val p1 = commandApdu[2]
        val p2 = commandApdu[3]

        // Handle SELECT (INS = 0xA4)
        if (ins == 0xA4.toByte()) {
            return handleSelect(commandApdu, p1, p2)
        }

        // Handle READ BINARY (INS = 0xB0)
        if (ins == 0xB0.toByte()) {
            return handleReadBinary(p1, p2, commandApdu)
        }

        return STATUS_CMD_NOT_SUPPORTED
    }

    private fun handleSelect(apdu: ByteArray, p1: Byte, p2: Byte): ByteArray {
        // P1=0x04: Select by DF name (AID)
        if (p1 == 0x04.toByte()) {
            val lc = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
            if (apdu.size < 5 + lc) return STATUS_FILE_NOT_FOUND
            val aid = apdu.copyOfRange(5, 5 + lc)
            if (aid.contentEquals(AID_NDEF_TYPE4_V2) ||
                aid.contentEquals(AID_NDEF_TYPE4_V1) ||
                aid.contentEquals(AID_TAPFLOW)) {
                selectedFile = FileType.NONE
                return STATUS_SUCCESS
            }
            return STATUS_FILE_NOT_FOUND
        }

        // P1=0x00 or 0x02: Select by EF ID
        // P2 can be 0x00 (first occurrence) or 0x0C (no FCI)
        if ((p1 == 0x00.toByte() || p1 == 0x02.toByte()) &&
            (p2 == 0x0C.toByte() || p2 == 0x00.toByte())) {
            val lc = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
            if (apdu.size < 5 + lc || lc < 2) return STATUS_FILE_NOT_FOUND
            val fileId = apdu.copyOfRange(5, 7)
            return when {
                fileId.contentEquals(FILE_ID_CC) -> {
                    selectedFile = FileType.CC
                    STATUS_SUCCESS
                }
                fileId.contentEquals(FILE_ID_NDEF) -> {
                    selectedFile = FileType.NDEF
                    STATUS_SUCCESS
                }
                else -> STATUS_FILE_NOT_FOUND
            }
        }

        return STATUS_CMD_NOT_SUPPORTED
    }

    private fun handleReadBinary(p1: Byte, p2: Byte, apdu: ByteArray): ByteArray {
        val offset = ((p1.toInt() and 0xFF) shl 8) or (p2.toInt() and 0xFF)
        val le = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0

        val fileBytes = when (selectedFile) {
            FileType.CC -> CC_FILE
            FileType.NDEF -> buildNdefFileBytes()
            FileType.NONE -> return STATUS_FILE_NOT_FOUND
        }

        if (offset >= fileBytes.size) {
            return STATUS_WRONG_OFFSET
        }

        val lengthToRead = if (le == 0) {
            minOf(256, fileBytes.size - offset)
        } else {
            minOf(le, fileBytes.size - offset)
        }

        val result = ByteArray(lengthToRead + 2)
        System.arraycopy(fileBytes, offset, result, 0, lengthToRead)
        result[lengthToRead] = 0x90.toByte()
        result[lengthToRead + 1] = 0x00.toByte()
        return result
    }

    private fun buildNdefFileBytes(): ByteArray {
        val len = ndefPayload.size
        val ndefFile = ByteArray(len + 2)
        ndefFile[0] = ((len ushr 8) and 0xFF).toByte()
        ndefFile[1] = (len and 0xFF).toByte()
        System.arraycopy(ndefPayload, 0, ndefFile, 2, len)
        return ndefFile
    }

    fun onDeactivated() {
        selectedFile = FileType.NONE
    }
}
