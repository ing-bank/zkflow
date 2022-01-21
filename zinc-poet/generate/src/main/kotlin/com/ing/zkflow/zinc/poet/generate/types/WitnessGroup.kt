package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE_CONSTANT
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.indent
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.nonceDigest
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PRIVACY_SALT
import net.corda.core.contracts.ComponentGroupEnum

internal data class WitnessGroup(
    val name: String,
    val module: BflType,
    val groupSize: Int,
    val componentGroup: ComponentGroupEnum
) {
    val witnessGroupOptions: WitnessGroupOptions = WitnessGroupOptions.cordaWrapped(name, module)

    fun arrayOfSerializedData(): ZincArray {
        return zincArray {
            size = groupSize.toString()
            elementType = zincArray {
                size = "$CONSTS::${witnessGroupOptions.witnessSizeConstant.getName()}"
                elementType = ZincPrimitive.Bool
            }
        }
    }

    fun generateDeserializeMethod(): ZincFunction? {
        if (groupSize == 0) return null
        val deserializeExpression = module.deserializeExpr(
            witnessGroupOptions,
            offset = CORDA_MAGIC_BITS_SIZE_CONSTANT,
            variablePrefix = name,
            witnessVariable = "self.$name[i]"
        )
        return zincMethod {
            comment = "Deserialize ${this@WitnessGroup.name} from the ${Witness::class.java.simpleName}."
            name = "deserialize_${this@WitnessGroup.name}"
            returnType = zincArray {
                elementType = module.toZincId()
                size = "$groupSize"
            }
            body = """
                let mut ${this@WitnessGroup.name}_array: [${module.id}; $groupSize] = [${module.defaultExpr()}; $groupSize];
                for i in 0..$groupSize {
                    ${this@WitnessGroup.name}_array[i] = ${deserializeExpression.indent(24.spaces)};
                }
                ${this@WitnessGroup.name}_array
            """.trimIndent()
        }
    }

    fun generateHashesMethod(): ZincFunction? {
        if (groupSize == 0) return null
        return zincMethod {
            comment = "Compute the ${this@WitnessGroup.name} leaf hashes."
            name = "compute_${this@WitnessGroup.name}_leaf_hashes"
            returnType = zincArray {
                elementType = nonceDigest.toZincId()
                size = "$groupSize"
            }
            body = """
                let mut ${this@WitnessGroup.name}_leaf_hashes = [[false; ${nonceDigest.getLengthConstant()}]; $groupSize];
                for i in (0 as u32)..$groupSize {
                    ${this@WitnessGroup.name}_leaf_hashes[i] = blake2s_multi_input(
                        $COMPUTE_NONCE(self.$PRIVACY_SALT, ${componentGroupEnum.id}::${componentGroup.name} as u32, i),
                        self.${this@WitnessGroup.name}[i],
                    );
                }
                ${this@WitnessGroup.name}_leaf_hashes
            """.trimIndent()
        }
    }
}
