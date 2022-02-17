package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincEnum
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.indent
import com.ing.zkflow.util.requireNotEmpty

data class BflEnum(
    override val id: String,
    val variants: List<String>,
) : BflModule {
    init {
        variants.requireNotEmpty {
            "An Enum should have at least 1 variant"
        }
    }

    override val bitSize = Int.SIZE_BITS

    override fun typeName() = id

    override fun deserializeExpr(
        witnessGroupOptions: WitnessGroupOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String = "$id::${witnessGroupOptions.deserializeMethodName}($witnessVariable, $offset)"

    override fun defaultExpr() = "$id::${variants[0]}"

    override fun equalsExpr(self: String, other: String) = "$self == $other"

    override fun accept(visitor: TypeVisitor) {
        // NOOP
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return generateDeserializeMethods(codeGenerationOptions)
    }

    private fun generateDeserializeMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return codeGenerationOptions.witnessGroupOptions.map {
            val variantClauses = variants.mapIndexed { index: Int, variant: String ->
                "$index => $id::$variant,"
            }.joinToString("\n") { variant -> variant }
            val deserializeIndex = BflPrimitive.U32.deserializeExpr(
                witnessGroupOptions = it,
                offset = OFFSET,
                variablePrefix = SERIALIZED,
                witnessVariable = SERIALIZED
            )
            zincFunction {
                name = it.deserializeMethodName
                parameter { name = SERIALIZED; type = it.witnessType }
                parameter { name = OFFSET; type = ZincPrimitive.U24 }
                returnType = Self
                comment = """
                    Deserialize ${aOrAn()} $id from the ${it.name.capitalize()} group `$SERIALIZED`, at `$OFFSET`.
                """.trimIndent()
                body = """
                    let index = ${deserializeIndex.indent(20.spaces)};
                    assert!(index < ${variants.size} as u32, "Not a $id");
                    match index as u8 {
                        ${variantClauses.indent(24.spaces)}
                        _ => $id::${variants[0]}, // Should never happen (see assert) but here to make the compiler happy
                    }
                """.trimIndent()
            }
        }
    }

    override fun toZincType() = ZincEnum.zincEnum {
        name = id
        variants.forEachIndexed { index, variant ->
            variant {
                name = variant
                ordinal = index
            }
        }
    }

    override fun generateZincFile(
        codeGenerationOptions: CodeGenerationOptions
    ): ZincFile = ZincFile.zincFile {
        comment("$id module")
        newLine()
        mod {
            module = CONSTS
        }
        newLine()
        constant {
            name = getLengthConstant()
            type = ZincPrimitive.U24
            initialization = "$bitSize"
        }
        add(getSerializedTypeDef())
        newLine()
        add(this@BflEnum.toZincType())
        newLine()
        impl {
            name = id
            addFunctions(generateMethods(codeGenerationOptions))
            addFunctions(getRegisteredMethods())
        }
    }
}
