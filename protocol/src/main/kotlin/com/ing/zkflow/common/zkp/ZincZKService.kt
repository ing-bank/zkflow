package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.serialization.zinc.json.PublicInputSerializer
import com.ing.zkflow.common.serialization.zinc.json.WitnessSerializer
import kotlinx.serialization.json.Json
import net.corda.core.utilities.loggerFor
import java.io.File
import java.io.File.createTempFile
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

class ZincZKService(
    val circuitFolder: String,
    val artifactFolder: String,
    private val buildTimeout: Duration,
    private val setupTimeout: Duration,
    private val provingTimeout: Duration,
    private val verificationTimeout: Duration
) : ZKService {

    private val circuitManifestPath = "$circuitFolder/Zargo.toml"
    private val defaultBuildPath = "$circuitFolder/build"
    private val defaultDataPath = "$circuitFolder/data"

    val compiledCircuitPath = "$defaultBuildPath/default.znb"
    val zkSetup = ZKSetup(
        provingKeyPath = "$defaultDataPath/proving_key",
        verifyingKeyPath = "$defaultDataPath/verifying_key.txt"
    )

    companion object {
        const val BUILD = "zargo build"
        const val SETUP = "zargo setup"
        const val PROVE = "zargo prove"
        const val RUN = "zargo run"
        const val VERIFY = "zargo verify"

        private val log = loggerFor<ZincZKService>()

        /**
         * Returns output of the command execution.
         **/
        fun completeZincCommand(command: String, timeout: Duration, input: File? = null): String {
            val process = command.toProcess(input)
            val output = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val hasCompleted = process.waitFor(timeout.seconds, TimeUnit.SECONDS)

            if (!hasCompleted) {
                process.destroy()
                error("$command ran longer than ${timeout.seconds} seconds")
            }

            return if (process.exitValue() != 0) {
                error("$command failed with the following output: $stderr")
            } else {
                log.debug(stderr)
                output
            }
        }

        private fun String.toProcess(input: File? = null): Process {
            return try {
                val builder = ProcessBuilder(split("\\s".toRegex()))
                if (input != null) {
                    builder.redirectInput(input)
                }
                builder.start()
            } catch (e: IOException) {
                error(e.localizedMessage)
            }
        }
    }

    data class ZKSetup(val provingKeyPath: String, val verifyingKeyPath: String)

    fun cleanup() {
        listOf(
            compiledCircuitPath,
            zkSetup.provingKeyPath,
            zkSetup.verifyingKeyPath
        ).forEach { File(it).delete() }
        File(defaultBuildPath).deleteRecursively()
        File(defaultDataPath).deleteRecursively()
    }

    fun setup() {
        val circuitManifest = File(circuitManifestPath)
        require(circuitManifest.exists()) { "Cannot find circuit manifest at $circuitManifestPath" }

        val witnessFile = createTempFile("zkp", null)
        val publicData = createTempFile("zkp", null)

        try {
            completeZincCommand(
                "$BUILD --manifest-path $circuitManifestPath --circuit $compiledCircuitPath " +
                    "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
                buildTimeout
            )
        } finally {
            // Neither witness, nor Public data carry useful information after build, they are just templates
            publicData.delete()
            witnessFile.delete()
        }
        require(File(compiledCircuitPath).exists()) { "Compile circuit not found in path $compiledCircuitPath." }

        completeZincCommand(
            "$SETUP --circuit $compiledCircuitPath " +
                "--proving-key ${zkSetup.provingKeyPath} --verifying-key ${zkSetup.verifyingKeyPath}",
            setupTimeout
        )
        require(File(zkSetup.provingKeyPath).exists()) { "Proving key not found at ${zkSetup.provingKeyPath}." }
    }

    override fun run(witness: Witness, publicInput: PublicInput): String {
        log.info("Witness size: ${witness.size()}")
        log.info("Padded Witness size: ${witness.size { it == 0.toByte() }}") // Assumes BFL zero-byte padding

        val witnessJson = Json.encodeToString(WitnessSerializer, witness)
        log.info("Witness JSON: $witnessJson")

        val publicInputJson = Json.encodeToString(PublicInputSerializer, publicInput)
        return run(witnessJson, publicInputJson)
    }

    fun run(witness: String, expectedOutput: String = ""): String {
        val witnessFile = createTempFile("zkp", null)
        witnessFile.writeText(witness)

        val publicDataFile = createTempFile("zkp", null)

        try {
            val result = completeZincCommand(
                "$RUN --circuit $compiledCircuitPath --manifest-path $circuitManifestPath " +
                    "--public-data ${publicDataFile.absolutePath} --witness ${witnessFile.absolutePath}",
                provingTimeout
            ).replace(" ", "")
                .replace("\n", "")
            assertOutputEqualsExpected(publicDataFile.readText(), expectedOutput)
            return result
        } catch (e: Exception) {
            throw ZKRunException("Failed to run circuit. Cause: $e\n")
        } finally {
            witnessFile.delete()
            publicDataFile.delete()
        }
    }

    private fun assertOutputEqualsExpected(circuitOutput: String, expectedOutput: String) {
        val actualJson = if (circuitOutput.isNotBlank()) Json.parseToJsonElement(circuitOutput) else null
        log.trace("Public Data (run): \n$actualJson")

        if (expectedOutput.isNotEmpty()) {
            val expectedJson = Json.parseToJsonElement(expectedOutput)

            if (actualJson != expectedJson) throw ZKRunException(
                "Public input does not match output from run. \n" +
                    "Expected: \n$expectedJson \n" +
                    "Actual: \n$actualJson"
            )
        }
    }

    override fun prove(witness: Witness): ByteArray {
        val witnessJson = Json.encodeToString(WitnessSerializer, witness)
        return prove(witnessJson)
    }

    fun prove(witnessJson: String): ByteArray {
        val witnessFile = createTempFile("zkp", null)
        witnessFile.writeText(witnessJson)

        val publicData = createTempFile("zkp", null)

        try {
            return completeZincCommand(
                "$PROVE --circuit $compiledCircuitPath --proving-key ${zkSetup.provingKeyPath} " +
                    "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
                provingTimeout
            )
                .toByteArray()
        } catch (e: IllegalStateException) {
            throw ZKProvingException("Could not create proof. Cause: $e\n")
        } finally {
            log.trace("Public Data (prove): \n${publicData.readText()}")
            publicData.delete()
        }
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        val publicInputJson = Json.encodeToString(PublicInputSerializer, publicInput)
        return verify(proof, publicInputJson)
    }

    fun verify(proof: ByteArray, publicInputJson: String) {
        val proofFile = createTempFile("zkp", null)
        proofFile.writeBytes(proof)

        val publicDataFile = createTempFile("zkp", null)
        publicDataFile.writeText(publicInputJson)

        try {
            completeZincCommand(
                "$VERIFY --circuit $compiledCircuitPath --verifying-key ${zkSetup.verifyingKeyPath} --public-data ${publicDataFile.absolutePath}",
                verificationTimeout, proofFile
            )
        } catch (e: Exception) {
            throw ZKVerificationException(
                "Could not verify proof.\nCause: $e"
            )
        } finally {
            proofFile.delete()
            publicDataFile.delete()
        }
    }
}
