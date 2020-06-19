package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.toZKStateAndRef
import net.corda.core.contracts.Command
import net.corda.core.crypto.DigestService
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction

class ZKProverTransactionFactory {
    companion object {
        fun create(
            ltx: LedgerTransaction,
            serializationFactoryService: SerializationFactoryService,
            componentGroupLeafDigestService: DigestService,
            nodeDigestService: DigestService = componentGroupLeafDigestService
        ): ZKProverTransaction {
            require(ltx.notary != null) { "A notary must always be set on a ZKProverTransaction" }
            require(ltx.commands.size == 1) { "There must be exactly one command on a ZKProverTransactions" }

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
                nodeDigestService = nodeDigestService
            )
        }
    }
}
