package com.tapflow.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagIdentifierTest {

    @Test
    fun bytesToHex_convertsByteArrayCorrectly() {
        val bytes = byteArrayOf(0x04, 0xA3.toByte(), 0xBC.toByte(), 0x7D.toByte())
        val hex = TagIdentifier.bytesToHex(bytes)
        assertEquals("04A3BC7D", hex)
    }

    @Test
    fun bytesToHex_handlesNullAndEmpty() {
        assertEquals("", TagIdentifier.bytesToHex(null))
        assertEquals("", TagIdentifier.bytesToHex(byteArrayOf()))
    }

    @Test
    fun isRandomUid_detects0x08Prefix() {
        val randomUid = byteArrayOf(0x08, 0x12, 0x34, 0x56)
        assertTrue("UID starting with 0x08 must be flagged as random", TagIdentifier.isRandomUid(randomUid))

        val standardUid = byteArrayOf(0x04, 0xA3.toByte(), 0xBC.toByte(), 0x7D.toByte())
        assertFalse("Standard NXP UID starting with 0x04 must not be flagged as random", TagIdentifier.isRandomUid(standardUid))
    }

    @Test
    fun isPotentialPaymentCard_detectsIsolatedIsoDep() {
        val paymentCardTechs = listOf("android.nfc.tech.IsoDep")
        assertTrue("Isolated IsoDep must be flagged as payment/access card", TagIdentifier.isPotentialPaymentCard(paymentCardTechs))

        val regularTagTechs = listOf("android.nfc.tech.NfcA", "android.nfc.tech.IsoDep", "android.nfc.tech.Ndef")
        assertFalse("Tag with NfcA and Ndef alongside IsoDep should not be flagged as payment card", TagIdentifier.isPotentialPaymentCard(regularTagTechs))
    }

    @Test
    fun buildCompositeKey_sortsAndFormatsCorrectly() {
        val techs = listOf("android.nfc.tech.Ndef", "android.nfc.tech.NfcA", "android.nfc.tech.MifareUltralight")
        val composite = TagIdentifier.buildCompositeKey("04A3BC7D", techs)
        assertEquals("04A3BC7D:MifareUltralight,Ndef,NfcA", composite)
    }

    @Test
    fun parse_createsCompleteTagData() {
        val uid = byteArrayOf(0x04, 0x11, 0x22, 0x33)
        val techs = arrayOf("android.nfc.tech.NfcA", "android.nfc.tech.Ndef")
        val tagData = TagIdentifier.parse(uid, techs)

        assertEquals("04112233:Ndef,NfcA", tagData.tagKey)
        assertEquals("04112233", tagData.rawUidHex)
        assertFalse(tagData.hasRandomUid)
        assertFalse(tagData.isPaymentCard)
        assertEquals(2, tagData.techList.size)
    }
}
