package com.ing.zkflow.common.contracts

import com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.KeepForDJVM
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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

        /*
         * By default, there should be no 'public only' outputs belonging to this contract in this transaction.
         * They should all be mentioned in the metadata, and therefore checked by the ZKP contract logic.
         * If they are not mentioned there, they should either not exist, or be checked here (in that case, override this function).
         */
        val publicOutputs = filteredLedgerTransaction.publicOnlyOutputsBelongingToZKContract(this::class)
        require(publicOutputs.isEmpty()) { "There should be no additional 'public only' outputs in the transaction for contract ${this::class}, found ${publicOutputs.size}" }

        /*
         * By default, there should be no 'public only' inputs belonging to this contract in this transaction.
         * They should all be mentioned in the metadata, and therefore checked by the ZKP contract logic.
         * If they are not mentioned there, they should either not exist, or be checked here (in that case, override this function).
         */
        val publicInputs = filteredLedgerTransaction.publicOnlyInputsBelongingToZKContract(this::class)
        require(publicInputs.isEmpty()) { "There should be no additional 'public only' inputs in the transaction for contract ${this::class}, found ${publicInputs.size}" }

        /*
         * By default, there should be no 'public only' inputs belonging to this contract in this transaction.
         * They should all be mentioned in the metadata, and therefore checked by the ZKP contract logic.
         * If they are not mentioned there, they should either not exist, or be checked here (in that case, override this function).
         */
        val publicReferences = filteredLedgerTransaction.publicOnlyReferencesBelongingToZKContract(this::class)
        require(publicReferences.isEmpty()) { "There should be no additional 'public only' inputs in the transaction for contract ${this::class}, found ${publicReferences.size}" }
    }
}

/**
 * Ensures a LedgerTransaction than only contains components that are public ONLY.
 * That is, they are not mentioned in any of the metadata of any command in this transaction.
 * 'Public Only' components are not checked by the ZKP circuit and therefore should either not exist,
 * or have a check rule in the public verification function.
 */
private fun LedgerTransaction.ensureFilteredLedgerTransaction(): LedgerTransaction {
    /**
     * Returns the outputs that are ONLY public, i.e. not mentioned in any metadata for this transaction.
     * Note that this should only be called on a non-filtered LedgerTransaction. For filtered LedgerTransactions the indexes of the components will be wrong.
     */
    fun LedgerTransaction.publicOnlyOutputs(metadata: ResolvedZKTransactionMetadata): List<TransactionState<ContractState>> =
        outputs.mapIndexedNotNull { index, transactionState -> if (metadata.outputs.any { it.index == index }) null else transactionState }

    /**
     * Returns the inputs that are ONLY public, i.e. not mentioned in any metadata for this transaction.
     * Note that this should only be called on a non-filtered LedgerTransaction. For filtered LedgerTransactions the indexes of the components will be wrong.
     */
    fun LedgerTransaction.publicOnlyInputs(metadata: ResolvedZKTransactionMetadata): List<StateAndRef<ContractState>> =
        inputs.mapIndexedNotNull { index, stateAndRef -> if (metadata.inputs.any { it.index == index }) null else stateAndRef }

    /**
     * Returns the references that are ONLY public, i.e. not mentioned in any metadata for this transaction.
     * Note that this should only be called on a non-filtered LedgerTransaction. For filtered LedgerTransactions the indexes of the components will be wrong.
     */
    fun LedgerTransaction.publicOnlyReferences(metadata: ResolvedZKTransactionMetadata): List<StateAndRef<ContractState>> =
        references.mapIndexedNotNull { index, stateAndRef -> if (metadata.references.any { it.index == index }) null else stateAndRef }

    return if (inFilteredContext()) {
        this
    } else {
        val metadata = zkTransactionMetadata()

        @Suppress("DEPRECATION")
        (
            LedgerTransaction.createForSandbox(
                // Any input that is mentioned in the metadata is safely coveredy by ZKP contract logic and can therefore be filtered out.
                inputs = publicOnlyInputs(metadata),
                // Any reference that is mentioned in the metadata is safely coveredy by ZKP contract logic and can therefore be filtered out.
                references = publicOnlyReferences(metadata),
                // Any output that is mentioned in the metadata is safely coveredy by ZKP contract logic and can therefore be filtered out.
                outputs = publicOnlyOutputs(metadata),
                commands = commands,
                attachments = attachments,
                id = id,
                timeWindow = timeWindow,
                notary = notary,
                privacySalt = privacySalt,
                digestService = digestService,

            /*
                 * According to LedgerTransaction, This is nullable only for backwards compatibility for serialized transactions.
                 * In reality this field will always be set when on the normal codepaths
                 */
                networkParameters = networkParameters!!
            )
            )
    }
}

/**
 * If contract verification is called from ZKTransactionVerifierService, we know the ltx was created from a ZKVerifierTransaction
 * and is therefore filtered.
 */
private fun inFilteredContext(): Boolean =
    Exception().stackTrace.any { it.className == ZKTransactionVerifierService::class.qualifiedName }

private fun LedgerTransaction.publicOnlyOutputsBelongingToZKContract(contractClass: KClass<out ZKContract>): List<TransactionState<ContractState>> {
    return outputs.filter { it.data.contractClass.isSubclassOf(contractClass) }
}

private fun LedgerTransaction.publicOnlyInputsBelongingToZKContract(contractClass: KClass<out ZKContract>): List<StateAndRef<ContractState>> {
    return inputs.filter { it.state.data.contractClass.isSubclassOf(contractClass) }
}

private fun LedgerTransaction.publicOnlyReferencesBelongingToZKContract(contractClass: KClass<out ZKContract>): List<StateAndRef<ContractState>> {
    return references.filter { it.state.data.contractClass.isSubclassOf(contractClass) }
}

private val ContractState.contractClass: KClass<out Contract>
    get() {
        val annotation = javaClass.getAnnotation(BelongsToContract::class.java)
        if (annotation != null) {
            return annotation.value
        }
        val enclosingClass = javaClass.enclosingClass
        @Suppress("UNCHECKED_CAST")
        if (Contract::class.java.isAssignableFrom(enclosingClass)) return enclosingClass.kotlin as KClass<out Contract>

        error("Could not determine contract class for $this. Either annotate it with @BelongsToContract or nest itinside a Contract class.")
    }
