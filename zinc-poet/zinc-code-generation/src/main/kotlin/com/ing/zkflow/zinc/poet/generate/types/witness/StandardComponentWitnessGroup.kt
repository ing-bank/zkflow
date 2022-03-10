package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflWrappedState
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import com.ing.zinc.poet.indent
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.digest
import com.ing.zkflow.zinc.poet.generate.types.Witness
import net.corda.core.contracts.ComponentGroupEnum

internal data class StandardComponentWitnessGroup(
    override val groupName: String,
    val module: BflWrappedState,
    private val groupSize: Int,
    val componentGroup: ComponentGroupEnum
) : WitnessGroup {
    override val isPresent: Boolean = groupSize > 0
    private val witnessGroupOptions = WitnessGroupOptions(groupName, module)
    override val options = listOf(witnessGroupOptions)
    override val dependencies: List<BflType> = listOf(
        witnessGroupOptions.type,
        witnessGroupOptions.type.lastField.type
    )
    override val serializedType: ZincType = zincArray {
        size = groupSize.toString()
        elementType = zincArray {
            size = "$CONSTS::${options[0].witnessSizeConstant.getName()}"
            elementType = ZincPrimitive.Bool
        }
    }

    fun generateDeserializeMethod(): ZincFunction? {
        if (groupSize == 0) return null
        val deserializeExpression = options[0].generateDeserializeExpr("self.$groupName[i]")
        val deserializedType = module.lastField.type
        return zincMethod {
            comment =
                "Deserialize $groupName from the ${Witness::class.java.simpleName}."
            name = "deserialize_$groupName"
            returnType = zincArray {
                elementType = deserializedType.toZincId()
                size = "$groupSize"
            }
            body = """
                let mut ${groupName}_array: [${deserializedType.id}; $groupSize] = [${deserializedType.defaultExpr()}; $groupSize];
                for i in 0..$groupSize {
                    ${groupName}_array[i] = ${deserializeExpression.indent(20.spaces)};
                }
                ${groupName}_array
            """.trimIndent()
        }
    }

    override val generateHashesMethod: ZincFunction = zincMethod {
        val serializedDigest = digest.getSerializedTypeDef().getType() as ZincArray
        comment = "Compute the $groupName leaf hashes."
        name = "compute_${groupName}_leaf_hashes"
        returnType = zincArray {
            elementType = digest.getSerializedTypeDef()
            size = "$groupSize"
        }
        body = """
            let mut ${groupName}_leaf_hashes: [${digest.getSerializedTypeDef().getName()}; $groupSize] = [[false; ${serializedDigest.getSize()}]; $groupSize];

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
