package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
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
            val filteredComponentGroups = filterWithoutFun(
                wtx,
                ftx.filteredComponentGroups.associate { it.groupIndex to it.nonces },
                wtx.zkTransactionMetadata()
            )

            return ZKVerifierTransaction(
                id = ftx.id,
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

        @Suppress("ComplexMethod")
        private fun filterWithoutFun(
            wtx: WireTransaction,
            availableComponentNonces: Map<Int, List<SecureHash>>,
            zkTransactionMetadata: ResolvedZKTransactionMetadata
        ): List<FilteredComponentGroup> {
            val filteredSerialisedComponents: MutableMap<Int, MutableList<OpaqueBytes>> = hashMapOf()
            val filteredComponentNonces: MutableMap<Int, MutableList<SecureHash>> = hashMapOf()
            val filteredComponentHashes: MutableMap<Int, MutableList<SecureHash>> = hashMapOf() // Required for partial Merkle tree generation.
            var signersIncluded = false

            fun componentHash(wtx: WireTransaction, groupIndex: Int, componentIndex: Int): SecureHash {
                val componentBytes = wtx.componentGroups.first { it.groupIndex == groupIndex }.components[componentIndex]
                return wtx.digestService.componentHash(availableComponentNonces[groupIndex]!![componentIndex], componentBytes)
            }

            fun filter(groupIndex: Int, componentIndex: Int) {
                if (zkTransactionMetadata.getComponentVisibility(groupIndex, componentIndex) == ZkpVisibility.Private) return

                val group = filteredSerialisedComponents[groupIndex]
                // Because the filter passed, we know there is a match. We also use first Vs single as the init function
                // of WireTransaction ensures there are no duplicated groups.
                val serialisedComponent = wtx.componentGroups.first { it.groupIndex == groupIndex }.components[componentIndex]
                if (group == null) {
                    // As all of the helper Map structures, like availableComponentNonces, availableComponentHashes
                    // and groupsMerkleRoots, are computed lazily via componentGroups.forEach, there should always be
                    // a match on Map.get ensuring it will never return null.
                    filteredSerialisedComponents[groupIndex] = mutableListOf(serialisedComponent)
                    filteredComponentNonces[groupIndex] = mutableListOf(availableComponentNonces[groupIndex]!![componentIndex])
                    filteredComponentHashes[groupIndex] = mutableListOf(componentHash(wtx, groupIndex, componentIndex))
                } else {
                    group.add(serialisedComponent)
                    // If the group[componentGroupIndex] existed, then we guarantee that
                    // filteredComponentNonces[componentGroupIndex] and filteredComponentHashes[componentGroupIndex] are not null.
                    filteredComponentNonces[groupIndex]!!.add(availableComponentNonces[groupIndex]!![componentIndex])
                    filteredComponentHashes[groupIndex]!!.add(componentHash(wtx, groupIndex, componentIndex))
                }
                // If at least one command is visible, then all command-signers should be visible as well.
                // This is required for visibility purposes, see FilteredTransaction.checkAllCommandsVisible() for more details.
                if (groupIndex == ComponentGroupEnum.COMMANDS_GROUP.ordinal && !signersIncluded) {
                    signersIncluded = true
                    val signersGroupIndex = ComponentGroupEnum.SIGNERS_GROUP.ordinal
                    // There exist commands, thus the signers group is not empty.
                    val signersGroupComponents = wtx.componentGroups.first { it.groupIndex == signersGroupIndex }
                    filteredSerialisedComponents[signersGroupIndex] = signersGroupComponents.components.toMutableList()
                    filteredComponentNonces[signersGroupIndex] = availableComponentNonces[signersGroupIndex]!!.toMutableList()
                    filteredComponentHashes[signersGroupIndex] = wtx.componentGroups.first { it.groupIndex == signersGroupIndex }.components.indices.map { index -> componentHash(wtx, signersGroupIndex, index) }.toMutableList()
                }
            }

            fun updateFilteredComponents() {
                wtx.inputs.indices.forEach { internalIndex -> filter(ComponentGroupEnum.INPUTS_GROUP.ordinal, internalIndex) }
                wtx.outputs.indices.forEach { internalIndex -> filter(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, internalIndex) }
                wtx.commands.indices.forEach { internalIndex -> filter(ComponentGroupEnum.COMMANDS_GROUP.ordinal, internalIndex) }
                wtx.attachments.indices.forEach { internalIndex -> filter(ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal, internalIndex) }
                if (wtx.notary != null) filter(ComponentGroupEnum.NOTARY_GROUP.ordinal, 0)
                if (wtx.timeWindow != null) filter(ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal, 0)
                // Note that because [inputs] and [references] share the same type [StateRef], we use a wrapper for references [ReferenceStateRef],
                // when filtering. Thus, to filter-in all [references] based on type, one should use the wrapper type [ReferenceStateRef] and not [StateRef].
                // Similar situation is for network parameters hash and attachments, one should use wrapper [NetworkParametersHash] and not [SecureHash].
                wtx.references.indices.forEach { internalIndex -> filter(ComponentGroupEnum.REFERENCES_GROUP.ordinal, internalIndex) }
                wtx.networkParametersHash?.let { filter(ComponentGroupEnum.PARAMETERS_GROUP.ordinal, 0) }
                // It is highlighted that because there is no a signers property in TraversableTransaction,
                // one cannot specifically filter them in or out.
                // The above is very important to ensure someone won't filter out the signers component group if at least one
                // command is included in a FilteredTransaction.

                // It's sometimes possible that when we receive a WireTransaction for which there is a new or more unknown component groups,
                // we decide to filter and attach this field to a FilteredTransaction.
                // An example would be to redact certain contract state types, but otherwise leave a transaction alone,
                // including the unknown new components.
                wtx.componentGroups
                    .filter { it.groupIndex >= ComponentGroupEnum.values().size }
                    .forEach { componentGroup -> componentGroup.components.indices.forEach { internalIndex -> filter(componentGroup.groupIndex, internalIndex) } }
            }

            fun createPartialMerkleTree(componentGroupIndex: Int): PartialMerkleTree {
                return PartialMerkleTree.build(
                    // TODO here we've already calculated these hashed so need to store them somewhere locally to not recalculate
                    MerkleTree.getMerkleTree(wtx.componentGroups.first { it.groupIndex == componentGroupIndex }.components.indices.map { index -> componentHash(wtx, componentGroupIndex, index) }.toMutableList(), wtx.digestService),
                    filteredComponentHashes[componentGroupIndex]!!
                )
            }

            fun createFilteredComponentGroups(): List<FilteredComponentGroup> {
                updateFilteredComponents()
                val filteredComponentGroups: MutableList<FilteredComponentGroup> = mutableListOf()
                filteredSerialisedComponents.forEach { (groupIndex, value) ->
                    filteredComponentGroups.add(FilteredComponentGroup(groupIndex, value, filteredComponentNonces[groupIndex]!!, createPartialMerkleTree(groupIndex)))
                }
                return filteredComponentGroups
            }

            return createFilteredComponentGroups()
        }
    }
}
