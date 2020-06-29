package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.toZKStateAndRef
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
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
            componentPadding: Map<ComponentGroupEnum, Int> = DEFAULT_PADDING
        ): ZKProverTransaction {
            requireThat {
                "A notary must always be set on a ZKProverTransaction" using (ltx.notary != null)
                "There must be exactly one command on a ZKProverTransactions" using (ltx.commands.size == 1)
            }

            return ZKProverTransaction(
                inputs = ltx.inputs.map {
                    it .toZKStateAndRef(
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
                    it .toZKStateAndRef(
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

        // TODO
        val DEFAULT_PADDING = mapOf(
            ComponentGroupEnum.INPUTS_GROUP to 2,
            ComponentGroupEnum.OUTPUTS_GROUP to 2,
            ComponentGroupEnum.REFERENCES_GROUP to 2,
            ComponentGroupEnum.SIGNERS_GROUP to 2
        )
    }
}
