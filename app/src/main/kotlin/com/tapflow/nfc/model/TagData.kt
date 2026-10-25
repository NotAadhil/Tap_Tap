package com.tapflow.nfc.model

/**
 * Normalized data extracted from a scanned physical NFC tag.
 *
 * @param tagKey The unique composite identifier formatted as "${rawUidHex}:${sortedTechList}".
 * @param rawUidHex The hexadecimal string representation of the tag's raw UID.
 * @param techList The list of NFC technology classes supported by this tag.
 * @param hasRandomUid True if the tag uses a dynamically randomized UID (e.g. ISO 14443-3 0x08 prefix).
 * @param isPaymentCard True if the tag matches payment card patterns (e.g. isolated IsoDep with no NFC-A/B/F).
 */
data class TagData(
    val tagKey: String,
    val rawUidHex: String,
    val techList: List<String>,
    val hasRandomUid: Boolean = false,
    val isPaymentCard: Boolean = false
) : java.io.Serializable {
    val techSummary: String
        get() = techList.joinToString(", ") { it.substringAfterLast('.') }
}
