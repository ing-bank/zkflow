package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.metadata.commandMetadata
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

    public fun calculatePublicInput(serviceHub: ServiceHub, tx: ZKVerifierTransaction): PublicInput {
        val inputHashes =
            serviceHub.collectUtxoInfos(tx.inputs)
                .map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }
        val referenceHashes =
            serviceHub.collectUtxoInfos(tx.references)
                .map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }

        return PublicInput(
            inputComponentHashes = tx.publicInputHashes[ComponentGroupEnum.INPUTS_GROUP.ordinal].orEmpty(),
            outputComponentHashes = tx.outputHashes,
            referenceComponentHashes = tx.publicInputHashes[ComponentGroupEnum.REFERENCES_GROUP.ordinal].orEmpty(),
            attachmentComponentHashes = tx.publicInputHashes[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal].orEmpty(),
            commandComponentHashes = tx.publicInputHashes[ComponentGroupEnum.COMMANDS_GROUP.ordinal].orEmpty(),
            notaryComponentHashes = tx.publicInputHashes[ComponentGroupEnum.NOTARY_GROUP.ordinal].orEmpty(),
            parametersComponentHashes = tx.publicInputHashes[ComponentGroupEnum.PARAMETERS_GROUP.ordinal].orEmpty(),
            timeWindowComponentHashes = tx.publicInputHashes[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal].orEmpty(),
            signersComponentHashes = tx.publicInputHashes[ComponentGroupEnum.SIGNERS_GROUP.ordinal].orEmpty(),

            inputUtxoHashes = inputHashes,
            referenceUtxoHashes = referenceHashes
        )
    }
}
