package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.zkp.fingerprint
import net.corda.core.contracts.ComponentGroupEnum
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
        private fun createComponentGroups(vtx: ZKVerifierTransaction): List<ComponentGroup> =
            mutableListOf<ComponentGroup>().apply {
                addGroups(
                    mapOf(
                        ComponentGroupEnum.INPUTS_GROUP to
                            vtx.padded.inputs().map { it.fingerprint },
                        ComponentGroupEnum.OUTPUTS_GROUP to
                            vtx.padded.outputs().map { it.fingerprint },
                        ComponentGroupEnum.REFERENCES_GROUP to
                            vtx.padded.references().map { it.fingerprint },
                        ComponentGroupEnum.NOTARY_GROUP to
                            listOf(vtx.notary.fingerprint),
                        ComponentGroupEnum.TIMEWINDOW_GROUP to
                            listOf(vtx.padded.timeWindow().content.fingerprint),
                        ComponentGroupEnum.PARAMETERS_GROUP to
                            listOf(vtx.padded.networkParametersHash().content.fingerprint)
                    )
                )
            }
    }

    override val groupHashes: List<SecureHash> by lazy {
        val componentGroupHashes = mutableListOf<SecureHash>()
        // For groups where we don't have a groupsMerkleRoot, we take the grouphash from the ZKProverTransaction.
        // If that is not present, we use allOnesHash, to make sure all groups are always present.
        for (i in 0..componentGroups.map { it.groupIndex }.max()!!) {
            val root = groupsMerkleRoots[i] ?: vtx.groupHashes[i] ?: nodeDigestService.allOnesHash
            componentGroupHashes.add(root)
        }
        componentGroupHashes
    }

    override val componentNonces: Map<Int, List<SecureHash>> = vtx.componentNonces

    // Override because we need to include the output hashes from the ProverTransaction unchanged
    override val componentHashes: Map<Int, List<SecureHash>> by lazy {
        val list = componentGroups.map { group ->
            group.groupIndex to group.components.mapIndexed { componentIndex, component ->
                componentGroupLeafDigestService.hash(componentNonces[group.groupIndex]!![componentIndex].bytes + component.bytes)
            }
        }.toMutableList()

        // Hack to include the hashes for the outputs as provided from the ProverTransaction
        if (vtx.outputHashes.isNotEmpty()) list.add(Pair(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, vtx.outputHashes))

        list.toMap()
    }
}
