package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serialization.PublicInputSerializer
import com.ing.zknotary.common.serialization.WitnessSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.io.File.createTempFile
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExperimentalSerializationApi
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

        /**
         * Returns output of the command execution.
         **/
        fun completeZincCommand(command: String, timeout: Duration, input: File? = null): String {
            val process = command.toProcess(input)
            val hasCompleted = process.waitFor(timeout.seconds, TimeUnit.SECONDS)

            if (!hasCompleted) {
                process.destroy()
                error("$command ran longer than ${timeout.seconds} seconds")
            }

            return if (process.exitValue() != 0) {
                val stdout = process.errorStream.bufferedReader().readText()
                error("$command failed with the following com.ing.zknotary.generator.error output: $stdout")
            } else {
                process.inputStream.bufferedReader().readText()
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

    @ExperimentalSerializationApi
    fun run(witness: Witness, publicInput: PublicInput): String {
        val witnessJson = Json.encodeToString(WitnessSerializer, witness).toByteArray()
        val publicInputJson = Json.encodeToString(PublicInputSerializer, publicInput).toByteArray()
        return run(witnessJson, publicInputJson)
    }

    fun run(witness: ByteArray, publicInput: ByteArray): String {
        val witnessFile = createTempFile("zkp", null)
        witnessFile.writeBytes(witness)

        val publicDataFile = createTempFile("zkp", null)
        publicDataFile.writeBytes(publicInput)

        try {
            return completeZincCommand(
                "$RUN --circuit $compiledCircuitPath --manifest-path $circuitManifestPath " +
                    "--public-data ${publicDataFile.absolutePath} --witness ${witnessFile.absolutePath}",
                provingTimeout
            ).replace(" ", "")
                .replace("\n", "")
        } catch (e: Exception) {
            throw ZKRunException("Failed to run circuit. Cause: $e\n")
        } finally {
            witnessFile.delete()
            publicDataFile.delete()
        }
    }

    override fun prove(witness: Witness): ByteArray {
        val witnessJson = Json.encodeToString(WitnessSerializer, witness).toByteArray()
        return prove(witnessJson)
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        val publicInputJson = Json.encodeToString(PublicInputSerializer, publicInput).toByteArray()
        return verify(proof, publicInputJson)
    }

    fun prove(witness: ByteArray): ByteArray {

        val witnessFile = createTempFile("zkp", null)
        witnessFile.writeBytes(witness)

        val publicData = createTempFile("zkp", null)

        try {
            return completeZincCommand(
                "$PROVE --circuit $compiledCircuitPath --proving-key ${zkSetup.provingKeyPath} " +
                    "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
                provingTimeout
            ).toByteArray()
        } catch (e: Exception) {
            throw ZKProvingException("Could not create proof. Cause: $e\n")
        } finally {
            publicData.delete()
        }
    }

    fun verify(proof: ByteArray, publicInput: ByteArray) {

        val proofFile = createTempFile("zkp", null)
        proofFile.writeBytes(proof)

        val publicDataFile = createTempFile("zkp", null)
        publicDataFile.writeBytes(publicInput)

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
