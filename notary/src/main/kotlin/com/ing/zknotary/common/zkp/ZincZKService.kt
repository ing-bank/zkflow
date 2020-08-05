package com.ing.zknotary.common.zkp

import net.corda.core.serialization.SingletonSerializeAsToken
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

class ZincZKService(
    private val circuitSrcPath: String,
    artifactFolder: String,
    private val buildTimeout: Duration,
    private val setupTimeout: Duration,
    private val provingTimeout: Duration,
    private val verificationTimeout: Duration
) : ZKService, SingletonSerializeAsToken() {
    val compiledCircuitPath = "$artifactFolder/compiled-${File(circuitSrcPath).nameWithoutExtension}.znb"
    val zkSetup = ZKSetup(
        provingKeyPath = "$artifactFolder/proving_key",
        verifyingKeyPath = "$artifactFolder/verifying_key.txt"
    )

    companion object {
        const val COMPILE = "znc"
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
            } catch (e: Exception) {
                error(e.localizedMessage)
            }
        }
    }

    data class ZKSetup(val provingKeyPath: String, val verifyingKeyPath: String)

    fun setup() {
        val circuitSrc = File(circuitSrcPath)
        require(circuitSrc.exists()) { "Cannot find circuit at $circuitSrcPath" }
        require(circuitSrc.name == "main.zn") { "The circuit filename must be 'main.zn', found $circuitSrcPath." }

        val witnessFile = createTempFile()
        val publicData = createTempFile()

        try {
            completeZincCommand(
                "$COMPILE $circuitSrcPath --output $compiledCircuitPath " +
                    "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
                buildTimeout
            )
        } finally {
            // Neither witness, nor Public data carry useful information after build, they are just templates
            publicData.delete()
            witnessFile.delete()
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
            throw ZKProvingException("Could not create proof. Cause: $e")
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
