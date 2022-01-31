package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.zkp.metadata.ZkpVisibility
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.PartialMerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredComponentGroup
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import java.security.PublicKey

@CordaSerializable
class ZKVerifierTransaction internal constructor(
    override val id: SecureHash,

    val proofs: Map<ZKCommandClassName, Proof>,

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
     * This value will contain as many hashes as there are component groups, otherwise fail.
     * Order of the elements corresponds to the order of groups listed in ComponentGroupEnum.
     */
    val groupHashes: List<SecureHash>,

    val filteredComponentGroups: List<FilteredComponentGroup>,

    digestService: DigestService
) : TraversableTransaction(
    filteredComponentGroups,
    digestService
) { // Preferably we directly extend FilteredTransaction, but its constructors are internal

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

    /**
     * Normally, all these checks would also happen on construction, to guard against invalid transactions.
     * In this case, we choose to only do this on verify, because it is an expensive operation and is always called on
     * transaction verification anyway, which always happens just before it is stored in verified storage.
     *
     * Much of this is directly lifted from FilteredTransaction because we can't extend it because its constructors are internal
     */
    fun verify() {

        require(groupHashes.isNotEmpty()) { "At least one component group hash is required" }
        // Verify the top level Merkle tree (group hashes are its leaves, including allOnesHash for empty list or null
        // components in WireTransaction).
        require(MerkleTree.getMerkleTree(groupHashes, digestService).hash == id) {
            "Top level Merkle tree cannot be verified against transaction's id"
        }

        // Check that output hashes indeed produce provided group hash for output group
        require(
            MerkleTree.getMerkleTree(
                outputHashes,
                digestService
            ).hash == groupHashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]
        )

        // Compute partial Merkle roots for each filtered component and verify each of the partial Merkle trees.
        filteredComponentGroups.forEach { (groupIndex, components, nonces, groupPartialTree) ->
            require(groupIndex < groupHashes.size) { "There is no matching component group hash for group $groupIndex" }
            val groupMerkleRoot = groupHashes[groupIndex]
            require(groupMerkleRoot == PartialMerkleTree.rootAndUsedHashes(groupPartialTree.root, mutableListOf())) {
                "Partial Merkle tree root and advertised full Merkle tree root for component group $groupIndex do not match"
            }
            require(
                groupPartialTree.verify(
                    groupMerkleRoot,
                    components.mapIndexed { index, component -> digestService.componentHash(nonces[index], component) }
                )
            ) {
                "Visible components in group $groupIndex cannot be verified against their partial Merkle tree"
            }
        }
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    companion object {

        fun fromWireTransaction(wtx: WireTransaction, proofs: Map<String, ByteArray>): ZKVerifierTransaction {

            // Here we don't need to filter anything, we only create FTX to be able to access hashes (they are internal in WTX)
            val ftx = FilteredTransaction.buildFilteredTransaction(wtx) { true }

            // Filter the component groups based on visibility data from 'zkTransactionMetadata'
            val filteredComponentGroups = filterPrivateComponents(
                wtx,
                ftx.filteredComponentGroups.associate { it.groupIndex to it.nonces }
            )

            return ZKVerifierTransaction(
                id = wtx.id,
                proofs = proofs,
                outputHashes = outputHashes(wtx, ftx),
                groupHashes = ftx.groupHashes,
                digestService = wtx.digestService,
                filteredComponentGroups = filteredComponentGroups
            )
        }

        private fun outputHashes(wtx: WireTransaction, ftx: FilteredTransaction): List<SecureHash> {
            val nonces = ftx.filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }!!.nonces
            return wtx.componentGroups
                .find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }!!
                .components.mapIndexed { componentIndex, component ->
                    wtx.digestService.componentHash(nonces[componentIndex], component)
                }
        }

        private fun filterPrivateComponents(
            wtx: WireTransaction,
            availableComponentNonces: Map<Int, List<SecureHash>>,
        ): List<FilteredComponentGroup> {

            val filteredSerialisedComponents: MutableMap<Int, MutableList<OpaqueBytes>> = hashMapOf()
            val filteredComponentNonces: MutableMap<Int, MutableList<SecureHash>> = hashMapOf()
            val filteredComponentHashes: MutableMap<Int, MutableList<SecureHash?>> = hashMapOf() // Required for partial Merkle tree generation.

            val zkTransactionMetadata = wtx.zkTransactionMetadata()

            wtx.componentGroups.forEach { componentGroup ->
                val groupIndex = componentGroup.groupIndex

                componentGroup.components.forEachIndexed { componentIndex, serialisedComponent ->
                    if (zkTransactionMetadata.getComponentVisibility(groupIndex, componentIndex) != ZkpVisibility.Private) {

                        // Init lists if they don't exist
                        if (!filteredSerialisedComponents.containsKey(groupIndex)) {
                            filteredSerialisedComponents[groupIndex] = mutableListOf()
                            filteredComponentNonces[groupIndex] = mutableListOf()
                            filteredComponentHashes[groupIndex] = mutableListOfNulls(wtx.componentGroups.first { it.groupIndex == groupIndex }.components.size)
                        }

                        // So now we are quite sure they are not null
                        filteredSerialisedComponents[groupIndex]!!.add(serialisedComponent)
                        filteredComponentNonces[groupIndex]!!.add(availableComponentNonces[groupIndex]!![componentIndex])
                        filteredComponentHashes[groupIndex]!![componentIndex] = componentHash(wtx, availableComponentNonces, groupIndex, componentIndex)
                    }
                }
            }

            val filteredComponentGroups: MutableList<FilteredComponentGroup> = mutableListOf()
            filteredSerialisedComponents.forEach { (groupIndex, value) ->
                filteredComponentGroups.add(
                    FilteredComponentGroup(groupIndex, value, filteredComponentNonces[groupIndex]!!, createPartialMerkleTree(wtx, availableComponentNonces, filteredComponentHashes, groupIndex))
                )
            }
            return filteredComponentGroups
        }

        private fun mutableListOfNulls(size: Int): MutableList<SecureHash?> {
            val list = ArrayList<SecureHash?>(size)
            (0..size).forEach { _ -> list.add(null) }
            return list
        }

        private fun componentHash(wtx: WireTransaction, nonces: Map<Int, List<SecureHash>>, groupIndex: Int, componentIndex: Int): SecureHash {
            val componentBytes = wtx.componentGroups.first { it.groupIndex == groupIndex }.components[componentIndex]
            return wtx.digestService.componentHash(nonces[groupIndex]!![componentIndex], componentBytes)
        }

        private fun createPartialMerkleTree(wtx: WireTransaction, nonces: Map<Int, List<SecureHash>>, hashes: Map<Int, List<SecureHash?>>, groupIndex: Int): PartialMerkleTree {
            return PartialMerkleTree.build(
                // we've already calculated filtered hashes, so we only need to calculate missing ones
                MerkleTree.getMerkleTree(
                    wtx.componentGroups.first { it.groupIndex == groupIndex }.components.indices.map {
                        componentIndex ->
                        hashes[groupIndex]?.get(componentIndex) ?: componentHash(wtx, nonces, groupIndex, componentIndex)
                    }.toMutableList(),
                    wtx.digestService
                ),
                hashes[groupIndex]!!.filterNotNull()
            )
        }
    }
}
