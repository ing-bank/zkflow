package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.Result
import net.corda.core.contracts.requireThat
import net.corda.core.serialization.SingletonSerializeAsToken
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

class ZincZKService(
    val compiledCircuit: String,
    //
    val zkSetup: ZKSetup,
    //
    private val provingTimeout: Duration,
    private val verificationTimeout: Duration
) : ZKService, SingletonSerializeAsToken() {
    companion object {
        const val Compile = "znc"
        const val Setup = "zargo setup"
        const val Prove = "zargo prove"
        const val Verify = "zargo verify"

        /** Returns Result of the command execution.
         *  In case of successful execution (empty error stream) Result contains text collected from input stream,
         *  in case of a failure, Result contains text collected from error stream.
         **/
        fun completeZincCommand(command: String, timeout: Duration, input: File? = null): Result<String, String> {
            val process = command.toProcess(input)
            val hasCompleted = process.waitFor(timeout.seconds, TimeUnit.SECONDS)
            if (!hasCompleted) {
                process.destroy()
                return Result.Failure("$command ran longer than ${timeout.seconds} seconds")
            }

            val errors = process.errorStream.bufferedReader().readText()
            return if (errors.isNotBlank()) {
                Result.Failure(errors)
            } else {
                Result.Success(process.inputStream.bufferedReader().readText())
            }
        }

        private fun String.toProcess(input: File? = null): Process {
            // println("Starting process: $this")
            return try {
                val builder = ProcessBuilder(split("\\s".toRegex()))
                if (input != null) {
                    builder.redirectInput(input)
                }
                builder.start()
            } catch (e: Exception) {
                e.printStackTrace()
                error(e.localizedMessage)
            }
        }
    }

    data class ZKSetup(val provingKeyPath: String? = null, val verifyingKeyPath: String? = null)

    init {
        requireThat {
            "Circuit file must exist" using File(compiledCircuit).exists()
            "Either proving or verifying key must be present" using (zkSetup.provingKeyPath != null || zkSetup.verifyingKeyPath != null)
        }
    }

    override fun prove(witness: ByteArray): Result<Proof, String> {
        val provingKeyPath = zkSetup.provingKeyPath ?: error("Proving key must be present")

        val witnessFile = createTempFile()
        witnessFile.writeBytes(witness)

        val publicData = createTempFile()

        val prove = completeZincCommand(
            "$Prove --circuit $compiledCircuit --proving-key $provingKeyPath " +
                "--public-data ${publicData.absolutePath} --witness ${witnessFile.absolutePath}",
            provingTimeout
        ).map {
            Proof(it.toByteArray(), publicData.readBytes())
        }

        publicData.delete()

        return prove
    }

    override fun verify(proof: Proof): Result<Unit, String> {
        val verifyingKeyPath = zkSetup.verifyingKeyPath ?: error("Verifying key must be present")

        val proofFile = createTempFile()
        proofFile.writeBytes(proof.value)

        val publicDataFile = createTempFile()
        publicDataFile.writeBytes(proof.publicData)

        val verify = completeZincCommand(
            "$Verify --circuit $compiledCircuit --verifying-key $verifyingKeyPath --public-data ${publicDataFile.absolutePath}",
            verificationTimeout, proofFile
        ).map { it.toLowerCase().contains("verified") }

        proofFile.delete()
        publicDataFile.delete()

        return when (verify) {
            is Result.Success ->
                if (verify.value) {
                    Result.Success(Unit)
                } else {
                    Result.Failure("Verification output does not contain \"Verified\" keyword")
                }
            is Result.Failure -> Result.Failure(verify.value)
        }
    }
}
