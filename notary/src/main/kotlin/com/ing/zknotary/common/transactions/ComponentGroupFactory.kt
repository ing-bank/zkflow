package com.ing.zknotary.common.transactions

import net.corda.core.serialization.serialize
import net.corda.core.transactions.ComponentGroup

class ComponentGroupsFactory {
    companion object {
        fun create(vtx: ZKVerifierTransaction): List<ComponentGroup> {
            val serializer = { value: Any, _: Int -> value.serialize(vtx.serializationFactoryService.factory) }

            return mutableListOf<ComponentGroup>().apply {
                addInputsGroup(vtx.inputs, serializer)
                addReferencesGroup(vtx.references, serializer)
                addOutputsGroup(vtx.outputs, serializer)
                addNotaryGroup(vtx.notary, serializer)
                addTimeWindowGroup(vtx.timeWindow, serializer)
                addNetWorkParametersHashGroup(vtx.networkParametersHash, serializer)
            }
        }

        fun create(ptx: ZKProverTransaction): List<ComponentGroup> {
            val serializer = { value: Any, _: Int -> value.serialize(ptx.serializationFactoryService.factory) }

            return mutableListOf<ComponentGroup>().apply {
                addInputsGroup(ptx.inputs.map { it.ref }, serializer)
                addReferencesGroup(ptx.references.map { it.ref }, serializer)
                addOutputsGroup(ptx.outputs.map { it.ref }, serializer)
                addCommandsGroup(ptx.commands, serializer)
                addAttachmentsGroup(ptx.attachments, serializer)
                addNotaryGroup(ptx.notary, serializer)
                addTimeWindowGroup(ptx.timeWindow, serializer)
                addNetWorkParametersHashGroup(ptx.networkParametersHash, serializer)
            }
        }
    }
}
