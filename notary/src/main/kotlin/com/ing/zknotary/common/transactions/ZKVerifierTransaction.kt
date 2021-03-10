package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKCommandData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import java.security.PublicKey
import java.util.function.Predicate

@Suppress("LongParameterList")
@CordaSerializable
class ZKVerifierTransaction(
    val proof: ByteArray,

    // Outputs are not visible in a normal FilteredTransaction, so we 'leak' some info here: the amount of outputs.
    // Outputs are the leaf hashes of the outputs component group. This is the only group where:
    // * we don't provide the leaf contents but provide the leaf hashes. In other cases we provide either all contents
    //   of all leaves, or we provide nothing (hide all leaves completely) and we just use the component group hash
    //   to build the Merkle tree. In the case of outputs, verifiers need to
    //   be able to see the component leaf hashes of past transactions in the backchain, so that they can:
    //   * For each input StateRef in the head transaction, find the matching output hash in the previous tx. Then for the proof verification
    //     they provide this list of output hashes (for the inputs being consumed) as public input. The circuit will enforce
    //     that for each input contents from the witness,  when combined with their nonce, should hash to the same hash as
    //     provided for that input in the public input.
    val outputHashes: List<SecureHash>,

    /**
     * This value will contain as many hashes as there are component groups,
     * otherwise fail.
     * Order of the elements corresponds to the order groups listed in ComponentGroupEnum.
     *
     * TODO: when we revert back to the normal WireTransaction instead of ZKProverTransaction,
     * this will become 'dynamic' again to support unknown groups. This should be reflected in Zinc and in
     * ZKVerifierTransaction
     */
    val groupHashes: List<SecureHash>,

    digestService: DigestService,

    componentGroups: List<ComponentGroup>

) : TraversableTransaction(componentGroups, digestService) {

    override val id: SecureHash by lazy { MerkleTree.getMerkleTree(groupHashes, digestService).hash }

    val zkCommandData: ZKCommandData = commands.single().value as ZKCommandData

    /** Public keys that need to be fulfilled by signatures in order for the transaction to be valid. */
    val requiredSigningKeys: Set<PublicKey>
        get() {
            val commandKeys = commands.flatMap { it.signers }.toSet()
            // TODO: prevent notary field from being set if there are no inputs and no time-window.
            @Suppress("ComplexCondition")
            return if (notary != null && (inputs.isNotEmpty() || references.isNotEmpty() || timeWindow != null)) {
                commandKeys + notary.owningKey
            } else {
                commandKeys
            }
        }

    fun verifyOutputsGroupHash() {

        // To prevent Corda's automatic promotion of a single leaf to the Merkle root,
        // ensure, there are at least 2 elements.
        // See, https://github.com/corda/corda/issues/6680
        val paddedOutputHashes = outputHashes // TODO .pad(2, digestService.zeroHash)

        require(
            MerkleTree.getMerkleTree(
                paddedOutputHashes,
                digestService
            ).hash == groupHashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]
        )
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    companion object {

        fun new(wtx: WireTransaction, proof: ByteArray): ZKVerifierTransaction {

            // Here we don't need to filter anything, we only create FTX to be able to access hashes (they are internal in WTX)
            val ftx = FilteredTransaction.buildFilteredTransaction(wtx, Predicate { true })

            return ZKVerifierTransaction(
                proof = proof,
                outputHashes = outputHashes(wtx, ftx),
                groupHashes = ftx.groupHashes,
                digestService = wtx.digestService,
                componentGroups = createComponentGroups(wtx)
            )
        }

        private fun createComponentGroups(wtx: WireTransaction): List<ComponentGroup> {

            // We don't filter anything here because of unfriendly Predicate interface...
            val ftx = wtx.buildFilteredTransaction(Predicate { true })

//             ... filtering happens here - we hide all groups except mentioned below.
//               For hidden groups we only store group hashes. No components, nonces or anything else
            return ftx.filteredComponentGroups.filter {
                it.groupIndex in listOf(
                    ComponentGroupEnum.INPUTS_GROUP.ordinal,
                    ComponentGroupEnum.REFERENCES_GROUP.ordinal,
                    ComponentGroupEnum.NOTARY_GROUP.ordinal,
                    ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
                    ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
                    ComponentGroupEnum.SIGNERS_GROUP.ordinal,
                    ComponentGroupEnum.COMMANDS_GROUP.ordinal
                )
            }
        }

        private fun outputHashes(wtx: WireTransaction, ftx: FilteredTransaction): List<SecureHash> {
            val nonces = ftx.filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }!!.nonces
            return wtx.componentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }!!.components.mapIndexed { componentIndex, component -> wtx.digestService.componentHash(nonces[componentIndex], component) }
        }
    }
}
