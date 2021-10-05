package com.ing.zknotary.common.zkp.metadata

import com.ing.zknotary.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractAttachment
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.transactions.LedgerTransaction
import java.io.File
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("TooManyFunctions") // TODO: Fix this once we agree on design
data class ResolvedZKTransactionMetadata(
    val network: ZKNetwork,
    val commands: List<ResolvedZKCommandMetadata>,
    val numberOfCorDappsForContracts: Int?
) {
    companion object {
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/transactions/"

        const val ERROR_NO_COMMANDS = "There should be at least one commmand in a ZKFlow transaction"
        const val ERROR_FIRST_COMMAND_NOT_TX_METADATA =
            "The first command in a ZKFLow transaction should always be a `ZKTransactionMetadataCommandData`"
        const val ERROR_FIRST_COMMAND_NOT_PRIVATE = "The first command in a ZKFLow transaction should always be private"
        const val ERROR_COMMAND_NOT_UNIQUE = "Multiple commands of one type found. All commands in a ZKFLow transaction should be unique"
        const val ERROR_PARTICIPANT_SIG_SCHEMES_DO_NOT_MATCH = "The participant signature scheme of all commands should match the network"
        const val ERROR_NOTARY_SIG_SCHEME_DOES_NOT_MATCH = "The notary signature scheme of all commands should match the network"
        const val ERROR_ATTACHMENT_CONSTRAINT_DOES_NOT_MATCH = "The attachment constraint of all commands should match the network"
    }

    init {
        require(commands.isNotEmpty()) { ERROR_NO_COMMANDS }
        require(commands.first().commandKClass.isSubclassOf(ZKTransactionMetadataCommandData::class)) { ERROR_FIRST_COMMAND_NOT_TX_METADATA }
        require(commands.first() is PrivateResolvedZKCommandMetadata) { ERROR_FIRST_COMMAND_NOT_PRIVATE }
        require(commands.distinctBy { it.commandKClass }.size == commands.size) { ERROR_COMMAND_NOT_UNIQUE }
        verifyCommandsMatchNetwork()
    }

    private fun verifyCommandsMatchNetwork() {
        commands.forEach {
            require(it.participantSignatureScheme == network.participantSignatureScheme) {
                ERROR_PARTICIPANT_SIG_SCHEMES_DO_NOT_MATCH + ". Expected '${network.participantSignatureScheme.schemeCodeName}', " +
                    "but command ${it.commandSimpleName} participant signature scheme is ${it.participantSignatureScheme.schemeCodeName}"
            }
            require(it.notarySignatureScheme == network.notary.signatureScheme) {
                ERROR_NOTARY_SIG_SCHEME_DOES_NOT_MATCH + ". Expected '${network.notary.signatureScheme.schemeCodeName}', " +
                    "but command ${it.commandSimpleName} notary signature scheme is ${it.notarySignatureScheme.schemeCodeName}"
            }
            require(it.attachmentConstraintType == network.attachmentConstraintType) {
                ERROR_ATTACHMENT_CONSTRAINT_DOES_NOT_MATCH + ". Expected '${network.attachmentConstraintType}', " +
                    "but command ${it.commandSimpleName} attachment constraint is ${it.attachmentConstraintType}"
            }
        }
    }

    private val privateCommands by lazy { commands.filterIsInstance<PrivateResolvedZKCommandMetadata>() }

    /**
     * The total number of signers of all commands added up.
     *
     * In theory, they may overlap (be the same PublicKeys), but we can't determine that easily.
     * Possible future optimization.
     */
    val numberOfSigners: Int by lazy { commands.sumOf { it.numberOfSigners } }

    /**
     * Best guess based on contract class names found in this transaction.
     *
     * There are many problems with this, so will probably cause errors on verification.
     * In that case, please set the number explicitly through `ZKTransactionMetadata.numberOfCorDappsForContracts`
     *
     * Assumption:
     * - If a contract has a different package, they are in a different CorDapp.
     */
    private fun guessNumberOfCordappsForContracts(): Int =
        commands.flatMap { it.contractClassNames }
            .map { it.packageName }.distinct().size

    // /**
    //  * The number of CorDapps required for the smart contract required for this transaction.
    //  * This number is determined by the location of all contracts required by this transaction.
    //  * They might be located in one CorDapp, or perhaps in multiple CorDapps.
    //  *
    //  * The difference with `val numberOfCorDappsForContracts` above is that this is calculated dynamically and therefore requires
    //  * the servicehub. They should return the same number
    //  */
    // fun numberOfCorDappsForContracts(services: ServicesForResolution): Int =
    //     commands.flatMap { it.contractClassNames }.distinct()
    //         .mapNotNull { services.cordappProvider.getContractAttachmentID(it) }
    //         .distinct().size

    /**
     * The aggregate list of java class to zinc type for all commands in this transaction.
     */
    val javaClass2ZincType: Map<KClass<*>, ZincType> by lazy {
        commands.fold(mapOf<KClass<*>, ZincType>()) { acc, resolvedZKCommandMetadata ->
            if (resolvedZKCommandMetadata is PrivateResolvedZKCommandMetadata) {
                acc + resolvedZKCommandMetadata.circuit.javaClass2ZincType
            } else acc
        }
    }

    /**
     * The target build folder for this transaction's main.zn.
     * The circuit parts for each command will be copied to this folder under `commands/<command.name>`.
     */
    // TODO: this is a hack for backwards compatibility. This ensure the circuit defined for single commands still works.
    // Unwanted side effect is that circuit parts for other commands in the tx are ignored.
    // Once we decide how and where command and transaction level circuit parts will be placed, we can redefine this.
    // val buildFolder: File = File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + commands.first().commandSimpleName)
    val buildFolder: File = privateCommands.first().circuit.buildFolder

    /**
     * Timeouts are the summed from all commands in this transaction.
     * TODO: is this the best way?
     */
    val buildTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.buildTimeout.seconds })
    val setupTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.setupTimeout.seconds })
    val provingTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.provingTimeout.seconds })
    val verificationTimeout: Duration = Duration.ofSeconds(privateCommands.sumOf { it.circuit.verificationTimeout.seconds })

    /**
     * Verify that the LedgerTransaction matches the expected structure defined in this metadata
     */
    fun verify(ltx: LedgerTransaction) {
        try {
            verifyCommandsAndSigners(ltx.commands.map { Command(it.value, it.signers) })
            verifyOutputs(ltx.outputs)
            verifyInputs(ltx.inputs)
            verifyReferences(ltx.references)
            verifyAttachments(ltx)
            // verifyNotary(ltx.notary)
            // verifyParameters(ltx)
        } catch (e: IllegalArgumentException) {
            throw IllegalTransactionStructureException(e)
        }
    }

    /**
     * Verify that the ZKTransactionBuilder matches the expected structure defined in this metadata
     * TODO: See if we can make sure this is always called, perhaps by calling it just before calling contract.verify
     * Alternatively, we can force users to extend ZKContract, which will do this for them and then delegate to normal verify function
     */
    fun verify(txb: ZKTransactionBuilder) {
        try {
            verifyCommandsAndSigners(txb.commands())
            verifyOutputs(txb.outputStates())
            verifyInputs(txb.inputsWithTransactionState)
            verifyReferences(txb.referencesWithTransactionState)
            verifyUserAttachments(txb) // Txb determines contract attachments only in `toWireTransaction()`, so not checking here.
            // verifyNotary(txb)
            // verifyParameters(txb)
        } catch (e: IllegalArgumentException) {
            throw IllegalTransactionStructureException(e)
        }
    }

    class IllegalTransactionStructureException(cause: Throwable) :
        IllegalArgumentException("Transaction does not match expected structure: ${cause.message}", cause)

    private val expectedUserAttachmentCount by lazy { commands.sumOf { it.numberOfUserAttachments } }

    /**
     * In the transaction builder, contract attachments are not yet determined and added.
     * This happens during toWireTransaction, so only verifying user-added attachments here.
     * The full list of attachments will be validated with the LedgerTransaction.
     */
    private fun verifyUserAttachments(txb: ZKTransactionBuilder) {
        verifyUserAttachmentCount(txb.attachments().size)
    }

    private fun verifyUserAttachmentCount(userAttachmentCount: Int) {
        require(userAttachmentCount == expectedUserAttachmentCount) {
            "Expected $expectedUserAttachmentCount user attachments. Found $userAttachmentCount"
        }
    }

    /**
     * We only verify the correct number of user attachments.
     *
     * Without servicehub, it is impossible to correctly calculate the expected contract attachments.
     * This is because contracts might be part of one or multiple CorDapps. There is no way of knowing this without
     * accessing the cordapp provider in the servicehub.
     */
    private fun verifyAttachments(ltx: LedgerTransaction) {
        val (actualContractAttachments, actualUserAttachments) = ltx.attachments.partition { it is ContractAttachment }

        val expectedNumberOfContractAttachments = numberOfCorDappsForContracts ?: guessNumberOfCordappsForContracts()

        require(actualContractAttachments.size == expectedNumberOfContractAttachments) {
            val actualAttachmentsToContracts = actualContractAttachments.map { attachment ->
                attachment as ContractAttachment
                "attachment id ${attachment.id.toHexString()} for contracts ${attachment.allContracts.joinToString(", ")}"
            }.joinToString("\n")

            "Expected $expectedNumberOfContractAttachments contract attachments, " +
                "found ${actualContractAttachments.size}: $actualAttachmentsToContracts. " +
                "If the number of contract attachments found is correct, please set or adjust `ZKTransactionMetadata.numberOfCorDappsForContracts` to match the attachments found. " +
                "If the number of contract attachments found is incorrect, please make sure the transaction matches the expected value."
        }

        verifyUserAttachmentCount(actualUserAttachments.size)
    }

    private fun verifyInputs(inputs: List<StateAndRef<ContractState>>) = matchTypes(
        componentName = "input",
        expectedTypes = commands.flatMap { it.inputs }.flattened,
        actualTypes = inputs.map { it.state.data::class }
    )

    @JvmName("verifyReferenceTransactionStates")
    private fun verifyReferences(references: List<TransactionState<ContractState>>) = matchTypes(
        componentName = "reference",
        expectedTypes = commands.flatMap { it.references }.flattened,
        actualTypes = references.map { it.data::class }
    )

    private fun verifyReferences(references: List<StateAndRef<ContractState>>) = verifyReferences(references.map { it.state })

    private fun verifyOutputs(outputs: List<TransactionState<*>>) = matchTypes(
        "output",
        expectedTypes = commands.flatMap { it.outputs }.flattened,
        actualTypes = outputs.map { it.data::class }
    )

    private fun verifyCommandsAndSigners(unverifiedCommands: List<Command<*>>) {
        commands.forEachIndexed { index, expectedCommandMetadata ->
            val actualCommand = unverifiedCommands.getOrElse(index) { error("Expected to find a command at index $index, nothing found") }

            require(actualCommand.value::class == expectedCommandMetadata.commandKClass) {
                "Expected command at index $index to be '${expectedCommandMetadata.commandKClass}', but found '${actualCommand.value::class}'"
            }

            require(actualCommand.signers.size == expectedCommandMetadata.numberOfSigners) {
                "Expected '${expectedCommandMetadata.numberOfSigners} signers for command $actualCommand, but found '${actualCommand.signers.size}'."
            }

            actualCommand.signers.forEachIndexed { signerIndex, key ->
                val actualScheme = Crypto.findSignatureScheme(key)
                require(actualScheme == expectedCommandMetadata.participantSignatureScheme) {
                    "Signer $signerIndex of command '${actualCommand.value::class}' should use signature scheme: '${expectedCommandMetadata.participantSignatureScheme.schemeCodeName}, but found '${actualScheme.schemeCodeName}'"
                }
            }
        }
    }

    private fun matchTypes(componentName: String, expectedTypes: List<KClass<*>>, actualTypes: List<KClass<*>>) {
        expectedTypes.forEachIndexed { index, expected ->
            val actual = actualTypes.getOrElse(index) { error("Expected to find $componentName $expected at index $index, nothing found") }
            require(actual == expected) { "Unexpected $componentName order. Expected '$expected' at index $index, but found '$actual'" }
        }
    }
}
