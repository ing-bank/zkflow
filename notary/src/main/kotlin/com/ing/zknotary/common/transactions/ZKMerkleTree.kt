package com.ing.zknotary.common.transactions

import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.ComponentGroup

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
     * It is possible to have the leaves of ComponentGroups use a different hash function than the nodes of the merkle trees.
     * This allows optimisation in choosing a leaf hash function that is better suited to arbitrary length inputs and a node function
     * that is suited to fixed length inputs.
     */
    val tree: MerkleTree
}

abstract class AbstractZKMerkleTree(
    val componentGroups: List<ComponentGroup>,
    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService
) : TransactionMerkleTree {

    override val root: SecureHash get() = tree.hash

    override val tree: MerkleTree by lazy { MerkleTree.getMerkleTree(groupHashes, nodeDigestService) }

    open val groupHashes: List<SecureHash> by lazy {
        val componentGroupHashes = mutableListOf<SecureHash>()
        // Even if empty and not used, we should at least send oneHashes for each known
        // or received but unknown (thus, bigger than known ordinal) component groups.
        for (i in 0..componentGroups.map { it.groupIndex }.max()!!) {
            val root = groupsMerkleRoots[i] ?: nodeDigestService.allOnesHash
            componentGroupHashes.add(root)
        }
        componentGroupHashes
    }

    val groupsMerkleRoots: Map<Int, SecureHash> by lazy {
        componentHashes.map { (groupIndex: Int, componentHashesInGroup: List<SecureHash>) ->
            groupIndex to MerkleTree.getMerkleTree(
                componentHashesInGroup,
                nodeDigestService,
                componentGroupLeafDigestService
            ).hash
        }.toMap()
    }

    abstract val componentNonces: Map<Int, List<SecureHash>>

    private val componentHashes: Map<Int, List<SecureHash>> by lazy {
        componentGroups.map { group ->
            group.groupIndex to group.components.mapIndexed { componentIndex, component ->
                componentGroupLeafDigestService.hash(componentNonces[group.groupIndex]!![componentIndex].bytes + component.bytes)
            }
        }.toMap()
    }
}
