package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.testing.dsl.interfaces.VerificationMode
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes

public interface TestDSLZKTransactionService {
    /**
     * Called to execute setup, prove and verify in one go
     */
    public fun run(wtx: WireTransaction, zkNetworkParameters: ZKNetworkParameters): SignedZKVerifierTransaction
    public fun verify(wtx: WireTransaction, zkNetworkParameters: ZKNetworkParameters, mode: VerificationMode): SignedZKVerifierTransaction

    public fun calculatePublicInput(
        serviceHub: ServiceHub,
        tx: ZKVerifierTransaction,
        commandMetadata: ResolvedZKCommandMetadata
    ): PublicInput {

        val privateInputIndices = commandMetadata.inputs.map { it.index }
        val privateInputHashes =
            serviceHub.collectUtxoInfos(tx.inputs).map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }
                .filterIndexed { index, _ -> privateInputIndices.contains(index) }

        val privateReferenceIndices = commandMetadata.references.map { it.index }
        val privateReferenceHashes = serviceHub.collectUtxoInfos(tx.references)
            .map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }
            .filterIndexed { index, _ -> privateReferenceIndices.contains(index) }

        // Fetch output component hashes for private outputs of the command
        val privateOutputIndices = commandMetadata.outputs.map { it.index }
        val privateOutputHashes = tx.outputHashes().filterIndexed { index, _ ->
            privateOutputIndices.contains(index)
        }

        return PublicInput(
            outputComponentHashes = privateOutputHashes,
            attachmentComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal),
            commandComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.COMMANDS_GROUP.ordinal),
            notaryComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.NOTARY_GROUP.ordinal),
            parametersComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.PARAMETERS_GROUP.ordinal),
            timeWindowComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal),
            signersComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.SIGNERS_GROUP.ordinal),

            inputUtxoHashes = privateInputHashes,
            referenceUtxoHashes = privateReferenceHashes
        )
    }
}
