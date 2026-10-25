package com.tapflow.nfc

import com.tapflow.nfc.model.TagData

/**
 * Utility responsible for extracting reliable identifiers from raw NFC tag data,
 * checking identifier stability, detecting random UIDs, and flagging payment cards.
 */
object TagIdentifier {

    /**
     * Converts a byte array to an uppercase hexadecimal string.
     */
    fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return ""
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    /**
     * Detects if the UID is a dynamic/random identifier.
     * Per ISO/IEC 14443-3, a 4-byte UID starting with 0x08 indicates a Random Identifier (RID)
     * which regenerates each time the tag is power-cycled in the RF field.
     */
    fun isRandomUid(id: ByteArray?): Boolean {
        if (id == null || id.isEmpty()) return false
        val firstByte = id[0].toInt() and 0xFF
        return firstByte == 0x08
    }

    /**
     * Determines whether the tag is likely a secured payment card (EMV/Credit/Debit card).
     * Payment cards generally support only IsoDep without user-programmable tags like Ndef/NfcA/NfcV.
     */
    fun isPotentialPaymentCard(techList: List<String>): Boolean {
        val simpleTechs = techList.map { it.substringAfterLast('.') }
        val hasIsoDep = simpleTechs.contains("IsoDep")
        val hasWritableTech = simpleTechs.any { it in listOf("Ndef", "NdefFormatable", "MifareClassic", "MifareUltralight", "NfcV") }
        val hasStandardTagTech = simpleTechs.any { it in listOf("NfcA", "NfcB", "NfcF") }

        // If it only exposes IsoDep without standard NFC-A/B or NDEF tech, treat as payment/banking card
        return hasIsoDep && !hasWritableTech && !hasStandardTagTech
    }

    /**
     * Builds the composite tagKey: "${uidHex}:${sortedTechList}".
     * Combining the hardware UID with the sorted tech list creates a collision-resistant key.
     */
    fun buildCompositeKey(uidHex: String, techList: List<String>): String {
        val sortedTechs = techList
            .map { it.substringAfterLast('.') }
            .distinct()
            .sorted()
            .joinToString(",")
        return if (sortedTechs.isNotEmpty()) "$uidHex:$sortedTechs" else uidHex
    }

    /**
     * Parses raw UID bytes and tech list array into a strongly-typed [TagData].
     */
    fun parse(id: ByteArray?, techArray: Array<String>?): TagData {
        val uidHex = bytesToHex(id)
        val techList = techArray?.toList() ?: emptyList()
        val hasRandom = isRandomUid(id)
        val isPayment = isPotentialPaymentCard(techList)
        val compositeKey = buildCompositeKey(uidHex, techList)

        return TagData(
            tagKey = compositeKey,
            rawUidHex = uidHex,
            techList = techList,
            hasRandomUid = hasRandom,
            isPaymentCard = isPayment
        )
    }
}
