package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes

public interface TestDSLZKTransactionService {
    /**
     * Called to execute setup, prove and verify in one go
     */
    public fun run(wtx: WireTransaction)
    public fun verify(wtx: WireTransaction, mode: VerificationMode)

    public fun calculatePublicInput(serviceHub: ServiceHub, tx: ZKVerifierTransaction, commandMetadata: ResolvedZKCommandMetadata): PublicInput {

        val privateInputIndices = commandMetadata.inputs.map { it.index }
        val privateInputHashes =
            serviceHub.collectUtxoInfos(tx.inputs)
                .map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }
                .filterIndexed { index, _ -> privateInputIndices.contains(index) }

        val privateReferenceIndices = commandMetadata.references.map { it.index }
        val privateReferenceHashes =
            serviceHub.collectUtxoInfos(tx.references)
                .map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }
                .filterIndexed { index, _ -> privateReferenceIndices.contains(index) }

        // Fetch output component hashes for private outputs of the command
        val privateOutputIndices = commandMetadata.outputs.map { it.index }
        val privateOutputHashes = tx.outputHashes.filterIndexed { index, _ ->
            privateOutputIndices.contains(index)
        }

        return PublicInput(
            inputComponentHashes = emptyList(), // StateRefs are always public
            outputComponentHashes = privateOutputHashes,
            referenceComponentHashes = emptyList(), // StateRefs are always public
            attachmentComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal].orEmpty(),
            commandComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.COMMANDS_GROUP.ordinal].orEmpty(),
            notaryComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.NOTARY_GROUP.ordinal].orEmpty(),
            parametersComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.PARAMETERS_GROUP.ordinal].orEmpty(),
            timeWindowComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal].orEmpty(),
            signersComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.SIGNERS_GROUP.ordinal].orEmpty(),

            inputUtxoHashes = privateInputHashes,
            referenceUtxoHashes = privateReferenceHashes
        )
    }
}
