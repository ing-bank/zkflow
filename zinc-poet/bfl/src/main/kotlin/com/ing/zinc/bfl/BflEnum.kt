package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflType.Companion.BITS_PER_BYTE
import com.ing.zinc.bfl.BflType.Companion.BYTES_PER_INT
import com.ing.zinc.bfl.BflType.Companion.SERIALIZED_VAR
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincEnum
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincTypeDef
import com.ing.zinc.poet.indent

data class BflEnum(
    override val id: String,
    val fields: List<String>,
    override val customMethods: List<ZincFunction>
) : BflType, BflModule {
    init {
        require(fields.isNotEmpty()) {
            "An Enum should have at least 1 element."
        }
    }

    override val bitSize = BYTES_PER_INT * BITS_PER_BYTE

    override fun typeName() = id

    override fun deserializeExpr(options: DeserializationOptions): String {
        return options.deserializeModule(this)
    }

    override fun defaultExpr() = "$id::${fields[0]}"

    override fun equalsExpr(self: String, other: String) = "$self == $other"

    override fun accept(visitor: TypeVisitor) {
        // NOOP
    }

    @ZincMethodList(order = 100)
    @Suppress("unused")
    fun generateDeserializeMethod(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return codeGenerationOptions.witnessGroupOptions.map {
            val fieldClauses = fields.mapIndexed { index: Int, field: String ->
                "$index => $id::$field,"
            }.joinToString("\n") { it }
            val deserializeIndex = BflPrimitive.U32.deserializeExpr(
                DeserializationOptions(it, SERIALIZED_VAR, "offset", SERIALIZED_VAR)
            )
            zincFunction {
                name = it.deserializeMethodName
                parameter { name = SERIALIZED_VAR; type = it.witnessType }
                parameter { name = "offset"; type = ZincPrimitive.U24 }
                returnType = Self
                comment = """
                    Deserialize ${aOrAn()} $id from the ${it.witnessGroupName.capitalize()} group `$SERIALIZED_VAR`, at `offset`.
                """.trimIndent()
                body = """
                    let index = ${deserializeIndex.indent(20.spaces)};
                    assert!(index < ${fields.size} as u32, "Not a $id");
                    match index as u8 {
                        ${fieldClauses.indent(24.spaces)}
                        _ => $id::${fields[0]}, // Should never happen (see assert) but here to make the compiler happy
                    }
                """.trimIndent()
            }
        }
    }

    override val serializedTypeDef: ZincTypeDef = ZincTypeDef.zincTypeDef {
        name = "Serialized$id"
        type = zincArray {
            elementType = ZincPrimitive.Bool
            size = getLengthConstant()
        }
    }

    private val enumDef: ZincEnum = ZincEnum.zincEnum {
        name = id
        fields.forEachIndexed { index, field ->
            variant {
                name = field
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
            module = "consts"
        }
        newLine()
        constant {
            name = getLengthConstant()
            type = ZincPrimitive.U24
            initialization = "$bitSize"
        }
        add(serializedTypeDef)
        newLine()
        add(enumDef)
        newLine()
        impl {
            name = id
            addFunctions(this@BflEnum.getAllMethods(codeGenerationOptions).toList())
        }
    }
}
