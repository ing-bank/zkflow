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

    /**
     * This value will contain as many hashes as there are component groups,
     * otherwise fail.
     * Order of the elements corresponds to the order groups listed in ComponentGroupEnum.
     */
    open val groupHashes: List<SecureHash> by lazy {
        ComponentGroupEnum.values().map {
            groupsMerkleRoots[it.ordinal] ?: SecureHash.allOnesHash
        }
    }

    val groupsMerkleRoots: Map<Int, SecureHash> by lazy {
        componentHashes.map { (groupIndex: Int, componentHashesInGroup: List<SecureHash>) ->
            val root = MerkleTree.getMerkleTree(
                componentHashesInGroup,
                nodeDigestService,
                componentGroupLeafDigestService
            ).hash
            groupIndex to root
        }.toMap()
    }

    abstract val componentNonces: Map<Int, List<SecureHash>>

    open val componentHashes: Map<Int, List<SecureHash>> by lazy {
        componentGroups.map { group ->
            group.groupIndex to group.components.mapIndexed { componentIndex, component ->
                componentGroupLeafDigestService.hash(componentNonces[group.groupIndex]!![componentIndex].bytes + component.bytes)
            }
                // To prevent Corda's automatic promotion of a single leaf to the Merkle root,
                // ensure, there are at least 2 elements.
                // See, https://github.com/corda/corda/issues/6680
                .pad(2, componentGroupLeafDigestService.zeroHash)
        }.toMap()
    }
}
