package com.ing.zknotary.common.transactions

import net.corda.core.crypto.SecureHash

class ZKPartialMerkleTree(
    vtx: ZKVerifierTransaction
) : AbstractZKMerkleTree(ComponentGroupsFactory.create(vtx), vtx.componentGroupLeafDigestService, vtx.nodeDigestService) {
    override val groupHashes: List<SecureHash> by lazy {
        val componentGroupHashes = mutableListOf<SecureHash>()
        // For groups where we don't have a groupsMerkleRoot, we take the grouphash from the ZKProverTransaction.
        for (i in 0..componentGroups.map { it.groupIndex }.max()!!) {
            val root = groupsMerkleRoots[i] ?: vtx.groupHashes[i]
            componentGroupHashes.add(root)
        }
        componentGroupHashes
    }

    override val componentNonces: Map<Int, List<SecureHash>> = vtx.componentNonces
}
