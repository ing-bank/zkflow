package com.ing.zknotary.zinc.witness

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class WitnessInBitsTest {
    private val log = loggerFor<WitnessInBitsTest>()
    private val runOnly = false
    private val merkleRootOnly = true

    private val circuitFolderBits2Bytes: String = javaClass.getResource("/witness/TestBits2Bytes").path
    private val zincZKServiceBits2Bytes = ZincZKService(
        circuitFolderBits2Bytes,
        artifactFolder = circuitFolderBits2Bytes,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val circuitFolderBytes2Bits: String = javaClass.getResource("/witness/TestBytes2Bits").path
    private val zincZKServiceBytes2Bits = ZincZKService(
        circuitFolderBytes2Bits,
        artifactFolder = circuitFolderBytes2Bits,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private var setupTimeBits2Bytes: Double = 0.0
    private var setupTimeBytes2Bits: Double = 0.0

    init {
        if (!runOnly) {
            setupTimeBits2Bytes = measureTime { zincZKServiceBits2Bytes.setup() }.inSeconds
            setupTimeBytes2Bits = measureTime { zincZKServiceBytes2Bits.setup() }.inSeconds
        }
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKServiceBits2Bytes.cleanup()
        zincZKServiceBytes2Bits.cleanup()
    }

    @Test
    fun `zinc computes the Merkle root and converts bits to bytes`() {
        val witnessJsonInBits = createSerializedWitnessInBits()
        println("Measuring performance for Bits2bytes")

        val runTimeBits2Bytes = measureTime { zincZKServiceBits2Bytes.run(witnessJsonInBits, getPublicDataJson()) }.inSeconds

        if (!runOnly) {
            var proofInBits: ByteArray
            val proofTimeBits2Bytes =
                measureTime { proofInBits = zincZKServiceBits2Bytes.prove(witnessJsonInBits) }.inSeconds

            val verifyTimeBits2Bytes =
                measureTime { zincZKServiceBits2Bytes.verify(proofInBits, getPublicDataJson()) }.inSeconds

            println("Timings:")
            println("Setup --> $setupTimeBits2Bytes")
            println("Run --> $runTimeBits2Bytes")
            println("Prove --> $proofTimeBits2Bytes")
            println("Verify --> $verifyTimeBits2Bytes")
        }
    }

    @Test
    fun `zinc converts bytes to bits and computes the Merkle root`() {
        val witnessJsonInBytes = createSerializedWitnessInBytes()
        println("Measuring performance for Bytes2Bits")

        val runTimeBytes2Bits = measureTime { zincZKServiceBytes2Bits.run(witnessJsonInBytes, getPublicDataJson()) }.inSeconds

        if (!runOnly) {
            var proofInBytes: ByteArray
            val proofTimeBytes2Bits =
                measureTime { proofInBytes = zincZKServiceBytes2Bits.prove(witnessJsonInBytes) }.inSeconds

            val verifyTimeBytes2Bits =
                measureTime { zincZKServiceBytes2Bits.verify(proofInBytes, getPublicDataJson()) }.inSeconds

            println("Timings:")
            println("Setup --> $setupTimeBytes2Bits")
            println("Run --> $runTimeBytes2Bits")
            println("Prove --> $proofTimeBytes2Bits")
            println("Verify --> $verifyTimeBytes2Bits")
        }
    }

    @Test
    fun `zinc compares the performance of bit and byte witness`() {
        val witnessJsonInBytes = createSerializedWitnessInBytes()
        val witnessJsonInBits = createSerializedWitnessInBits()

        println("Measuring performance for Bits2bytes")
        val runTimeBits2Bytes = measureTime { zincZKServiceBits2Bytes.run(witnessJsonInBits, getPublicDataJson()) }.inSeconds

        if (!runOnly) {
            var proofInBits: ByteArray
            val proofTimeBits2Bytes =
                measureTime { proofInBits = zincZKServiceBits2Bytes.prove(witnessJsonInBits) }.inSeconds

            val verifyTimeBits2Bytes =
                measureTime { zincZKServiceBits2Bytes.verify(proofInBits, getPublicDataJson()) }.inSeconds

            println("Timings:")
            println("Setup --> $setupTimeBits2Bytes")
            println("Run --> $runTimeBits2Bytes")
            println("Prove --> $proofTimeBits2Bytes")
            println("Verify --> $verifyTimeBits2Bytes")
        }

        println("Measuring performance for Bytes2Bits")
        val runTimeBytes2Bits = measureTime { zincZKServiceBytes2Bits.run(witnessJsonInBytes, getPublicDataJson()) }.inSeconds

        if (!runOnly) {
            var proofInBytes: ByteArray
            val proofTimeBytes2Bits =
                measureTime { proofInBytes = zincZKServiceBytes2Bits.prove(witnessJsonInBytes) }.inSeconds

            val verifyTimeBytes2Bits =
                measureTime { zincZKServiceBytes2Bits.verify(proofInBytes, getPublicDataJson()) }.inSeconds

            println("Timings:")
            println("Setup --> $setupTimeBytes2Bits")
            println("Run --> $runTimeBytes2Bits")
            println("Prove --> $proofTimeBytes2Bits")
            println("Verify --> $verifyTimeBytes2Bits")
        }
    }

    private fun createSerializedWitnessInBits(): String {
        val witness = Witness()
        return "{" +
            "\"witness\": {" +
            "\"inputs\": ${witness.inputs.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"outputs\": ${witness.outputs.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"references\": ${witness.references.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"commands\": ${witness.commands.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"attachments\": ${witness.attachments.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"notary\": ${witness.notary.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"time_window\": ${witness.timeWindow.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"parameters\": ${witness.parameters.map { it.map { byte -> byte.toBits() }.flatten() }}," +
            "\"signers\": ${witness.signers.map { it.map { byte -> byte.toBits() }.flatten() }}," +

            "\"privacy_salt\": ${witness.privacySalt.map { byte -> byte.toBits() }.flatten()}" +
            if (!merkleRootOnly) {
                ",\"input_nonces\": ${witness.inputNonces.map { it.map { byte -> byte.toBits() }.flatten() }}," +
                    "\"reference_nonces\": ${witness.referenceNonces.map { it.map { byte -> byte.toBits() }.flatten() }}," +
                    "\"serialized_reference_utxos\": {" +
                    " \"reference_state_1\": ${witness.serializedReferenceUTXOs.referenceState1.map { it.map { byte -> byte.toBits() }.flatten() }}," +
                    " \"reference_state_2\": ${witness.serializedReferenceUTXOs.referenceState2.map { it.map { byte -> byte.toBits() }.flatten() }}," +
                    " \"reference_state_3\": ${witness.serializedReferenceUTXOs.referenceState3.map { it.map { byte -> byte.toBits() }.flatten() }}" +
                    "}" +
                    "} }"
            } else {
                "} }"
            }
    }

    private fun createSerializedWitnessInBytes(): String {
        val witness = Witness()
        return "{" +
            "\"witness\": {" +
            "\"inputs\": ${witness.inputs.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"outputs\": ${witness.outputs.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"references\": ${witness.references.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"commands\": ${witness.commands.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"attachments\": ${witness.attachments.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"notary\": ${witness.notary.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"time_window\": ${witness.timeWindow.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"parameters\": ${witness.parameters.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
            "\"signers\": ${witness.signers.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +

            "\"privacy_salt\": ${witness.privacySalt.map { byte -> "\"${byte.asUnsigned()}\"" }}" +
            if (!merkleRootOnly) {
                ",\"input_nonces\": ${witness.inputNonces.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
                    "\"reference_nonces\": ${witness.referenceNonces.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
                    "\"serialized_reference_utxos\": {" +
                    " \"reference_state_1\": ${witness.serializedReferenceUTXOs.referenceState1.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
                    " \"reference_state_2\": ${witness.serializedReferenceUTXOs.referenceState2.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}," +
                    " \"reference_state_3\": ${witness.serializedReferenceUTXOs.referenceState3.map { it.map { byte -> "\"${byte.asUnsigned()}\"" } }}" +
                    "}" +
                    "} }"
            } else {
                "} }"
            }
    }

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

    private fun getPublicDataJson(): String {
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

    private fun Byte.toBits(): List<String> {
        val bits = MutableList(ComponentSizes.BYTE_BITS) { "false" }

        for (index in 0..ComponentSizes.BYTE_BITS) {
            if ((this.toInt().shr(index) and 1) == 1) {
                bits[ComponentSizes.BYTE_BITS - 1 - index] = "true"
            }
        }
        return bits
    }
}
