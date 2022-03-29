package com.ing.zkflow.common.transactions.verification

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransactionWithoutProofs
import com.ing.zkflow.common.transactions.zkToFilteredLedgerTransaction
import com.ing.zkflow.common.transactions.zkTransactionMetadataOrNull
import com.ing.zkflow.common.zkp.ZKTransactionService
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

// TODO @Aleksei: as discussed, please replace with a proper service, with the classloader and everything?
class ZKTransactionVerifierService(
    private val services: ServiceHub,
    private val zkTransactionService: ZKTransactionService,
) {
    fun verify(svtx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean) {
        val vtx = svtx.tx
        validatePrivateComponents(vtx)

        ensureNoUncheckedPrivateOutputs(vtx)
        ensureNoUncheckedPrivateInputs(vtx)

        if (checkSufficientSignatures) svtx.verifyRequiredSignatures()

        validatePublicComponents(vtx)
    }

    fun verify(stx: SignedTransaction, checkSufficientSignatures: Boolean) {
        val wtx = stx.tx
        val vtx = validatePrivateComponents(wtx)

        ensureNoUncheckedPrivateOutputs(vtx)
        ensureNoUncheckedPrivateInputs(wtx)

        if (checkSufficientSignatures) stx.verifyRequiredSignatures()

        validatePublicComponents(vtx)
    }

    private fun validatePrivateComponents(vtx: ZKVerifierTransaction) = zkTransactionService.verify(vtx)

    private fun validatePrivateComponents(wtx: WireTransaction): ZKVerifierTransactionWithoutProofs = zkTransactionService.verify(wtx)

    private fun validatePublicComponents(tx: ZKVerifierTransaction) = tx.zkToFilteredLedgerTransaction(services).verify()

    /*
     * Resolve all inputs: if the UTXOs they point to are private in their creating transaction,
     * they should be mentioned in the transaction metadata inputs.
     *
     * If they are mentioned, it means that the ZKP circuits for this transaction ensure that the contents of those private outputs
     * are part of the witness used to create this transaction's proofs, and that they match the hash the verifier knows for them, and
     * that they are valid according to the contract rules from the circuit.
     *
     * If they are not mentioned in the transaction metadata, it means that they are not part of the witness, and therefore not
     * validated in any way. This means that it is possible to consume states without checks, resulting in a denial of state attack.
     */
    private fun ensureNoUncheckedPrivateInputs(tx: TraversableTransaction) {
        // The indexes of the inputs whose UTXOs are private in their creating transaction.
        val actualPrivateInputIndexes = tx.inputs.mapIndexedNotNull { index, stateRef ->
            val utxoIsPrivate =
                zkTransactionService.vtxStorage.getTransaction(stateRef.txhash)?.tx?.isPrivateComponent(
                    ComponentGroupEnum.OUTPUTS_GROUP,
                    stateRef.index
                )
                    ?: error("Could not find ZKVerifierTransaction in storage with id ${stateRef.txhash}")
            if (utxoIsPrivate) index else null
        }

        if (actualPrivateInputIndexes.isNotEmpty()) {
            // Collect the indexes of inputs which are relevant to the circuits of this transaction and therefore whose UTXOs are resolved and checked by the circuits
            val circuitRelevantInputIndexes =
                tx.zkTransactionMetadataOrNull()?.commands?.flatMap { it.inputs.map { input -> input.index } }?.toSet() ?: error(
                    "There are inputs in the transaction whose UTXOs are private, but no metadata for them. " + "This means the ZKPs don't prove anything about these inputs and they are consumed without verification"
                )
            require(actualPrivateInputIndexes.all { it in circuitRelevantInputIndexes }) {
                "There are private inputs that are not mentioned in the metadata. " +
                    "This means the ZKPs don't prove anything about these inputs and they are consumed without verification!" +
                    "Unsafe inputs at index: ${(actualPrivateInputIndexes - circuitRelevantInputIndexes).joinToString(", ")}"
            }
        }
    }

    /*
     * Check outputs: if they are hidden in the filteredcomponentsgroup, i.e. the actual transaction,
     * they should be mentioned one of the command metadata. That ensures that their contents are part of the witness and therefore
     * validated by the ZKP circuit.
    */
    private fun ensureNoUncheckedPrivateOutputs(vtx: ZKVerifierTransaction) {
        val actualPrivateOutputIndexes = vtx.privateComponentIndexes(ComponentGroupEnum.OUTPUTS_GROUP)
        if (actualPrivateOutputIndexes.isNotEmpty()) {
            val expectedPrivateOutputIndexes =
                vtx.zkTransactionMetadataOrNull()?.commands?.flatMap { it.outputs.map { output -> output.index } }?.toSet() ?: error(
                    "There are private outputs in the transaction, but no metadata for them. " + "This means the ZKPs don't prove anything about these outputs and they are created without verification"
                )
            require(actualPrivateOutputIndexes.all { it in expectedPrivateOutputIndexes }) {
                "There are private outputs that are not mentioned in the metadata. " +
                    "This means the ZKPs don't prove anything about these outputs!" +
                    "Unsafe outputs at index: ${(actualPrivateOutputIndexes - expectedPrivateOutputIndexes).joinToString(", ")}"
            }
        }
    }
}
