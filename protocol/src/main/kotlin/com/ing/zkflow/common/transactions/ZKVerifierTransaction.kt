package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.transactions.ZKFilteredComponentGroup.Companion.fromComponentGroup
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
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

    /**
     * Convenience interface to get output hashes, that are often used in the protocol.
     * We cannot use lazy val here because delegated properties cannot be transient and we don't want to serialize them
     */
    fun outputHashes(): List<SecureHash> =
        filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }?.allComponentHashes(digestService) ?: emptyList()

    /**
     * Convenience interface to get private component hashes, to be used in the public input generation
     * We cannot use lazy val here because delegated properties cannot be transient and we don't want to serialize them
     */
    fun privateComponentHashes(groupIndex: Int): List<SecureHash> =
        filteredComponentGroups.find { it.groupIndex == groupIndex }?.privateComponentHashes?.values?.toList() ?: emptyList()

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    fun isPrivateComponent(group: ComponentGroupEnum, index: Int): Boolean =
        filteredComponentGroups.find { it.groupIndex == group.ordinal }?.privateComponentHashes?.contains(index) ?: false

    companion object {

        fun fromWireTransaction(wtx: WireTransaction, proofs: Map<String, ByteArray>): ZKVerifierTransaction {
            return ZKVerifierTransaction(
                id = wtx.id,
                proofs = proofs,
                digestService = wtx.digestService,
                filteredComponentGroups = filterPrivateComponents(wtx)
            )
        }

        /**
         * Filters the component groups based on visibility data from 'zkTransactionMetadata'
         */
        private fun filterPrivateComponents(wtx: WireTransaction): List<ZKFilteredComponentGroup> {

            // Here we don't need to filter anything, we only create FTX to be able to access hashes (they are internal in WTX)
            val ftx = FilteredTransaction.buildFilteredTransaction(wtx) { true }

            return wtx.componentGroups.map { componentGroup ->
                fromComponentGroup(
                    componentGroup,
                    ftx.filteredComponentGroups.find { it.groupIndex == componentGroup.groupIndex }!!,
                    wtx.zkTransactionMetadataOrNull()
                )
            }
        }
    }
}
