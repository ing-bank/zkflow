package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.states.EMPTY_STATEREF
import com.ing.zknotary.common.states.toZKCommand
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction

fun WireTransaction.toZKProverTransaction(
    services: ServiceHub,
    txIdMap: Map<SecureHash, SecureHash>, // Temp hack, this should be a CordaService on the ServiceHub later.
    componentGroupLeafDigestService: DigestService,
    nodeDigestService: DigestService = componentGroupLeafDigestService,
    componentPaddingConfiguration: ComponentPaddingConfiguration = ZKProverTransactionFactory.DEFAULT_PADDING_CONFIGURATION
): ZKProverTransaction {
    requireThat {
        // "A notary must always be set on a ZKProverTransaction" using (notary != null)
        "There must be exactly one command on a ZKProverTransaction" using (commands.size == 1)
    }

    val ltx = toLedgerTransaction(services)

    fun List<StateAndRef<*>>.mapToZkid(txIdMap: Map<SecureHash, SecureHash>): List<StateAndRef<*>> {
        return map {
            val zkid = checkNotNull(txIdMap[it.ref.txhash]) {
                "Unexpectedly could not find the tx id map for ${it.ref.txhash}. Did you run ResolveTransactionsFlow before?"
            }
            StateAndRef(it.state, StateRef(zkid, it.ref.index))
        }
    }

    return ZKProverTransaction(
        inputs = ltx.inputs.mapToZkid(txIdMap),
        outputs = ltx.outputs.map { TransactionState(data = it.data as ZKContractState, notary = it.notary) },
        references = ltx.references.mapToZkid(txIdMap),
        command = ltx.commands.map { it.toZKCommand() }.single(),
        notary = ltx.notary!!,
        timeWindow = ltx.timeWindow,
        privacySalt = ltx.privacySalt,
        networkParametersHash = ltx.networkParameters?.serialize()?.hash,
        attachments = ltx.attachments.map { it.id },
        // serializationFactoryService = serializationFactoryService,
        componentGroupLeafDigestService = componentGroupLeafDigestService,
        nodeDigestService = nodeDigestService,
        componentPaddingConfiguration = componentPaddingConfiguration
    )
}

class ZKProverTransactionFactory {
    companion object {
        fun create(
            ltx: LedgerTransaction,
            componentGroupLeafDigestService: DigestService,
            nodeDigestService: DigestService,
            componentPaddingConfiguration: ComponentPaddingConfiguration = DEFAULT_PADDING_CONFIGURATION
        ): ZKProverTransaction {
            requireThat {
                // "A notary must always be set on a ZKProverTransaction" using (ltx.notary != null)
                "There must be exactly one command on a ZKProverTransactions" using (ltx.commands.size == 1)
            }

            return ZKProverTransaction(
                inputs = ltx.inputs,
                outputs = ltx.outputs.map { TransactionState(data = it.data as ZKContractState, notary = it.notary) },
                references = ltx.references,
                command = ltx.commands.map { it.toZKCommand() }.single(),
                notary = ltx.notary!!,
                timeWindow = ltx.timeWindow,
                privacySalt = ltx.privacySalt,
                networkParametersHash = ltx.networkParameters?.serialize()?.hash,
                attachments = ltx.attachments.map { it.id },
                componentGroupLeafDigestService = componentGroupLeafDigestService,
                nodeDigestService = nodeDigestService,
                componentPaddingConfiguration = componentPaddingConfiguration
            )
        }

        val DEFAULT_PADDING_CONFIGURATION by lazy {
            // TODO: Make this more generic, based on the contract and asking it to provide the padding state
            val emptyState = TestContract.TestState(ZKNulls.NULL_PARTY, 0)
            val transactionStateFiller = ComponentPaddingConfiguration.Filler.TransactionState(
                TransactionState(emptyState, notary = ZKNulls.NULL_PARTY)
            )
            val stateAndRefFiller = ComponentPaddingConfiguration.Filler.StateAndRef(
                StateAndRef(
                    TransactionState(emptyState, notary = ZKNulls.NULL_PARTY),
                    EMPTY_STATEREF
                )
            )

            ComponentPaddingConfiguration.Builder()
                .inputs(2, stateAndRefFiller)
                .outputs(2, transactionStateFiller)
                .references(2, stateAndRefFiller)
                .attachments(2, ComponentPaddingConfiguration.Filler.SecureHash(SecureHash.zeroHash))
                .signers(2)
                .build()
        }
    }
}
