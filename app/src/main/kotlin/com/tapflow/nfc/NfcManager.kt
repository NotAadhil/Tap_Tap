package com.tapflow.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.tapflow.nfc.model.TagData

/**
 * Manages the Android [NfcAdapter] lifecycle and tag dispatch.
 * Utilizes [NfcAdapter.enableReaderMode] for high-priority, zero-chooser foreground tag discovery.
 */
class NfcManager(
    context: Context,
    private val coordinator: NfcDispatchCoordinator
) : NfcAdapter.ReaderCallback {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context.applicationContext)

    val isNfcAvailable: Boolean
        get() = adapter != null

    val isNfcEnabled: Boolean
        get() = adapter?.isEnabled == true

    /**
     * Enables foreground reader mode on the provided [Activity].
     * Flags ensure standard contactless NFC tags (NfcA, NfcB, NfcF, NfcV) are captured
     * while skipping Android Beam / P2P pairing.
     */
    fun startReaderMode(activity: Activity) {
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        adapter?.enableReaderMode(activity, this, flags, null)
    }

    /**
     * Disables foreground reader mode when the activity is paused.
     */
    fun stopReaderMode(activity: Activity) {
        adapter?.disableReaderMode(activity)
    }

    /**
     * Callback invoked by Android's NFC service on a background thread when a tag is detected
     * during active ReaderMode.
     */
    override fun onTagDiscovered(tag: Tag?) {
        val tagData = NfcTagReader.readTag(tag) ?: return
        coordinator.onTagDetected(tagData)
    }

    /**
     * Entry point for tags delivered via intent dispatch (such as background cold start
     * or onNewIntent).
     */
    fun handleTag(tag: Tag) {
        val tagData = NfcTagReader.readTag(tag) ?: return
        coordinator.onTagDetected(tagData)
    }

    /**
     * Allows manual or simulated tag dispatch for testing environments where NFC hardware is absent.
     */
    fun simulateTag(tagData: TagData) {
        coordinator.onTagDetected(tagData)
    }
}
