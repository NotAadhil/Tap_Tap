package com.tapflow.nfc.writer

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/**
 * Result of attempting to write or format an NFC tag.
 */
sealed interface TagWriteResult {
    object Success : TagWriteResult
    data class Error(val message: String) : TagWriteResult
}

/**
 * Low-level utility for reading, writing, and formatting physical NFC tags with NDEF data.
 */
object NfcTagWriter {

    /**
     * Writes an [NdefMessage] to the provided [Tag].
     * Supports both pre-formatted [Ndef] tags and unformatted [NdefFormatable] tags.
     */
    fun writeMessage(tag: Tag, message: NdefMessage): TagWriteResult {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return TagWriteResult.Error("This tag is locked or write-protected.")
                }
                val msgBytes = message.toByteArray()
                if (ndef.maxSize < msgBytes.size) {
                    ndef.close()
                    return TagWriteResult.Error("Payload too large: ${msgBytes.size} bytes (Tag capacity: ${ndef.maxSize} bytes).")
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                TagWriteResult.Success
            } catch (e: Exception) {
                try { ndef.close() } catch (_: Exception) {}
                TagWriteResult.Error(e.message ?: "Failed to write to NFC tag.")
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return try {
                formatable.connect()
                formatable.format(message)
                formatable.close()
                TagWriteResult.Success
            } catch (e: Exception) {
                try { formatable.close() } catch (_: Exception) {}
                TagWriteResult.Error(e.message ?: "Failed to format and write to NFC tag.")
            }
        }

        return TagWriteResult.Error("This tag does not support NDEF formatting or writing.")
    }

    /**
     * Reads the current [NdefMessage] payload from an NFC [Tag], if available.
     * Useful for cloning tags.
     */
    fun readMessage(tag: Tag): NdefMessage? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage
            ndef.close()
            msg
        } catch (_: Exception) {
            try { ndef.close() } catch (_: Exception) {}
            null
        }
    }

    /**
     * Permanently locks an NFC tag to make it read-only.
     * Warning: This action cannot be reversed on most NFC tags (NTAG213/215/216).
     */
    fun makeReadOnly(tag: Tag): TagWriteResult {
        val ndef = Ndef.get(tag)
            ?: return TagWriteResult.Error("Tag does not support NDEF locking.")

        return try {
            ndef.connect()
            if (!ndef.canMakeReadOnly()) {
                ndef.close()
                return TagWriteResult.Error("This tag hardware does not support permanent write-lock.")
            }
            val success = ndef.makeReadOnly()
            ndef.close()
            if (success) TagWriteResult.Success else TagWriteResult.Error("Failed to lock NFC tag.")
        } catch (e: Exception) {
            try { ndef.close() } catch (_: Exception) {}
            TagWriteResult.Error(e.message ?: "Failed to lock NFC tag.")
        }
    }
}
