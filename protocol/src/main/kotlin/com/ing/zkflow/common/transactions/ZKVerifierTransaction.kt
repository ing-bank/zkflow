package com.ing.zkflow.common.transactions

import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import java.security.PublicKey

@Suppress("LongParameterList")
@CordaSerializable
class ZKVerifierTransaction internal constructor(
    override val id: SecureHash,
    val proofs: Map<ZKCommandClassName, Proof>,
    val filteredComponentGroups: List<ZKFilteredComponentGroup>,
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
        val groupHashes = getGroupHashes()

        require(groupHashes.isNotEmpty()) { "At least one component group hash is required" }
        // Verify the top level Merkle tree (group hashes are its leaves, including allOnesHash for empty list or null
        // components in WireTransaction).
        require(MerkleTree.getMerkleTree(groupHashes, digestService).hash == id) {
            "Top level Merkle tree cannot be verified against transaction's id"
        }
    }

    private fun getGroupHashes(): List<SecureHash> {
        val listOfLeaves = mutableListOf<SecureHash>()
        // Even if empty and not used, we should at least send oneHashes for each known
        // or received but unknown (thus, bigger than known ordinal) component groups.
        val allOnesHash = digestService.allOnesHash
        for (i in 0..filteredComponentGroups.maxOf { it.groupIndex }) {
            val root = filteredComponentGroups.find { it.groupIndex == i }?.merkleTree(digestService)?.hash ?: allOnesHash
            listOfLeaves.add(root)
        }
        return listOfLeaves
    }

    fun outputHashes(): List<SecureHash> =
        filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }?.allComponentHashes(digestService) ?: emptyList()

    fun privateComponentHashes(groupIndex: Int): List<SecureHash> =
        filteredComponentGroups.find { it.groupIndex == groupIndex }?.privateComponentHashes?.values?.toList() ?: emptyList()

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    @Suppress("UNUSED_PARAMETER") // TODO @Aleksei: remove suppression when function body implemented
    fun isPrivateComponent(outputsGroup: ComponentGroupEnum, index: Int): Boolean {
        return true // TODO @Aleksei: Implement based on improved PartialMerkleTree. Now always returns true, so ZKTransactionBuilder checks succeed
    }

    companion object {

        fun fromWireTransaction(wtx: WireTransaction, proofs: Map<String, ByteArray>): ZKVerifierTransaction {

            // Here we don't need to filter anything, we only create FTX to be able to access hashes (they are internal in WTX)
            val ftx = FilteredTransaction.buildFilteredTransaction(wtx) { true }

            // Filter the component groups based on visibility data from 'zkTransactionMetadata'
            val filteredComponentGroups = filterPrivateComponents(
                wtx,
                ftx.filteredComponentGroups.associate { it.groupIndex to it.nonces }
            )

            val result = ZKVerifierTransaction(
                id = wtx.id,
                proofs = proofs,
                digestService = wtx.digestService,
                filteredComponentGroups = filteredComponentGroups
            )

            return result
        }

        private fun filterPrivateComponents(
            wtx: WireTransaction,
            allWireTransactionComponentNonces: Map<Int, List<SecureHash>>,
        ): List<ZKFilteredComponentGroup> {
            val zkTransactionMetadata = wtx.zkTransactionMetadata()

            return wtx.componentGroups.map { componentGroup ->
                val groupIndex = componentGroup.groupIndex

                val visibleSerialisedComponents: MutableList<OpaqueBytes> = mutableListOf()
                val visibleComponentNonces: MutableList<SecureHash> = mutableListOf()
                val privateComponentHashes: MutableMap<Int, SecureHash> = LinkedHashMap()

                componentGroup.components.forEachIndexed { componentIndex, serialisedComponent ->
                    if (zkTransactionMetadata.shouldBeVisibleInFilteredComponentGroup(groupIndex, componentIndex)) {
                        visibleSerialisedComponents.add(serialisedComponent)
                        visibleComponentNonces.add(allWireTransactionComponentNonces[groupIndex]!![componentIndex])
                    } else {
                        privateComponentHashes[componentIndex] = componentHash(wtx, allWireTransactionComponentNonces, groupIndex, componentIndex)
                    }
                }

                ZKFilteredComponentGroup(
                    groupIndex = groupIndex,
                    components = visibleSerialisedComponents,
                    nonces = visibleComponentNonces,
                    privateComponentHashes = privateComponentHashes
                )
            }
        }

        private fun componentHash(wtx: WireTransaction, nonces: Map<Int, List<SecureHash>>, groupIndex: Int, componentIndex: Int): SecureHash {
            val componentBytes = wtx.componentGroups.first { it.groupIndex == groupIndex }.components[componentIndex]
            return wtx.digestService.componentHash(nonces[groupIndex]!![componentIndex], componentBytes)
        }
    }
}
