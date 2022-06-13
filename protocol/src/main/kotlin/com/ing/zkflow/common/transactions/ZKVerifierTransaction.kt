package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import java.security.PublicKey

@Suppress("LongParameterList")
@CordaSerializable
open class ZKVerifierTransaction internal constructor(
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
                commandKeys + notary!!.owningKey
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
    fun verifyMerkleTree() {
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

    /**
     * Convenience interface to get output hashes, that are often used in the protocol.
     * We cannot use lazy val here because delegated properties cannot be transient and we don't want to serialize them
     */
    fun outputHashes(): List<SecureHash> =
        filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }?.allComponentHashes(digestService) ?: emptyList()

    /**
     * Convenience function to get component hashes, to be used in the public input generation
     */
    fun visibleInWitnessComponentHashes(
        commandMetadata: ResolvedZKCommandMetadata,
        group: ComponentGroupEnum,
    ): List<SecureHash> {
        return filteredComponentGroups
            .find { it.groupIndex == group.ordinal }
            ?.allComponentHashes(digestService)
            ?.filterIndexed { index, _ -> commandMetadata.isVisibleInWitness(group.ordinal, index) }
            ?: emptyList()
    }

    /**
     * Convenience interface to get private component hashes, to be used in the public input generation
     * We cannot use lazy val here because delegated properties cannot be transient and we don't want to serialize them
     */
    fun privateComponentHashes(group: ComponentGroupEnum): List<SecureHash> = privateComponents(group).values.toList()

    /**
     * Convenience interface to get private component indexes, to be used in the public input generation
     * We cannot use lazy val here because delegated properties cannot be transient and we don't want to serialize them
     */
    fun privateComponentIndexes(group: ComponentGroupEnum): Set<Int> = privateComponents(group).keys

    /**
     * Convenience interface to get private components, to be used in the public input generation
     * We cannot use lazy val here because delegated properties cannot be transient and we don't want to serialize them
     */
    private fun privateComponents(group: ComponentGroupEnum): Map<Int, SecureHash> =
        filteredComponentGroups.find { it.groupIndex == group.ordinal }?.privateComponentHashes ?: emptyMap()

    /**
     * Returns a map of the public components with their original indexes in the original WireTransaction.
     * This map's entries are in the order in which the components appear in the transaction, meaning that
     * the indexes of the map entries can be used to match them with the visible components in the ZKVerifierTransaction.
     * See [publicComponentIndexesToOriginalWtxIndexes].
     */
    fun publicComponents(group: ComponentGroupEnum): Map<Int, OpaqueBytes> =
        filteredComponentGroups.find { it.groupIndex == group.ordinal }?.publicComponents() ?: emptyMap()

    /**
     * A map between the component's current index in the [ZKVerifierTransaction] after filtering of the private components,
     * and its original index in the [WireTransaction] that this [ZKVerifierTransaction] was built from.
     */
    fun publicComponentIndexesToOriginalWtxIndexes(group: ComponentGroupEnum): Map<Int, Int> =
        publicComponents(group).entries
            .mapIndexed { publicComponentIndex, (originalWtxIndex, _) -> publicComponentIndex to originalWtxIndex }
            .toMap()

    /**
     * Given the current index in the [ZKVerifierTransaction] after filtering of the private components,
     * returns its original index in the [WireTransaction] that this [ZKVerifierTransaction] was built from.
     */
    fun publicComponentIndexToOriginalWtxIndex(group: ComponentGroupEnum, componentIndex: Int): Int =
        publicComponentIndexesToOriginalWtxIndexes(group)[componentIndex]
            ?: error("No component at index $componentIndex in group $group")

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    fun isPrivateComponent(group: ComponentGroupEnum, index: Int): Boolean =
        filteredComponentGroups.find { it.groupIndex == group.ordinal }?.privateComponentHashes?.contains(index) ?: false

    companion object {

        /**
         * Normally we create VTX from WTX while building new transaction,
         * in this case we almost always just use system class loader
         */
        fun fromWireTransaction(wtx: WireTransaction, proofs: Map<String, ByteArray>): ZKVerifierTransaction {
            val metadata = wtx.zkTransactionMetadataOrNull(ClassLoader.getSystemClassLoader())

            return ZKVerifierTransaction(
                id = wtx.id,
                proofs = proofs,
                digestService = wtx.digestService,
                filteredComponentGroups = filterPrivateComponents(wtx, metadata)
            )
        }

        /**
         * Sometimes we want to use ClassLoader from attachments
         */
        fun fromWireTransaction(wtx: WireTransaction, proofs: Map<String, ByteArray>, services: ServiceHub): ZKVerifierTransaction {
            val metadata = wtx.zkTransactionMetadataOrNull(services)

            return ZKVerifierTransaction(
                id = wtx.id,
                proofs = proofs,
                digestService = wtx.digestService,
                filteredComponentGroups = filterPrivateComponents(wtx, metadata)
            )
        }

        /**
         * Filters the component groups based on visibility data from 'zkTransactionMetadata'
         */
        private fun filterPrivateComponents(wtx: WireTransaction, metadata: ResolvedZKTransactionMetadata?): List<ZKFilteredComponentGroup> {
            // Here we don't need to filter anything, we only create FTX to be able to access hashes (they are internal in WTX)
            val ftx = FilteredTransaction.buildFilteredTransaction(wtx) { true }

            return wtx.componentGroups.map { componentGroup ->
                ZKFilteredComponentGroup.fromComponentGroup(
                    componentGroup,
                    ftx.filteredComponentGroups.find { it.groupIndex == componentGroup.groupIndex }!!,
                    metadata
                )
            }
        }
    }
}

class ZKVerifierTransactionWithoutProofs internal constructor(
    id: SecureHash,
    filteredComponentGroups: List<ZKFilteredComponentGroup>,
    digestService: DigestService
) : ZKVerifierTransaction(id, emptyMap(), filteredComponentGroups, digestService) {

    companion object {
        fun fromWireTransaction(wtx: WireTransaction): ZKVerifierTransactionWithoutProofs {
            val zkvtx = fromWireTransaction(wtx, emptyMap())
            return ZKVerifierTransactionWithoutProofs(zkvtx.id, zkvtx.filteredComponentGroups, zkvtx.digestService)
        }
    }
}
