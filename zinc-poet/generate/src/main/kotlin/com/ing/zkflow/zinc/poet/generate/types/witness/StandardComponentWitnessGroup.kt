package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE_CONSTANT
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import com.ing.zinc.poet.indent
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.Witness
import com.ing.zkflow.zinc.poet.generate.types.WitnessGroup
import net.corda.core.contracts.ComponentGroupEnum

internal data class StandardComponentWitnessGroup(
    override val groupName: String,
    val module: BflType,
    private val groupSize: Int,
    val componentGroup: ComponentGroupEnum
) : WitnessGroup {
    override val isPresent: Boolean = groupSize > 0
    override val options = listOf(WitnessGroupOptions.cordaWrapped(groupName, module))
    override val dependencies: List<BflType> = listOfNotNull(module as? BflModule)
    override val serializedType: ZincType = ZincArray.zincArray {
        size = groupSize.toString()
        elementType = ZincArray.zincArray {
            size = "$CONSTS::${options[0].witnessSizeConstant.getName()}"
            elementType = ZincPrimitive.Bool
        }
    }

    fun generateDeserializeMethod(): ZincFunction? {
        if (groupSize == 0) return null
        val deserializeExpression = module.deserializeExpr(
            options[0],
            offset = CORDA_MAGIC_BITS_SIZE_CONSTANT,
            variablePrefix = groupName,
            witnessVariable = "self.$groupName[i]"
        )
        return ZincMethod.zincMethod {
            comment =
                "Deserialize $groupName from the ${Witness::class.java.simpleName}."
            name = "deserialize_$groupName"
            returnType = ZincArray.zincArray {
                elementType = module.toZincId()
                size = "$groupSize"
            }
            body = """
                let mut ${groupName}_array: [${module.id}; $groupSize] = [${module.defaultExpr()}; $groupSize];
                for i in 0..$groupSize {
                    ${groupName}_array[i] = ${deserializeExpression.indent(20.spaces)};
                }
                ${groupName}_array
            """.trimIndent()
        }
    }

    override val generateHashesMethod: ZincFunction = zincMethod {
        comment = "Compute the $groupName leaf hashes."
        name = "compute_${groupName}_leaf_hashes"
        returnType = ZincArray.zincArray {
            elementType = StandardTypes.nonceDigest.toZincId()
            size = "$groupSize"
        }
        body = """
            let mut ${groupName}_leaf_hashes = [[false; ${StandardTypes.nonceDigest.getLengthConstant()}]; $groupSize];
            for i in (0 as u32)..$groupSize {
                ${groupName}_leaf_hashes[i] = blake2s_multi_input(
                    $COMPUTE_NONCE(self.${Witness.PRIVACY_SALT}, ${StandardTypes.componentGroupEnum.id}::${componentGroup.name} as u32, i),
                    self.$groupName[i],
                );
            }
            ${groupName}_leaf_hashes
        """.trimIndent()
    }
}
