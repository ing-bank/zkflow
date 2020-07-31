package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.common.states.toZKCommand
import com.ing.zknotary.common.states.toZKStateAndRef
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.DigestService
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction

class ZKProverTransactionFactory {
    companion object {
        fun create(
            ltx: LedgerTransaction,
            componentGroupLeafDigestService: DigestService,
            nodeDigestService: DigestService,
            componentPaddingConfiguration: ComponentPaddingConfiguration = DEFAULT_PADDING_CONFIGURATION
        ): ZKProverTransaction {
            requireThat {
                "A notary must always be set on a ZKProverTransaction" using (ltx.notary != null)
                "There must be exactly one command on a ZKProverTransactions" using (ltx.commands.size == 1)
            }

            return ZKProverTransaction(
                inputs = ltx.inputs.map {
                    it.toZKStateAndRef(componentGroupLeafDigestService)
                },
                outputs = ltx.outputs.map {
                    it.toZKStateAndRef(componentGroupLeafDigestService)
                },
                references = ltx.references.map {
                    it.toZKStateAndRef(componentGroupLeafDigestService)
                },
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

        private val DEFAULT_PADDING_CONFIGURATION by lazy {
            val emptyState = TestContract.TestState(ZKNulls.NULL_PARTY, 0)
            val filler = ComponentPaddingConfiguration.Filler.ZKStateAndRef(
                ZKStateAndRef(
                    TransactionState(emptyState, notary = ZKNulls.NULL_PARTY),
                    ZKStateRef.empty()
                )
            )

            ComponentPaddingConfiguration.Builder()
                .inputs(2, filler)
                .outputs(2, filler)
                .references(2, filler)
                .signers(2)
                .build()
        }
    }
}
