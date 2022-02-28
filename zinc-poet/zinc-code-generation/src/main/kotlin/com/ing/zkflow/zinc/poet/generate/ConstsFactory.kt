package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE_CONSTANT_NAME
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.poet.ZincPrimitive
import java.nio.file.Path

const val BYTE_BITS = "BYTE_BITS"
const val U32_BYTES = "U32_BYTES"
const val U32_BITS = "U32_BITS"

class ConstsFactory {
    fun generateConsts(
        buildPath: Path,
        codeGenerationOptions: CodeGenerationOptions
    ) {
        buildPath.zincSourceFile("$CONSTS.zn") {
            constant {
                name = BYTE_BITS
                type = ZincPrimitive.U24
                initialization = "${Byte.SIZE_BITS} as u24"
            }
            constant {
                name = U32_BYTES
                type = ZincPrimitive.U24
                initialization = "${Int.SIZE_BYTES} as u24"
            }
            constant {
                name = U32_BITS
                type = ZincPrimitive.U24
                initialization = "$U32_BYTES * $BYTE_BITS"
            }
            constant {
                name = CORDA_MAGIC_BITS_SIZE_CONSTANT_NAME
                type = ZincPrimitive.U24
                initialization = "$CORDA_MAGIC_BITS_SIZE"
                comment = "Number of bits in Corda serialization header"
            }
            codeGenerationOptions.witnessGroupOptions.forEach {
                add(it.witnessSizeConstant)
            }
        }
    }
}
