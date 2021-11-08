package com.ing.zinc.bfl.generator

import com.ing.zinc.bfl.BflType
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincConstant.Companion.zincConstant
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.util.bitsToByteBoundary

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
     * [ZincType] of the witness for this witness group.
     *
     * This utility value can be used when generating functions/methods that take the witness group as parameter
     */
    val witnessType: ZincType by lazy {
        zincArray {
            elementType = ZincPrimitive.Bool
            size = "consts::${witnessSizeConstant.getName()}"
        }
    }

    /**
     * [ZincConstant] for the size of this witness group.
     *
     * This utility value can be used to generate "src/consts.zn"
     */
    val witnessSizeConstant: ZincConstant by lazy {
        zincConstant {
            name = "WITNESS_SIZE_${this@WitnessGroupOptions.name.toUpperCase()}_GROUP"
            type = ZincPrimitive.U24
            initialization = "$sizeInBits"
        }
    }

    /**
     * Name of the deserialize method for this witness group.
     */
    val deserializeMethodName: String = "deserializeFrom${name.capitalize()}Group"
}
