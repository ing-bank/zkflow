package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.indent

data class BflOption(val innerType: BflType) : BflStruct(
    "${innerType.typeName()}Option",
    listOf(
        Field(HAS_VALUE_FIELD, BflPrimitive.Bool),
        Field(VALUE_FIELD, innerType)
    )
) {
    override fun generateFieldDeserialization(
        witnessGroupOptions: WitnessGroupOptions,
        field: FieldWithParentStruct,
        witnessIndex: String
    ): String {
        return if (field.name == VALUE_FIELD) {
            val offset = field.generateConstant(OFFSET) + " + $witnessIndex"
            val deserializedField = field.type.deserializeExpr(witnessGroupOptions, offset, field.name)
            // 'has_value' is deserialized before 'value', so can be used in this block
            return """
                let ${field.name}: ${field.type.id} = {
                    if $HAS_VALUE_FIELD {
                        ${deserializedField.indent(24.spaces)}
                    } else {
                        ${field.type.defaultExpr().indent(24.spaces)}
                    }
                };
            """.trimIndent()
        } else {
            super.generateFieldDeserialization(witnessGroupOptions, field, witnessIndex)
        }
    }

    override fun generateFieldEquals(field: FieldWithParentStruct): String {
        return if (field.name == VALUE_FIELD) {
            "(!self.$HAS_VALUE_FIELD || ${super.generateFieldEquals(field)})"
        } else {
            super.generateFieldEquals(field)
        }
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return super.generateMethods(codeGenerationOptions) +
            listOf(
                generateSomeMethod()
            )
    }

    internal fun generateSomeMethod(): ZincFunction = ZincFunction.zincFunction {
        name = "some"
        parameter { name = VALUE_FIELD; type = innerType.toZincId() }
        returnType = Self
        comment = """
            Construct an $id, with the given `$VALUE_FIELD`.
        """.trimIndent()
        body = """
            Self {
                $HAS_VALUE_FIELD: true,
                $VALUE_FIELD: $VALUE_FIELD
            }
        """.trimIndent()
    }

    fun generateSome(expr: String): String {
        return "$id::some($expr)"
    }

    companion object {
        const val HAS_VALUE_FIELD = "has_value"
        const val VALUE_FIELD = "value"
    }
}
