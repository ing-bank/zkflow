package com.ing.zinc.bfl.generator

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflType.Companion.BITS_PER_BYTE
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincConstant.Companion.zincConstant
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import com.ing.zinc.toByteBoundary

data class WitnessGroupOptions(
    val witnessGroupName: String,
    private val witnessGroupSize: Int,
    private val witnessGroupSizeString: String
) {
    constructor(groupName: String, module: BflModule) : this(
        groupName,
        module.bitSize.toByteBoundary(),
        "${module.bitSize.toByteBoundary()}"
    )

    init {
        require(witnessGroupSize % BITS_PER_BYTE == 0) {
            "witnessGroupSize MUST be a multiple of $BITS_PER_BYTE"
        }
    }

    val witnessType: ZincType by lazy {
        zincArray {
            elementType = ZincPrimitive.Bool
            size = "consts::${witnessSizeConstant.getName()}"
        }
    }

    val witnessSizeConstant: ZincConstant by lazy {
        zincConstant {
            name = "WITNESS_SIZE_${witnessGroupName.toUpperCase()}_GROUP"
            type = ZincPrimitive.U24
            initialization = witnessGroupSizeString
        }
    }

    val deserializeMethodName: String = "deserializeFrom${witnessGroupName.capitalize()}Group"
}
