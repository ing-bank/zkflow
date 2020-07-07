package com.ing.zknotary.common.transactions

import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.ComponentGroup

class ZKPartialMerkleTree(
    vtx: ZKVerifierTransaction
) : AbstractZKMerkleTree(
    createComponentGroups(vtx),
    vtx.componentGroupLeafDigestService,
    vtx.nodeDigestService
) {

    companion object {
        private fun createComponentGroups(vtx: ZKVerifierTransaction): List<ComponentGroup> {
            val digestService = vtx.componentGroupLeafDigestService

            return mutableListOf<ComponentGroup>().apply {
                addInputsGroup(vtx.padded.inputs())
                addReferencesGroup(vtx.padded.references())
                addOutputsGroup(vtx.padded.outputs())
                addNotaryGroup(vtx.notary, digestService)
                addTimeWindowGroup(vtx.timeWindow, digestService)
                addNetWorkParametersHashGroup(vtx.networkParametersHash)
            }
        }
    }

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
