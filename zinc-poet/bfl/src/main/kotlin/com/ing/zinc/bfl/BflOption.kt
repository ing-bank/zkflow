package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflType.Companion.SERIALIZED_VAR
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.indent

data class BflOption(val innerType: BflType) : BflStruct(
    "${innerType.typeName()}Option",
    listOf(
        Field(hasValueFieldName, BflPrimitive.Bool),
        Field(valueFieldName, innerType)
    )
) {
    override fun generateFieldDeserialization(
        witnessGroupOptions: WitnessGroupOptions,
        field: Field,
        witnessIndex: String?
    ): String {
        return if (field.name == valueFieldName) {
            val offset = field.generateConstant("offset") + (witnessIndex?.let { " + $it" } ?: "")
            val deserializedField = field.type.deserializeExpr(
                DeserializationOptions(witnessGroupOptions, SERIALIZED_VAR, offset, field.name)
            )
            // 'has_value' is deserialized before 'value', so can be used in this block
            return """
                let ${field.name}: ${field.type.id} = {
                    if $hasValueFieldName {
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

    override fun generateFieldEquals(field: Field): String {
        return if (field.name == valueFieldName) {
            "(!self.$hasValueFieldName || ${super.generateFieldEquals(field)})"
        } else {
            super.generateFieldEquals(field)
        }
    }

    @ZincMethod(order = 50)
    @Suppress("unused")
    internal fun generateSomeMethod(codeGenerationOptions: CodeGenerationOptions): ZincFunction = ZincFunction.zincFunction {
        name = "some"
        parameter { name = valueFieldName; type = innerType.toZincId() }
        returnType = Self
        comment = """
            Construct an $id, with the given `$valueFieldName`.
        """.trimIndent()
        body = """
            Self {
                $hasValueFieldName: true,
                $valueFieldName: $valueFieldName
            }
        """.trimIndent()
    }

    fun generateSome(expr: String): String {
        return "$id::some($expr)"
    }

    companion object {
        const val hasValueFieldName = "has_value"
        const val valueFieldName = "value"
    }
}
