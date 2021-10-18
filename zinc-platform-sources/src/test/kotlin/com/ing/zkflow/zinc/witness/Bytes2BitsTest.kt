package com.ing.zkflow.zinc.witness

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zkflow.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime

@Tag("slow")
@Disabled("This is a benchmark. Should only be enabled for benchmarks")
class Bytes2BitsTest {
    private val runOnly = true
    private val merkleRootOnly = false

    private val circuitFolder: String = javaClass.getResource("/witness/TestBytes2Bits").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private var setupTimeBytes2Bits: Double = 0.0

    init {
        if (!runOnly) {
            setupTimeBytes2Bits = measureTime { zincZKService.setup() }.toDouble(DurationUnit.SECONDS)
        }
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc converts bytes to bits and computes the Merkle root`() {
        val witnessJsonInBytes = createSerializedWitnessInBytes()
        measureTime { zincZKService.run(witnessJsonInBytes, getPublicDataJson(merkleRootOnly)) }.toDouble(DurationUnit.SECONDS)

        if (!runOnly) {
            val timeResults = File(circuitFolder).resolve("timings.txt")
            timeResults.writeText("Measuring performance for Bytes2Bits (in seconds)")

            var proofInBytes: ByteArray
            val proofTimeBytes2Bits =
                measureTime { proofInBytes = zincZKService.prove(witnessJsonInBytes) }.toDouble(DurationUnit.SECONDS)

            val verifyTimeBytes2Bits =
                measureTime { zincZKService.verify(proofInBytes, getPublicDataJson(merkleRootOnly)) }.toDouble(DurationUnit.SECONDS)

            timeResults.appendText("Setup : $setupTimeBytes2Bits")
            timeResults.appendText("Prove : $proofTimeBytes2Bits")
            timeResults.appendText("Verify : $verifyTimeBytes2Bits")
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
}
