package com.ing.zkflow.zinc.witness

import com.ing.zkflow.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
@Tag("slow")
class Bits2BytesTest {
    private val log = loggerFor<Bits2BytesTest>()
    private val runOnly = true
    private val merkleRootOnly = false

    private val circuitFolder: String = javaClass.getResource("/witness/TestBits2Bytes").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private var setupTimeBits2Bytes: Double = 0.0

    init {
        if (!runOnly) {
            setupTimeBits2Bytes = measureTime { zincZKService.setup() }.inSeconds
        }
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }
    @Test
    fun `zinc computes the Merkle root and converts bits to bytes`() {
        val witnessJson = createSerializedWitnessInBits()
        val runTimeBits2Bytes = measureTime { zincZKService.run(witnessJson, getPublicDataJson(merkleRootOnly)) }.inSeconds

        if (!runOnly) {
            val timeResults = File(circuitFolder).resolve("timings.txt")
            timeResults.writeText("Measuring performance for Bits2bytes (in seconds)")

            var proofInBits: ByteArray
            val proofTimeBits2Bytes =
                measureTime { proofInBits = zincZKService.prove(witnessJson) }.inSeconds

            val verifyTimeBits2Bytes =
                measureTime { zincZKService.verify(proofInBits, getPublicDataJson(merkleRootOnly)) }.inSeconds

            timeResults.appendText("Setup : $setupTimeBits2Bytes")
            timeResults.appendText("Prove : $proofTimeBits2Bytes")
            timeResults.appendText("Verify : $verifyTimeBits2Bytes")
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
}
