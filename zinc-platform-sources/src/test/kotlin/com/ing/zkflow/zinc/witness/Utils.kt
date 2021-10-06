package com.ing.zkflow.zinc.witness

import com.ing.dlt.zkkrypto.util.asUnsigned

data class Witness(
    val inputs: List<ByteArray> = List(ComponentSizes.INPUT_GROUP_SIZE) { ByteArray(ComponentSizes.INPUT_COMPONENT_SIZE) },
    val outputs: List<ByteArray> = List(ComponentSizes.OUTPUT_GROUP_SIZE) { ByteArray(ComponentSizes.OUTPUT_COMPONENT_SIZE) },
    val references: List<ByteArray> = List(ComponentSizes.REFERENCE_GROUP_SIZE) { ByteArray(ComponentSizes.REFERENCE_COMPONENT_SIZE) },
    val commands: List<ByteArray> = List(ComponentSizes.COMMAND_GROUP_SIZE) { ByteArray(ComponentSizes.COMMAND_COMPONENT_SIZE) },
    val attachments: List<ByteArray> = List(ComponentSizes.ATTACHMENT_GROUP_SIZE) { ByteArray(ComponentSizes.ATTACHMENT_COMPONENT_SIZE) },
    val notary: List<ByteArray> = List(ComponentSizes.NOTARY_GROUP_SIZE) { ByteArray(ComponentSizes.NOTARY_COMPONENT_SIZE) },
    val timeWindow: List<ByteArray> = List(ComponentSizes.TIMEWINDOW_GROUP_SIZE) { ByteArray(ComponentSizes.TIMEWINDOW_COMPONENT_SIZE) },
    val parameters: List<ByteArray> = List(ComponentSizes.PARAMETER_GROUP_SIZE) { ByteArray(ComponentSizes.PARAMETER_COMPONENT_SIZE) },
    val signers: List<ByteArray> = List(ComponentSizes.SIGNER_GROUP_SIZE) { ByteArray(ComponentSizes.COMMAND_SIGNER_SIZE) },

    val privacySalt: ByteArray = ByteArray(ComponentSizes.PRIVACY_SALT_SIZE),

    val inputNonces: List<ByteArray> = List(ComponentSizes.INPUT_GROUP_SIZE) { ByteArray(ComponentSizes.NONCE_DIGEST_SIZE) },
    val referenceNonces: List<ByteArray> = List(ComponentSizes.REFERENCE_GROUP_SIZE) { ByteArray(ComponentSizes.NONCE_DIGEST_SIZE) },

    // val serializedInputUTXOs: InputUTXOs,
    val serializedReferenceUTXOs: ReferenceUTXOs = ReferenceUTXOs()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Witness

        if (!privacySalt.contentEquals(other.privacySalt)) return false

        return true
    }

    override fun hashCode(): Int {
        return privacySalt.contentHashCode()
    }
}

data class ReferenceUTXOs(
    val referenceState1: List<ByteArray> = List(ComponentSizes.REFERENCE_STATE_1_GROUP_SIZE) { ByteArray(ComponentSizes.REFERENCE_STATE_1_COMPONENT_SIZE) },
    val referenceState2: List<ByteArray> = List(ComponentSizes.REFERENCE_STATE_2_GROUP_SIZE) { ByteArray(ComponentSizes.REFERENCE_STATE_2_COMPONENT_SIZE) },
    val referenceState3: List<ByteArray> = List(ComponentSizes.REFERENCE_STATE_3_GROUP_SIZE) { ByteArray(ComponentSizes.REFERENCE_STATE_3_COMPONENT_SIZE) },
)

// data class InputUTXOs(
// val inputState1: List<ByteArray>
// )

fun getPublicDataJson(merkleRootOnly: Boolean): String {
//    // Public output with Pedersen hash
//    val transactionId = byteArrayOf(18, -100, 6, 36, 30, 91, 112, -72, 112, 19, -124, 36, 40, 99, 18, -37, -74, 124, -111, -48, 42, 6, -123, -1, 1, 10, 55, 30, 83, 19, 15, 4)
//
//    val referenceHashes = listOf(
//        byteArrayOf(31, 93, 103, -128, 19, -1, 78, 44, -113, -77, -46, -52, 112, -80, 47, 52, 27, -119, -5, 69, 89, -70, -81, -78, -24, 22, -116, -109, 33, 80, 51, -10),
//        byteArrayOf(31, 93, 103, -128, 19, -1, 78, 44, -113, -77, -46, -52, 112, -80, 47, 52, 27, -119, -5, 69, 89, -70, -81, -78, -24, 22, -116, -109, 33, 80, 51, -10),
//        byteArrayOf(31, 93, 103, -128, 19, -1, 78, 44, -113, -77, -46, -52, 112, -80, 47, 52, 27, -119, -5, 69, 89, -70, -81, -78, -24, 22, -116, -109, 33, 80, 51, -10),
//
//        byteArrayOf(44, -69, 83, 79, 115, -70, -44, -16, -45, 94, 122, -78, -75, -96, 23, -93, -6, -87, 68, -88, 39, 118, 14, 118, 14, 36, -124, -85, -60, 62, 43, 9),
//        byteArrayOf(44, -69, 83, 79, 115, -70, -44, -16, -45, 94, 122, -78, -75, -96, 23, -93, -6, -87, 68, -88, 39, 118, 14, 118, 14, 36, -124, -85, -60, 62, 43, 9),
//        byteArrayOf(44, -69, 83, 79, 115, -70, -44, -16, -45, 94, 122, -78, -75, -96, 23, -93, -6, -87, 68, -88, 39, 118, 14, 118, 14, 36, -124, -85, -60, 62, 43, 9),
//
//        byteArrayOf(32, 46, -82, -56, 10, 99, -37, -102, -58, 73, -13, 48, -2, -64, 14, -103, 26, -111, -45, 7, -93, -16, 55, -22, 95, -26, -110, -8, 92, -99, 33, -71)
//    )

    // Public output with Blake2s hash
    val transactionId = byteArrayOf(30, -100, -20, -104, 109, -26, -55, -32, 83, -111, -64, 115, -124, -111, -29, 34, -101, 92, 21, -85, -24, 58, -74, -127, -92, -55, -27, -21, -64, 79, 90, 118)
    val referenceHashes = listOf(
        byteArrayOf(-42, 3, 109, -48, 100, -13, -106, 78, 108, -119, 69, -119, 116, 76, -48, -127, -11, -29, 95, -119, 52, 83, -71, 127, 50, -110, 2, 93, -122, 103, 118, 106),
        byteArrayOf(-42, 3, 109, -48, 100, -13, -106, 78, 108, -119, 69, -119, 116, 76, -48, -127, -11, -29, 95, -119, 52, 83, -71, 127, 50, -110, 2, 93, -122, 103, 118, 106),
        byteArrayOf(-42, 3, 109, -48, 100, -13, -106, 78, 108, -119, 69, -119, 116, 76, -48, -127, -11, -29, 95, -119, 52, 83, -71, 127, 50, -110, 2, 93, -122, 103, 118, 106),
        byteArrayOf(24, 127, 8, 103, -70, -84, -71, 90, -6, 61, 80, -1, -24, 84, 14, 68, -76, 40, 91, -120, 53, -94, -85, 47, -76, -26, 52, 3, -96, -126, -109, 100),
        byteArrayOf(24, 127, 8, 103, -70, -84, -71, 90, -6, 61, 80, -1, -24, 84, 14, 68, -76, 40, 91, -120, 53, -94, -85, 47, -76, -26, 52, 3, -96, -126, -109, 100),
        byteArrayOf(24, 127, 8, 103, -70, -84, -71, 90, -6, 61, 80, -1, -24, 84, 14, 68, -76, 40, 91, -120, 53, -94, -85, 47, -76, -26, 52, 3, -96, -126, -109, 100),
        byteArrayOf(87, -110, 25, 125, -94, -40, 69, 61, -37, 20, -28, -97, 75, 86, 94, -90, -2, 91, 89, -63, -3, -66, -5, 78, -35, -112, -18, 73, 76, 5, -60, -112)
    )

    return if (!merkleRootOnly) {
        "{\"input_hashes\":[]," +
            "\"reference_hashes\": ${referenceHashes.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"transaction_id\": ${transactionId.map { "\"${it.asUnsigned()}\"" }}}"
    } else {
        "{\"transaction_id\": ${transactionId.map { "\"${it.asUnsigned()}\"" }}}"
    }
}

fun Byte.toBits(): List<String> {
    val bits = MutableList(ComponentSizes.BYTE_BITS) { "false" }

    for (index in 0..ComponentSizes.BYTE_BITS) {
        if ((this.toInt().shr(index) and 1) == 1) {
            bits[ComponentSizes.BYTE_BITS - 1 - index] = "true"
        }
    }
    return bits
}
