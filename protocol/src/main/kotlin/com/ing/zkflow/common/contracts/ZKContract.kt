package com.ing.zkflow.common.contracts

import com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService
import com.ing.zkflow.common.transactions.zkTransactionMetadataOrNull
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.ZKIndexedTypedElement
import net.corda.core.KeepForDJVM
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Please note that even though [ZKContract] extends [Contract], Corda still requires users to implement both interfaces directly.
 * If that is not done, the contract attachments will not be correctly resolved when building WireTransactions.
 * See also this check in Corda's AttachmentWithContext class:
 *
 * ```
 * require(contract in contractAttachment.allContracts) {
 *     "This AttachmentWithContext was not initialised properly. " +
 *     "Please ensure all Corda contracts extending existing Corda contracts also implement the Contract base class."
 * }
 * ```
 */
@KeepForDJVM
@CordaSerializable
interface ZKContract : Contract {
    /**
     * Checks on the publicly visible components of a transaction.
     *
     * 'Public only' components are components that are not mentioned in any of the metadata of any command in this transaction.
     * That means they will not be checked by the ZKP circuits of this transaction.
     * 'Public Only' should therefore either not exist or have a check rule in this public verification function.
     * By default, ZKFlow enforces zero public only inputs and outputs for any ZKContract. If this is not acceptable,
     * please override this function and write appropriate checks.
     */
    override fun verify(tx: LedgerTransaction) {
        val filteredLedgerTransaction = tx.ensureFilteredLedgerTransaction()
        val metadata = filteredLedgerTransaction.zkTransactionMetadataOrNull()
        /*
         * By default, there should be no 'public only' outputs belonging to this contract in this transaction.
         * They should all be mentioned in the metadata, and therefore checked by the ZKP contract logic.
         * If they are not mentioned there, they should either not exist, or be checked here (in that case, override this function).
         */
        val publicOutputs = filteredLedgerTransaction.outputStates.publicOnlyStatesBelongingToZKContract(this::class, metadata?.outputs)
        require(publicOutputs.isEmpty()) { "There should be no additional 'public only' outputs in the transaction for contract ${this::class}, found $publicOutputs" }

        /*
         * By default, there should be no 'public only' inputs belonging to this contract in this transaction.
         * They should all be mentioned in the metadata, and therefore checked by the ZKP contract logic.
         * If they are not mentioned there, they should either not exist, or be checked here (in that case, override this function).
         */
        val publicInputs = filteredLedgerTransaction.inputStates.publicOnlyStatesBelongingToZKContract(this::class, metadata?.inputs)
        require(publicInputs.isEmpty()) { "There should be no additional 'public only' inputs in the transaction for contract ${this::class}, found $publicInputs" }
    }
}

/**
 * Ensures a LedgerTransaction than only contains components that are public ONLY.
 * That is, they are not mentioned in any of the metadata of any command in this transaction.
 * 'Public Only' components are not checked by the ZKP circuit and therefore should either not exist,
 * or have a check rule in the public verification function.
 */
private fun LedgerTransaction.ensureFilteredLedgerTransaction(): LedgerTransaction =
    if (inFilteredContext()) this else filter(zkTransactionMetadataOrNull())

// Filters out components that should not be publicly visible according to the metadata.
fun LedgerTransaction.filter(metadata: ResolvedZKTransactionMetadata?): LedgerTransaction {
    if (metadata == null) return this

    fun <T> List<T>.filterComponents(metadata: ResolvedZKTransactionMetadata, group: ComponentGroupEnum) =
        mapIndexedNotNull { index, component ->
            if (metadata.shouldBeVisibleInFilteredComponentGroup(group.ordinal, index)) component else null
        }

    return LedgerTransaction.createForSandbox(
        inputs = inputs.filterComponents(metadata, ComponentGroupEnum.INPUTS_GROUP),
        references = references.filterComponents(metadata, ComponentGroupEnum.REFERENCES_GROUP),
        outputs = outputs.filterComponents(metadata, ComponentGroupEnum.OUTPUTS_GROUP),
        commands = commands.filterComponents(metadata, ComponentGroupEnum.COMMANDS_GROUP),
        attachments = attachments.filterComponents(metadata, ComponentGroupEnum.ATTACHMENTS_GROUP),
        timeWindow = listOf(timeWindow).filterComponents(metadata, ComponentGroupEnum.TIMEWINDOW_GROUP).firstOrNull(),
        notary = listOf(notary).filterComponents(metadata, ComponentGroupEnum.NOTARY_GROUP).firstOrNull(),

        id = id,
        privacySalt = privacySalt,
        digestService = digestService,

        /*
         * According to LedgerTransaction, This is nullable only for backwards compatibility for serialized transactions.
         * In reality this field will always be set when on the normal codepaths.
         * Additionally, ZKVerifierTransaction.zkToFilteredLedgerTransaction does not filter this, so we simply assign
         */
        networkParameters = networkParameters!!

    )
}

/**
 * If contract verification is called from ZKTransactionVerifierService, we know the ltx was created from a ZKVerifierTransaction
 * (which is itself filtered by nature) and is therefore already filtered.
 */
private fun inFilteredContext(): Boolean = Exception().stackTrace.any { it.className == ZKTransactionVerifierService::class.qualifiedName }

private fun <T : ContractState> List<T>.publicOnlyStatesBelongingToZKContract(
    contractClass: KClass<out ZKContract>,
    metadata: MutableList<out ZKIndexedTypedElement>?
): List<T> {
    // First we filter the 'public only' components
    return if (metadata == null) {
        this
    } else {
        val publicMetadataForThisContract =
            metadata.sortedBy { it.index }.filter { it.type.contractClass == contractClass && it.isPubliclyVisible() }
        val publicStatesForThisContract = this.filter { it.contractClass.isSubclassOf(contractClass) }
        publicStatesForThisContract.drop(publicMetadataForThisContract.size)
    }
}

private val KClass<out ContractState>.contractClass: KClass<out Contract>
    get() {
        val annotation = java.getAnnotation(BelongsToContract::class.java)
        if (annotation != null) {
            return annotation.value
        }
        val enclosingClass = java.enclosingClass
        @Suppress("UNCHECKED_CAST")
        if (Contract::class.java.isAssignableFrom(enclosingClass)) return enclosingClass.kotlin as KClass<out Contract>

        error("Could not determine contract class for $this. Either annotate it with @BelongsToContract or nest it inside a Contract class.")
    }

private val ContractState.contractClass: KClass<out Contract>
    get() = this::class.contractClass
