package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateRef
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.serialization.serialize
import net.corda.core.transactions.ComponentGroup
import net.corda.core.utilities.OpaqueBytes
import java.nio.ByteBuffer

class ZKFullMerkleTree(
    ptx: ZKProverTransaction
) : AbstractZKMerkleTree(buildComponentGroups(ptx), ptx.componentGroupLeafDigestService, ptx.nodeDigestService) {

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

    companion object {
        fun buildComponentGroups(ptx: ZKProverTransaction): List<ComponentGroup> {
            // construct the componentGroups based on our custom serialization
            return buildComponentGroups(
                ptx.inputs.map { it.ref },
                ptx.outputs.map { it.ref },
                ptx.commands,
                ptx.attachments,
                ptx.notary,
                ptx.timeWindow,
                ptx.references.map { it.ref },
                ptx.networkParametersHash,
                ptx.serializationFactoryService
            )
        }

        private fun buildComponentGroups(
            inputs: List<ZKStateRef>,
            outputs: List<ZKStateRef>,
            commands: List<Command<*>>,
            attachments: List<SecureHash>,
            notary: Party?,
            timeWindow: TimeWindow?,
            references: List<ZKStateRef>,
            networkParametersHash: SecureHash?,
            serializationFactoryService: SerializationFactoryService
        ): List<ComponentGroup> {
            val serialize = { value: Any, _: Int -> value.serialize(serializationFactoryService.factory) }

            val componentGroupMap: MutableList<ComponentGroup> = mutableListOf()
            if (inputs.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.INPUTS_GROUP.ordinal,
                    inputs.lazyMapped(serialize)
                )
            )
            if (references.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.REFERENCES_GROUP.ordinal,
                    references.lazyMapped(serialize)
                )
            )
            if (outputs.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.OUTPUTS_GROUP.ordinal,
                    outputs.lazyMapped(serialize)
                )
            )
            // Adding commandData only to the commands group. Signers are added in their own group.
            if (commands.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.COMMANDS_GROUP.ordinal,
                    commands.map { it.value }.lazyMapped(serialize)
                )
            )
            if (attachments.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal,
                    attachments.lazyMapped(serialize)
                )
            )
            if (notary != null) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.NOTARY_GROUP.ordinal,
                    listOf(notary).lazyMapped(serialize)
                )
            )
            if (timeWindow != null) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
                    listOf(timeWindow).lazyMapped(serialize)
                )
            )
            // Adding signers to their own group. This is required for command visibility purposes: a party receiving
            // a FilteredTransaction can now verify it sees all the commands it should sign.
            if (commands.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.SIGNERS_GROUP.ordinal,
                    commands.map { it.signers }.lazyMapped(serialize)
                )
            )
            if (networkParametersHash != null) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
                    listOf(networkParametersHash).lazyMapped(serialize)
                )
            )
            return componentGroupMap
        }
    }
}
