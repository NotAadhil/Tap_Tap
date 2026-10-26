package com.tapflow.nfc

import android.nfc.Tag
import com.tapflow.nfc.model.TagData

/**
 * Handles reading from an Android framework [Tag] object.
 */
object NfcTagReader {

    /**
     * Extracts normalized [TagData] from an Android NFC [Tag].
     * Reads hardware UID, technology list, and attempts to extract any NDEF URI or Text payload
     * (supporting physical NFC tags, Type 4 smart cards, and active phone-to-phone HCE beacons).
     */
    fun readTag(tag: Tag?): TagData? {
        if (tag == null) return null
        val id = tag.id
        val techList = tag.techList
        val baseData = TagIdentifier.parse(id, techList)

        // Attempt reading NDEF payload (supports Type 4 Smart Cards & HCE Beacons)
        val ndefMessage = Type4TagReader.readType4Tag(tag)
        val extracted = NdefPayloadParser.extractContent(ndefMessage)
        val isVirtual = baseData.hasRandomUid && (extracted.uri != null || extracted.text != null)

        return baseData.copy(
            ndefUri = extracted.uri,
            ndefText = extracted.text,
            isVirtualBeacon = isVirtual
        )
    }
}
