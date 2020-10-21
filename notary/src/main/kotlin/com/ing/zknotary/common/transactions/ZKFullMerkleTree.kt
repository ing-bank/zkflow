package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
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
                            ptx.padded.inputs().map { Dactyloscopist.identify(it.content.ref) },
                        ComponentGroupEnum.OUTPUTS_GROUP to
                            ptx.padded.outputs().map { Dactyloscopist.identify(it.content) },
                        ComponentGroupEnum.REFERENCES_GROUP to
                            ptx.padded.references().map { Dactyloscopist.identify(it.content.ref) },
                        ComponentGroupEnum.COMMANDS_GROUP to
                            listOf(Dactyloscopist.identify(ptx.command.value)),
                        ComponentGroupEnum.ATTACHMENTS_GROUP to
                            ptx.padded.attachments().map { Dactyloscopist.identify(it.content) },
                        ComponentGroupEnum.NOTARY_GROUP to
                            listOf(Dactyloscopist.identify(ptx.notary)),
                        ComponentGroupEnum.TIMEWINDOW_GROUP to
                            listOf(Dactyloscopist.identify(ptx.padded.timeWindow().content)),
                        ComponentGroupEnum.PARAMETERS_GROUP to
                            listOf(Dactyloscopist.identify(ptx.padded.networkParametersHash().content)),
                        ComponentGroupEnum.SIGNERS_GROUP to
                            ptx.padded.signers().map { Dactyloscopist.identify(it.content) }
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
