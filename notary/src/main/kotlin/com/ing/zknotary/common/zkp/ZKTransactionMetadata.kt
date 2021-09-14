package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
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
import kotlin.reflect.KClass

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

    fun resolved(): ResolvedZKTransactionMetadata {
        return ResolvedZKTransactionMetadata(
            network,
            commands.resolve()
        )
    }
}

data class ResolvedZKTransactionMetadata(
    val network: ZKNetwork,
    val commands: List<ResolvedZKCommandMetadata>
) {
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
        require(txb.outputStates().size == outputCount) { "Expected $outputCount outputs in transaction, found ${txb.outputStates().size}" }
        txb.outputStates().forEachIndexed { index, transactionState ->
            require(transactionState.data::class == outputTypesFlattened[index]) { "Unexpected output order. Expected '${outputTypesFlattened[index]}' at index $index, but found '${transactionState.data::class}'" }
        }
    }

    private fun verifyCommands(txb: ZKTransactionBuilder) {
        txb.commands().forEachIndexed { index, command ->
            val expectedCommandMetadata = commands[index]
            require(
                command.value::class == expectedCommandMetadata.commandKClass
            ) { "Command at index $index expect to be '${expectedCommandMetadata.commandKClass}', but found '${command.value::class}'" }

            require(command.signers.size == expectedCommandMetadata.numberOfSigners) { "Expected '${expectedCommandMetadata.numberOfSigners} signers, but found '${command.signers.size}'." }
        }
    }
}

fun transactionMetadata(init: ZKTransactionMetadata.() -> Unit): ZKTransactionMetadata {
    return ZKTransactionMetadata().apply(init)
}
