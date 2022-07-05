package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.serialization.zinc.json.PublicInputSerializer
import com.ing.zkflow.common.serialization.zinc.json.WitnessSerializer
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.serialization.json.Json
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.File.createTempFile
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@Suppress("TooManyFunctions")
@SuppressFBWarnings(
    "PATH_TRAVERSAL_IN",
    "COMMAND_INJECTION",
    justification = "Paths can only come from command classes. Zinc commands are hardcoded, processing fun is private"
)
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
        private fun completeZincCommand(command: String, timeout: Duration, input: File? = null): String {
            log.debug("Running command: '$command'")
            val process = command.toProcess(input)
            val output = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val hasCompleted = process.waitFor(timeout.seconds, TimeUnit.SECONDS)

            if (!hasCompleted) {
                process.destroy()
                error("'$command' ran longer than ${timeout.seconds} seconds")
            }

            return if (process.exitValue() != 0) {
                error("'$command' failed with the following output: $stderr")
            } else {
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
        log.debug("Witness size: ${witness.size()}, of which padding bytes: ${witness.size { it == 0.toByte() }}") // Assumes BFL zero-byte padding

        val witnessJson = WitnessSerializer.fromWitness(witness)
        log.trace("Witness JSON: $witnessJson")

        val publicInputJson = PublicInputSerializer.fromPublicInput(publicInput)
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
            val assertionErrorIdentifier = "[ERROR   zvm] runtime error: assertion error: "
            val jsonErrorIdentifier = "[ERROR   zvm] invalid json structure: "
            if (e.message?.contains(assertionErrorIdentifier) == true) {
                val assertionError = e.message?.substringAfter(assertionErrorIdentifier)?.substringBefore("[ERROR zargo]")
                throw ZKRunException("ZKP assertion failed: $assertionError\n")
            } else if (e.message?.contains(jsonErrorIdentifier) == true) {
                val jsonError = e.message?.substringAfter(jsonErrorIdentifier)?.substringBefore("at witness")
                throw ZKRunException("Invalid witness contents: $jsonError\n")
            } else {
                throw ZKRunException("Failed to run circuit. Cause: $e\n")
            }
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

    /**
     * this is used to get the logger for the caller of the caller.
     */
    private val loggerForMyCaller: Logger
        get() = LoggerFactory.getLogger(Throwable().stackTrace[2].className)

    fun setupTimed(log: Logger = loggerForMyCaller) {
        val time = measureTime {
            this.setup()
        }
        log.debug("[setup] $time")
    }

    fun proveTimed(witnessJson: String, log: Logger = loggerForMyCaller): ByteArray {
        val timedValue = measureTimedValue {
            this.prove(witnessJson)
        }
        log.debug("[prove] ${timedValue.duration}")
        return timedValue.value
    }

    fun verifyTimed(proof: ByteArray, publicInputJson: String, log: Logger = loggerForMyCaller) {
        val time = measureTime {
            this.verify(proof, publicInputJson)
        }
        log.debug("[verify] $time")
    }

    override fun prove(witness: Witness): ByteArray {
        log.info("Proving")
        log.debug("Witness size: ${witness.size()}, of which padding bytes: ${witness.size { it == 0.toByte() }}") // Assumes BFL zero-byte padding

        val witnessJson = WitnessSerializer.fromWitness(witness)
        log.trace("Witness JSON: $witnessJson")

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
            witnessFile.delete()
        }
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        log.info("Verifying proof")
        val publicInputJson = PublicInputSerializer.fromPublicInput(publicInput)
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
