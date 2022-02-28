package com.ing.zinc.bfl.generator

import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BYTES_SIZE
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincConstant.Companion.zincConstant
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.util.bitsToByteBoundary
import java.util.Locale

/**
 * Configuration options specific to a certain witness group.
 *
 * Contains utilities to aid in generating deserialization methods for this witness group.
 *
 * @property name Name of the witness group. (i.e. inputs, outputs, references, ...)
 * @property type [BflType] describing the type contained in this witness group
 */
data class WitnessGroupOptions(
    val name: String,
    private val type: BflType
) {
    private val sizeInBits: Int = type.bitSize.bitsToByteBoundary()

    /**
     * [ZincConstant] for the size of this witness group.
     *
     * This utility value can be used to generate "src/consts.zn"
     */
    val witnessSizeConstant = zincConstant {
        name = "WITNESS_SIZE_${this@WitnessGroupOptions.name.toUpperCase(Locale.getDefault())}"
        type = ZincPrimitive.U24
        initialization = "$sizeInBits"
    }

    /**
     * [ZincType] of the witness for this witness group.
     *
     * This utility value can be used when generating functions/methods that take the witness group as parameter
     */
    val witnessType = zincArray {
        elementType = ZincPrimitive.Bool
        size = "$CONSTS::${witnessSizeConstant.getName()}"
    }

    /**
     * Name of the deserialize method for this witness group.
     */
    val deserializeMethodName: String = "deserialize_from_${name.camelToSnakeCase()}"

    companion object {
        fun cordaWrapped(groupName: String, stateClass: BflType): WitnessGroupOptions = WitnessGroupOptions(
            groupName,
            struct {
                name = groupName
                field {
                    name = "corda_magic_bits"
                    type = array {
                        capacity = CORDA_MAGIC_BYTES_SIZE
                        elementType = BflPrimitive.I8
                    }
                }
                field {
                    name = "state_class"
                    type = stateClass
                }
            }
        )
    }
}
