package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import com.ing.zknotary.common.zkp.ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
import com.ing.zknotary.common.zkp.ZKFlow.requireSupportedSignatureScheme
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.internal.lazyMapped
import net.corda.core.internal.objectOrNewInstance
import java.io.File
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object ZKFlow {
    val DEFAULT_ZKFLOW_SIGNATURE_SCHEME = Crypto.EDDSA_ED25519_SHA512

    fun requireSupportedSignatureScheme(scheme: SignatureScheme) {
        /**
         * Initially, we only support Crypto.EDDSA_ED25519_SHA512.
         * Later, we may support all scheme supported by Corda: `require(participantSignatureScheme in supportedSignatureSchemes())`
         */
        require(scheme == Crypto.EDDSA_ED25519_SHA512) {
            "Unsupported signature scheme: ${scheme.schemeCodeName}. Only ${Crypto.EDDSA_ED25519_SHA512.schemeCodeName} is supported."
        }
    }
}

@DslMarker
annotation class ZKTransactionMetadataDSL

@ZKCommandMetadataDSL
data class ZKNotary(
    /**
     * The public key type used by the notary in this network.
     */
    var signatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
) {
    init {
        requireSupportedSignatureScheme(signatureScheme)
    }
}

@ZKTransactionMetadataDSL
data class ZKNetwork(
    var participantSignatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
    var attachmentConstraintType: KClass<out AttachmentConstraint> = SignatureAttachmentConstraint::class
) {
    var notary = ZKNotary()

    init {
        requireSupportedSignatureScheme(participantSignatureScheme)
    }

    fun notary(init: ZKNotary.() -> Unit) = notary.apply(init)
}

@ZKTransactionMetadataDSL
class ZKCommandList : ArrayList<KClass<out CommandData>>() {
    fun command(command: KClass<out CommandData>): Boolean {
        require(!contains(command)) { "The transaction already contains a '$command'. ZKFLow transactions can only contain one of each command" }

        return add(command)
    }

    operator fun KClass<out CommandData>.unaryPlus() = command(this)

    fun resolve(): List<ResolvedZKCommandMetadata> {
        return lazyMapped { kClass, _ ->
            val command = kClass.objectOrNewInstance() as ZKCommandData
            command.metadata.resolve()
        }
    }
}

@ZKTransactionMetadataDSL
class ZKTransactionMetadata {
    var network = ZKNetwork()
    var commands = ZKCommandList()

    fun network(init: ZKNetwork.() -> Unit): ZKNetwork {
        return network.apply(init)
    }

    fun commands(init: ZKCommandList.() -> Unit): ZKCommandList {
        return commands.apply(init)
    }

    val resolved: ResolvedZKTransactionMetadata by lazy {
        ResolvedZKTransactionMetadata(
            network,
            commands.resolve()
        )
    }
}

data class ResolvedZKTransactionMetadata(
    val network: ZKNetwork,
    val commands: List<ResolvedZKCommandMetadata>
) {
    companion object {
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/transactions/"
    }

    init {
        require(commands.first().commandKClass.isSubclassOf(ZKTransactionMetadataCommandData::class)) { "The first command in a ZKFLow transaction should always be a `ZKTransactionMetadataCommandData`." }
        require(commands.first() is PrivateResolvedZKCommandMetadata) { "The first command in a ZKFLow transaction should always be private." }
    }

    val privateCommands = commands.filterIsInstance<PrivateResolvedZKCommandMetadata>()

    /**
     * The total number of signers of all commands added up.
     *
     * In theory, they may overlap (be the same PublicKeys), but we can't determine that easily.
     * Possible future optimization.
     */
    val numberOfSigners: Int = commands.sumOf { it.numberOfSigners }

    /**
     * All output types for all commands merged.
     *
     * The order in which the commands are mentioned in the ZKTransactionMetadata determines
     * the order they appear here. This means it is expected that transaction built for this metadata
     * respect this order. This will be validate in [ZKTransactionBuilder.toWireTransaction]
     */
    private val outputTypes = commands.flatMap { it.outputs }

    private val outputTypesFlattened: List<KClass<out ContractState>> = outputTypes.fold(listOf()) { acc, typeCount ->
        acc + List(typeCount.count) { typeCount.type }
    }

    /**
     * The total number of outputs in this transaction
     */
    private val outputCount = outputTypesFlattened.size

    /**
     * The aggregate list of java class to zinc type for all commands in this transaction.
     */
    val javaClass2ZincType: Map<KClass<*>, ZincType> = commands.fold(mapOf<KClass<*>, ZincType>()) { acc, resolvedZKCommandMetadata ->
        if (resolvedZKCommandMetadata is PrivateResolvedZKCommandMetadata) {
            acc + resolvedZKCommandMetadata.circuit.javaClass2ZincType
        } else acc
    }

    /**
     * The target build folder for this transaction's main.zn.
     * The circuit parts for each command will be copied to this folder under `commands/<command.name>`.
     */
    val buildFolder: File = File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + commands.first().commandSimpleName)

    /**
     * Timeouts are the summed from all commands in this transaction.
     * TODO: is this the best way?
     */
    val buildTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.buildTimeout.seconds })
    val setupTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.setupTimeout.seconds })
    val provingTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.provingTimeout.seconds })
    val verificationTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.verificationTimeout.seconds })

    fun verify(txb: ZKTransactionBuilder) {
        try {
            verifyCommands(txb)
            verifyOutputs(txb)
        } catch (e: IllegalArgumentException) {
            throw IllegalTransactionStructureException(e)
        }
    }

    class IllegalTransactionStructureException(cause: Throwable) :
        IllegalArgumentException("Transaction does not match expected structure.", cause)

    private fun verifyOutputs(txb: ZKTransactionBuilder) {
        outputTypesFlattened.forEachIndexed { index, kClass ->
            val actualOutput = txb.outputStates().getOrElse(index) { error("Expected to find an output at index $index, nothing found") }
            require(actualOutput.data::class == kClass) {
                "Unexpected output order. Expected '$kClass' at index $index, but found '${actualOutput.data::class}'"
            }
        }
    }

    private fun verifyCommands(txb: ZKTransactionBuilder) {
        commands.forEachIndexed { index, expectedCommandMetadata ->
            val actualCommand = txb.commands().getOrElse(index) { error("Expected to find a command at index $index, nothing found") }

            require(actualCommand.value::class == expectedCommandMetadata.commandKClass) {
                "Expected command at index $index to be '${expectedCommandMetadata.commandKClass}', but found '${actualCommand.value::class}'"
            }

            require(actualCommand.signers.size == expectedCommandMetadata.numberOfSigners) {
                "Expected '${expectedCommandMetadata.numberOfSigners} signers, but found '${actualCommand.signers.size}'."
            }
        }
    }
}

fun transactionMetadata(init: ZKTransactionMetadata.() -> Unit): ZKTransactionMetadata {
    return ZKTransactionMetadata().apply(init)
}
