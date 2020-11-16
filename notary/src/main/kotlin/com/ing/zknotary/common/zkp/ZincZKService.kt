package com.ing.zknotary.common.zkp

import net.corda.core.serialization.SingletonSerializeAsToken
import java.io.File
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
) : ZKService, SingletonSerializeAsToken() {
    private val circuitManifestPath = "$circuitFolder/Zargo.toml"
    private val defaultBuildPath = "$circuitFolder/build"
    private val defaultDataPath = "$circuitFolder/data"

    val compiledCircuitPath = "$artifactFolder/compiled-circuit.znb"
    val zkSetup = ZKSetup(
        provingKeyPath = "$artifactFolder/proving_key",
        verifyingKeyPath = "$artifactFolder/verifying_key.txt"
    )

    companion object {
        const val BUILD = "zargo build"
        const val SETUP = "zargo setup"
        const val PROVE = "zargo prove"
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
                error("$command failed with the following error output: $stdout")
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
    }

    fun setup() {
        val circuitManifest = File(circuitManifestPath)
        require(circuitManifest.exists()) { "Cannot find circuit manifest at $circuitManifestPath" }

        val witnessFile = createTempFile()
        val publicData = createTempFile()

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
            // Zinc creates files in the default locations independently if it was specified the exact locations,
            // clear the defaults too.
            File(defaultBuildPath).deleteRecursively()
            File(defaultDataPath).deleteRecursively()
        }

        completeZincCommand(
            "$SETUP --circuit $compiledCircuitPath " +
                "--proving-key ${zkSetup.provingKeyPath} --verifying-key ${zkSetup.verifyingKeyPath}",
            setupTimeout
        )
    }

    override fun prove(witness: ByteArray): ByteArray {
        require(File(compiledCircuitPath).exists()) { "Compile circuit not found in path $compiledCircuitPath." }
        require(File(zkSetup.provingKeyPath).exists()) { "Proving key not found at ${zkSetup.provingKeyPath}." }

        val witnessFile = createTempFile()
        witnessFile.writeBytes(witness)

        val publicData = createTempFile()

        try {
            return completeZincCommand(
                "$PROVE --circuit $compiledCircuitPath --proving-key ${zkSetup.provingKeyPath} " +
                    "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
                provingTimeout
            ).toByteArray()
        } catch (e: Exception) {
            throw ZKProvingException("Could not create proof. Cause: $e\n\nProvided witness: \n${String(witness)}")
        } finally {
            publicData.delete()
        }
    }

    override fun verify(proof: ByteArray, publicInput: ByteArray) {
        require(File(compiledCircuitPath).exists()) { "Compile circuit not found in path $compiledCircuitPath." }
        require(File(zkSetup.provingKeyPath).exists()) { "Proving key not found at ${zkSetup.provingKeyPath}." }

        val proofFile = createTempFile()
        proofFile.writeBytes(proof)

        val publicDataFile = createTempFile()
        publicDataFile.writeBytes(publicInput)

        try {
            completeZincCommand(
                "$VERIFY --circuit $compiledCircuitPath --verifying-key ${zkSetup.verifyingKeyPath} --public-data ${publicDataFile.absolutePath}",
                verificationTimeout, proofFile
            )
        } catch (e: Exception) {
            throw ZKVerificationException(
                "Could not verify proof. \nProof: ${String(proof)} \nPublic data: ${String(publicInput)}. \nCause: $e"
            )
        } finally {
            proofFile.delete()
            publicDataFile.delete()
        }
    }
}
