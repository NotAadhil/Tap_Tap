package com.tapflow.nfc

import com.tapflow.nfc.hce.NdefPayloadBuilder
import com.tapflow.nfc.hce.Type4TagEmulator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Type4TagEmulatorTest {

    @Test
    fun testNdefPayloadBuilder_buildsValidUriRecord() {
        val payload = NdefPayloadBuilder.buildUriNdef("https://github.com/NotAadhil/Tap_Tap")
        assertTrue(payload.isNotEmpty())
        assertEquals(0xD1.toByte(), payload[0]) // Flags
        assertEquals(0x01.toByte(), payload[1]) // Type Length ('U')
        assertEquals(0x55.toByte(), payload[3]) // Type 'U'
        assertEquals(0x04.toByte(), payload[4]) // https:// prefix code
    }

    @Test
    fun testNdefPayloadBuilder_buildsValidTextRecord() {
        val payload = NdefPayloadBuilder.buildTextNdef("Hello TapFlow")
        assertTrue(payload.isNotEmpty())
        assertEquals(0xD1.toByte(), payload[0])
        assertEquals(0x01.toByte(), payload[1])
        assertEquals(0x54.toByte(), payload[3]) // Type 'T'
    }

    @Test
    fun testEmulator_selectNdefApplicationSucceeds() {
        val emulator = Type4TagEmulator(isEnabled = true)
        // 00 A4 04 00 07 D2 76 00 00 85 01 01
        val selectNdefApdu = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )
        val response = emulator.processCommand(selectNdefApdu)
        assertArrayEquals(Type4TagEmulator.STATUS_SUCCESS, response)
    }

    @Test
    fun testEmulator_fullType4TagReadFlow() {
        val ndefBytes = NdefPayloadBuilder.buildUriNdef("https://example.com")
        val emulator = Type4TagEmulator(isEnabled = true, ndefPayload = ndefBytes)

        // 1. Select NDEF Application (AID D2760000850101)
        val selectApp = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )
        assertArrayEquals(Type4TagEmulator.STATUS_SUCCESS, emulator.processCommand(selectApp))

        // 2. Select CC File (E1 03)
        val selectCc = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x03.toByte()
        )
        assertArrayEquals(Type4TagEmulator.STATUS_SUCCESS, emulator.processCommand(selectCc))

        // 3. Read CC File (00 B0 00 00 0F)
        val readCc = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0F.toByte()
        )
        val ccResponse = emulator.processCommand(readCc)
        assertEquals(17, ccResponse.size) // 15 bytes data + 90 00
        assertEquals(0x90.toByte(), ccResponse[15])
        assertEquals(0x00.toByte(), ccResponse[16])

        // 4. Select NDEF File (E1 04)
        val selectNdef = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(),
            0xE1.toByte(), 0x04.toByte()
        )
        assertArrayEquals(Type4TagEmulator.STATUS_SUCCESS, emulator.processCommand(selectNdef))

        // 5. Read NDEF Length (first 2 bytes, offset 0, Le 2)
        val readNdefLen = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte()
        )
        val lenResponse = emulator.processCommand(readNdefLen)
        assertEquals(4, lenResponse.size) // 2 bytes length + 90 00
        val nlen = ((lenResponse[0].toInt() and 0xFF) shl 8) or (lenResponse[1].toInt() and 0xFF)
        assertEquals(ndefBytes.size, nlen)

        // 6. Read NDEF Data (offset 2, Le = ndefBytes.size)
        val readNdefData = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x02.toByte(), ndefBytes.size.toByte()
        )
        val dataResponse = emulator.processCommand(readNdefData)
        assertEquals(ndefBytes.size + 2, dataResponse.size)
        assertEquals(0x90.toByte(), dataResponse[dataResponse.size - 2])
        assertEquals(0x00.toByte(), dataResponse[dataResponse.size - 1])
    }

    @Test
    fun testEmulator_disabledReturnsFailed() {
        val emulator = Type4TagEmulator(isEnabled = false)
        val selectApp = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )
        val response = emulator.processCommand(selectApp)
        assertArrayEquals(Type4TagEmulator.STATUS_FAILED, response)
    }
}
