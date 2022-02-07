package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.poet.ZincArray
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroupsContainer

const val PUBLIC_INPUT = "PublicInput"

class PublicInputFactory(
    private val witnessGroupsContainer: WitnessGroupsContainer
) {
    fun create(): BflStruct {
        return struct {
            name = PUBLIC_INPUT
            (witnessGroupsContainer.witnessGroups).forEach { witnessGroup ->
                witnessGroup.generateHashesMethod?.let {
                    val groupSize = (it.getReturnType() as ZincArray).getSize()
                    field {
                        name = witnessGroup.groupName
                        type = array {
                            capacity = groupSize.toInt()
                            elementType = StandardTypes.nonceDigest
                        }
                    }
                }
            }
            isDeserializable = false
        }
    }
}
