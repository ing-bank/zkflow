package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKContractState
import com.ing.zknotary.common.states.toZKStateAndRef
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateAndRef
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
            componentPadding: Map<ComponentGroupEnum, Int> = DEFAULT_PADDING
        ): ZKProverTransaction {
            requireThat {
                "A notary must always be set on a ZKProverTransaction" using (ltx.notary != null)
                "There must be exactly one command on a ZKProverTransactions" using (ltx.commands.size == 1)

                "Size of Inputs group must be defined with a positive number" using (
                    componentPadding.getOrDefault(ComponentGroupEnum.INPUTS_GROUP, -1) > 0)
                "Size of Outputs group must be defined with a positive number" using (
                    componentPadding.getOrDefault(ComponentGroupEnum.OUTPUTS_GROUP, -1) > 0)
                "Size of References group must be defined with a positive number" using (
                    componentPadding.getOrDefault(ComponentGroupEnum.REFERENCES_GROUP, -1) > 0)

                "Inputs' size exceeds quantity accepted by ZK circuit" using (
                    ltx.inputs.size <= componentPadding.getOrDefault(
                        ComponentGroupEnum.INPUTS_GROUP, ltx.inputs.size - 1))
                "Outputs' size exceeds quantity accepted by ZK circuit" using (
                    ltx.outputs.size <= componentPadding.getOrDefault(
                        ComponentGroupEnum.OUTPUTS_GROUP, ltx.outputs.size - 1))
                "References' size exceeds quantity accepted by ZK circuit" using (
                    ltx.references.size <= componentPadding.getOrDefault(
                        ComponentGroupEnum.REFERENCES_GROUP, ltx.references.size - 1))

                // TODO: what if inputs are empty? it's not possible then to construct empty states
                "Inputs must implement ZKContractState" using ltx.inputs.all { it.state.data is ZKContractState }
                "Outputs must implement ZKContractState" using ltx.outputs.all { it.data is ZKContractState }
                "References must implement ZKContractState" using ltx.references.all { it.state.data is ZKContractState }
            }

            return ZKProverTransaction(
                inputs = ltx.inputs.map {
                    (it as StateAndRef<ZKContractState>).toZKStateAndRef(
                        serializationFactoryService,
                        componentGroupLeafDigestService
                    )
                },
                outputs = ltx.outputs.map {
                    (it as TransactionState<ZKContractState>).toZKStateAndRef(
                        serializationFactoryService,
                        componentGroupLeafDigestService
                    )
                },
                references = ltx.references.map {
                    (it as StateAndRef<ZKContractState>).toZKStateAndRef(
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

        val DEFAULT_PADDING = mapOf(
            ComponentGroupEnum.INPUTS_GROUP to 2,
            ComponentGroupEnum.OUTPUTS_GROUP to 2,
            ComponentGroupEnum.REFERENCES_GROUP to 2,
            ComponentGroupEnum.SIGNERS_GROUP to 2
        )
    }
}
