package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.dactyloscopy.fingerprint
import net.corda.core.contracts.ComponentGroupEnum
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
        fun createComponentGroups(ptx: ZKProverTransaction): List<ComponentGroup> =
            mutableListOf<ComponentGroup>().apply {
                addGroups(
                    mapOf(
                        ComponentGroupEnum.INPUTS_GROUP to
                            ptx.padded.inputs().map { it.content.ref.fingerprint() },
                        ComponentGroupEnum.OUTPUTS_GROUP to
                            ptx.padded.outputs().map { it.content.fingerprint() },
                        ComponentGroupEnum.REFERENCES_GROUP to
                            ptx.padded.references().map { it.content.ref.fingerprint() },
                        ComponentGroupEnum.COMMANDS_GROUP to
                            listOf(ptx.command.value.fingerprint()),
                        ComponentGroupEnum.ATTACHMENTS_GROUP to
                            ptx.padded.attachments().map { it.content.fingerprint() },
                        ComponentGroupEnum.NOTARY_GROUP to
                            listOf(ptx.notary.fingerprint()),
                        ComponentGroupEnum.TIMEWINDOW_GROUP to
                            listOf(ptx.padded.timeWindow().content.fingerprint()),
                        ComponentGroupEnum.PARAMETERS_GROUP to
                            listOf(ptx.padded.networkParametersHash().content.fingerprint()),
                        ComponentGroupEnum.SIGNERS_GROUP to
                            ptx.padded.signers().map { it.content.fingerprint() }
                    )
                )
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
