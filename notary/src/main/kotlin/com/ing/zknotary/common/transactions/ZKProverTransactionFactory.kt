package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.toZKCommand
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.node.services.ZKTransactionStorage
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.DigestService
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction

/**
 * This function deterministically creates a [ZKProverTransaction] from a [WireTransaction].
 *
 * This is deterministic, because the [ZKProverTransaction] reuses the [PrivacySalt] from the WireTransaction.
 */
fun WireTransaction.toWitness(
    services: ServiceHub,
    zkTransactionStorage: ZKTransactionStorage,
    componentGroupLeafDigestService: DigestService,
    nodeDigestService: DigestService = componentGroupLeafDigestService
): Witness {
    require(commands.size == 1) { "There must be exactly one command on a ZKProverTransaction" }

    // Look up the ZKid for each WireTransaction.id
    fun List<StateAndRef<*>>.mapToZkid(): List<StateAndRef<*>> {
        return map {
            val zkid = checkNotNull(zkTransactionStorage.map.get(it.ref.txhash)) {
                "Unexpectedly could not find the tx id map for ${it.ref.txhash}. Did you run ResolveTransactionsFlow before?"
            }
            StateAndRef(it.state, StateRef(zkid, it.ref.index))
        }
    }

    val ltx = toLedgerTransaction(services)

    val ptx = ZKProverTransaction(
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

    // Because the PrivacySalt of the WireTransaction is reused to create the ProverTransactions,
    // the nonces are also identical from WireTransaction to ZKProverTransaction.
    // This means we can collect the UTXO nonces for the inputs and references of the wiretransaction and it should
    // just work.
    // When we move to full backchain privacy and no longer have the WireTransactions at all, we will
    // promote the ZKProverTransactions to first-class citizens and then they will be saved in the vault as WireTransactions
    // are now.
    fun List<PaddingWrapper<StateAndRef<ContractState>>>.collectUtxoNonces() = mapIndexed { index, it ->
        when (it) {
            is PaddingWrapper.Filler -> {
                // TODO: The hash to use should probably come from paddingConfig
                // When it is a padded state, the nonce is ALWAYS a zerohash
                ptx.componentGroupLeafDigestService.zeroHash
            }
            is PaddingWrapper.Original -> {
                val outputTx =
                    services.validatedTransactions.getTransaction(it.content.ref.txhash)
                        ?: error("Could not fetch output transaction for StateRef ${it.content.ref}")
                outputTx.tx.availableComponentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!![it.content.ref.index]
            }
        }
    }

    // Collect the nonces for the outputs pointed to by the inputs and references.
    val inputNonces = ptx.padded.inputs().collectUtxoNonces()
    val referenceNonces = ptx.padded.references().collectUtxoNonces()

    return Witness(ptx, inputNonces = inputNonces, referenceNonces = referenceNonces)
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
