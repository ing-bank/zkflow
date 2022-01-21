package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata

const val PUBLIC_INPUT = "PublicInput"

class PublicInputFactory(
    private val metadata: ResolvedZKCommandMetadata,
) {
    fun create(): BflStruct {
        return struct {
            name = PUBLIC_INPUT
            addComponent(Witness.INPUTS, metadata.privateInputs.size)
            addComponent(Witness.OUTPUTS, metadata.privateOutputs.size)
            addComponent(Witness.REFERENCES, metadata.privateReferences.size)
            addComponent(Witness.COMMANDS, 1)
            // addComponent(Witness.ATTACHMENTS, metadata.attachmentCount)
            addComponent(Witness.NOTARY, 1)
            addComponent(Witness.TIME_WINDOW, if (metadata.timeWindow) 1 else 0)
            addComponent(Witness.SIGNERS, metadata.numberOfSigners)
            addComponent(Witness.PARAMETERS, 1)
            addComponent(Witness.SERIALIZED_INPUT_UTXOS, metadata.privateInputs.size)
            addComponent(Witness.SERIALIZED_REFERENCE_UTXOS, metadata.privateReferences.size)
            isDeserializable = false
        }
    }

    private fun StructBuilder.addComponent(groupName: String, groupSize: Int) {
        if (groupSize > 0) {
            field {
                name = groupName
                type = array {
                    capacity = groupSize
                    elementType = StandardTypes.nonceDigest
                }
            }
        }
    }
}
