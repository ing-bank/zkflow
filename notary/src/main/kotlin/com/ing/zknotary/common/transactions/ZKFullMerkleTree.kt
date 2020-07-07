package com.ing.zknotary.common.transactions

import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.ComponentGroup
import java.nio.ByteBuffer

class ZKFullMerkleTree(
    ptx: ZKProverTransaction
) : AbstractZKMerkleTree(
    createComponentGroups(ptx),
    ptx.componentGroupLeafDigestService,
    ptx.nodeDigestService
) {
    companion object {
        fun createComponentGroups(ptx: ZKProverTransaction): List<ComponentGroup> {
            val digestService = ptx.componentGroupLeafDigestService

            return mutableListOf<ComponentGroup>().apply {
                addInputsGroup(ptx.padded.inputs().map { it.content.ref })
                addReferencesGroup(ptx.padded.references().map { it.content.ref })
                addOutputsGroup(ptx.padded.outputs().map { it.content.ref })
                addCommandGroup(ptx.command.value, digestService)
                addAttachmentsGroup(ptx.attachments)
                addNotaryGroup(ptx.notary, digestService)
                addTimeWindowGroup(ptx.timeWindow, digestService)
                addNetWorkParametersHashGroup(ptx.networkParametersHash)
                addCommandSignersGroup(ptx.padded.signers().map { it.content }, digestService)
            }
        }
    }

    override val componentNonces: Map<Int, List<SecureHash>> by lazy {
        componentGroups.map { group ->
            group.groupIndex to group.components.mapIndexed { componentIndex, _ ->
                componentGroupLeafDigestService.hash(
                    ptx.privacySalt.bytes + ByteBuffer.allocate(8)
                        .putInt(group.groupIndex).putInt(componentIndex).array()
                )
            }
        }.toMap()
    }
}
