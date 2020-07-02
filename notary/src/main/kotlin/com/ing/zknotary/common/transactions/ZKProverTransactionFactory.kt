package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.common.states.toZKStateAndRef
import com.ing.zknotary.common.util.ComponentPadding
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.DigestService
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction

class ZKProverTransactionFactory {
    companion object {
        fun create(
            ltx: LedgerTransaction,
            serializationFactoryService: SerializationFactoryService,
            componentGroupLeafDigestService: DigestService,
            nodeDigestService: DigestService = componentGroupLeafDigestService,
            componentPadding: ComponentPadding = DEFAULT_PADDING
        ): ZKProverTransaction {
            requireThat {
                "A notary must always be set on a ZKProverTransaction" using (ltx.notary != null)
                "There must be exactly one command on a ZKProverTransactions" using (ltx.commands.size == 1)
            }

            return ZKProverTransaction(
                inputs = ltx.inputs.map {
                    it.toZKStateAndRef(
                        serializationFactoryService,
                        componentGroupLeafDigestService
                    )
                },
                outputs = ltx.outputs.map {
                    it.toZKStateAndRef(
                        serializationFactoryService,
                        componentGroupLeafDigestService
                    )
                },
                references = ltx.references.map {
                    it.toZKStateAndRef(
                        serializationFactoryService,
                        componentGroupLeafDigestService
                    )
                },
                command = ltx.commands.map { Command(it.value, it.signers) }.single(),
                notary = ltx.notary!!,
                timeWindow = ltx.timeWindow,
                privacySalt = ltx.privacySalt,
                networkParametersHash = ltx.networkParameters?.serialize()?.hash,
                attachments = ltx.attachments.map { it.id },
                serializationFactoryService = serializationFactoryService,
                componentGroupLeafDigestService = componentGroupLeafDigestService,
                nodeDigestService = nodeDigestService,
                componentPadding = componentPadding
            )
        }

        private val DEFAULT_PADDING by lazy {
            val emptyState = TestContract.TestState(ZKNulls.NULL_PARTY, 0)
            val filler = ComponentPadding.Filler.ZKStateAndRef(
                ZKStateAndRef(
                    TransactionState(emptyState, notary = ZKNulls.NULL_PARTY),
                    ZKStateRef.empty()
                )
            )

            ComponentPadding.Builder()
                .inputs(2, filler)
                .outputs(2, filler)
                .references(2, filler)
                .signers(2)
                .build()
        }
    }
}
