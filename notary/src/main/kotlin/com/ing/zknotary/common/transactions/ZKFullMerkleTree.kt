package com.ing.zknotary.common.transactions

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.serialize
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
            val serializer = { value: Any, _: Int -> value.serialize(ptx.serializationFactoryService.factory) }

            return mutableListOf<ComponentGroup>().apply {
                addInputsGroup(ptx.padded.inputs().map { it.content.ref }, serializer)
                addReferencesGroup(ptx.padded.references().map { it.content.ref }, serializer)
                addOutputsGroup(ptx.padded.outputs().map { it.content.ref }, serializer)
                addCommandGroup(ptx.command.value, serializer)
                addAttachmentsGroup(ptx.attachments, serializer)
                addNotaryGroup(ptx.notary, serializer)
                addTimeWindowGroup(ptx.timeWindow, serializer)
                addNetWorkParametersHashGroup(ptx.networkParametersHash, serializer)
                addCommandSignersGroup(ptx.padded.signers().map { it.content }, serializer)
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
