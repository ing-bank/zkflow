package com.ing.zknotary.common.transactions

import net.corda.core.crypto.SecureHash
import java.nio.ByteBuffer

class ZKFullMerkleTree(
    ptx: ZKProverTransaction
) : AbstractZKMerkleTree(
    ComponentGroupsFactory.create(ptx),
    ptx.componentGroupLeafDigestService,
    ptx.nodeDigestService
) {

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
