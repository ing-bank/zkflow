package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.toZKCommand
import com.ing.zknotary.node.services.ZKTransactionStorage
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.DigestService
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction

fun WireTransaction.toZKProverTransaction(
    services: ServiceHub,
    zkTransactionStorage: ZKTransactionStorage,
    componentGroupLeafDigestService: DigestService,
    nodeDigestService: DigestService = componentGroupLeafDigestService
): ZKProverTransaction {
    requireThat {
        // "A notary must always be set on a ZKProverTransaction" using (notary != null)
        "There must be exactly one command on a ZKProverTransaction" using (commands.size == 1)
    }

    val ltx = toLedgerTransaction(services)

    fun List<StateAndRef<*>>.mapToZkid(): List<StateAndRef<*>> {
        return map {
            val zkid = checkNotNull(zkTransactionStorage.map.get(it.ref.txhash)) {
                "Unexpectedly could not find the tx id map for ${it.ref.txhash}. Did you run ResolveTransactionsFlow before?"
            }
            StateAndRef(it.state, StateRef(zkid, it.ref.index))
        }
    }

    return ZKProverTransaction(
        inputs = ltx.inputs.mapToZkid(),
        outputs = ltx.outputs.map { TransactionState(data = it.data, notary = it.notary) },
        references = ltx.references.mapToZkid(),
        command = ltx.commands.single().toZKCommand(),
        notary = ltx.notary!!,
        timeWindow = ltx.timeWindow,
        privacySalt = ltx.privacySalt,
        networkParametersHash = ltx.networkParameters?.serialize()?.hash,
        attachments = ltx.attachments.map { it.id },
        componentGroupLeafDigestService = componentGroupLeafDigestService,
        nodeDigestService = nodeDigestService
    )
}

// TODO: remove this, since it does not take into account the history and does not replace the input StateRefs with vtx.StateRefs.
class ZKProverTransactionFactory {
    companion object {
        fun create(
            ltx: LedgerTransaction,
            componentGroupLeafDigestService: DigestService,
            nodeDigestService: DigestService
        ): ZKProverTransaction {
            requireThat {
                // "A notary must always be set on a ZKProverTransaction" using (ltx.notary != null)
                "There must be exactly one command on a ZKProverTransactions" using (ltx.commands.size == 1)
            }

            return ZKProverTransaction(
                inputs = ltx.inputs,
                outputs = ltx.outputs.map { TransactionState(data = it.data, notary = it.notary) },
                references = ltx.references,
                command = ltx.commands.map { it.toZKCommand() }.single(),
                notary = ltx.notary!!,
                timeWindow = ltx.timeWindow,
                privacySalt = ltx.privacySalt,
                networkParametersHash = ltx.networkParameters?.serialize()?.hash,
                attachments = ltx.attachments.map { it.id },
                componentGroupLeafDigestService = componentGroupLeafDigestService,
                nodeDigestService = nodeDigestService
            )
        }
    }
}
