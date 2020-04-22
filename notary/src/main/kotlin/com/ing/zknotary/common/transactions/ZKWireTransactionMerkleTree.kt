package com.ing.zknotary.common.transactions

import java.nio.ByteBuffer
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.ComponentGroup
import net.corda.core.utilities.OpaqueBytes

interface TransactionMerkleTree {
    val root: SecureHash

    /**
     * The full Merkle tree for a transaction.
     * Each transaction component group has its own sub Merkle tree.
     * All of the roots of these trees are used as leaves in the top level Merkle tree.
     *
     * Note that ordering of elements inside a [ComponentGroup] matters when computing the Merkle root.
     * On the other hand, insertion group ordering does not affect the top level Merkle tree construction, as it is
     * actually an ordered Merkle tree, where its leaves are ordered based on the group ordinal in [ComponentGroupEnum].
     * If any of the groups is an empty list or a null object, then [SecureHash.allOnesHash] is used as its hash.
     * Also, [privacySalt] is not a Merkle tree leaf, because it is already "inherently" included via the component nonces.
     *
     * It is possible to have the leafs of ComponentGroups use a different hash function than the nodes of the merkle trees.
     * This allows optimisation in choosing a leaf hash function that is better suited to arbitrary length inputs and a node function
     * that is suited to fixed length inputs.
     */
    val tree: MerkleTree
}

class ZKWireTransactionMerkleTree(
    zkwtx: ZKWireTransaction,
    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService
) : TransactionMerkleTree {
    private val componentGroups: List<ComponentGroup> = zkwtx.wtx.componentGroups
    private val privacySalt: PrivacySalt = zkwtx.wtx.privacySalt

    constructor(wtx: ZKWireTransaction, digestService: DigestService) : this(wtx, digestService, digestService)

    override val root: SecureHash get() = tree.hash

    override val tree: MerkleTree by lazy { MerkleTree.getMerkleTree(groupHashes, nodeDigestService) }

    /**
     * For each component group: the root hashes of the sub Merkle tree for that component group
     *
     * If a group's Merkle root is allOnesHash, it is a flag that denotes this group is empty (if list) or null (if single object)
     * in the wire transaction.
     */
    internal val groupHashes: List<SecureHash> by lazy {
        val componentGroupHashes = mutableListOf<SecureHash>()
        // Even if empty and not used, we should at least send oneHashes for each known
        // or received but unknown (thus, bigger than known ordinal) component groups.
        for (i in 0..componentGroups.map { it.groupIndex }.max()!!) {
            val root = groupsMerkleRoots[i] ?: nodeDigestService.allOnesHash
            componentGroupHashes.add(root)
        }
        componentGroupHashes
    }

    /**
     * Calculate the root hashes of the component groups that are used to build the transaction's Merkle tree.
     * Each group has its own sub Merkle tree and the hash of the root of this sub tree works as a leaf of the top
     * level Merkle tree. The root of the latter is the transaction identifier.
     */
    private val groupsMerkleRoots: Map<Int, SecureHash> by lazy {
        componentHashes.map { (groupIndex: Int, componentHashesInGroup: List<SecureHash>) ->
            groupIndex to MerkleTree.getMerkleTree(componentHashesInGroup, nodeDigestService, componentGroupLeafDigestService).hash
        }.toMap()
    }

    /**
     * Nonces for every transaction component in [componentGroups], including new fields (due to backwards compatibility support) we cannot process.
     * Nonce are computed in the following way:
     * nonce1 = H(salt || path_for_1st_component)
     * nonce2 = H(salt || path_for_2nd_component)
     * etc.
     * Thus, all of the nonces are "independent" in the sense that knowing one or some of them, you can learn nothing about the rest.
     */
    private val componentNonces: Map<Int, List<SecureHash>> by lazy {
        componentGroups.map { group ->
            group.groupIndex to group.components.mapIndexed { componentIndex, _ ->
                computeNonce(privacySalt, group.groupIndex, componentIndex)
            }
        }.toMap()
    }

    /**
     * The hash for every transaction component, per component group. These will be used to build the full Merkle tree.
     */
    private val componentHashes: Map<Int, List<SecureHash>> by lazy {
        componentGroups.map { group ->
            group.groupIndex to group.components.mapIndexed { componentIndex, component ->
                computeHash(componentNonces[group.groupIndex]!![componentIndex], component)
            }
        }.toMap()
    }

    private fun computeHash(nonce: SecureHash, opaqueBytes: OpaqueBytes): SecureHash =
        componentGroupLeafDigestService.hash(nonce.bytes + opaqueBytes.bytes)

    /**
     * Method to compute a nonce based on privacySalt, component group index and component internal index.
     * @param privacySalt a [PrivacySalt].
     * @param groupIndex the fixed index (ordinal) of this component group.
     * @param internalIndex the internal index of this object in its corresponding components list.
     * @return H(privacySalt || groupIndex || internalIndex))
     */
    private fun computeNonce(privacySalt: PrivacySalt, groupIndex: Int, internalIndex: Int) = componentGroupLeafDigestService.hash(privacySalt.bytes + ByteBuffer.allocate(8)
            .putInt(groupIndex).putInt(internalIndex).array())
}
