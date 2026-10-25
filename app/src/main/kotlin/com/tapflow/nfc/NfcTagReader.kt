package com.tapflow.nfc

import android.nfc.Tag
import com.tapflow.nfc.model.TagData

/**
 * Handles reading from an Android framework [Tag] object.
 */
object NfcTagReader {

    /**
     * Extracts normalized [TagData] from an Android NFC [Tag].
     */
    fun readTag(tag: Tag?): TagData? {
        if (tag == null) return null
        val id = tag.id
        val techList = tag.techList
        return TagIdentifier.parse(id, techList)
    }
}
