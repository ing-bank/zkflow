package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincInvokeable
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.indent

/**
 * A [BflWrappedTransactionComponent] is a [BflStruct], where the deserialize method will only return the last field.
 * The primary use-case for this is fields that are prefixed with Corda magic bytes, and Utxos and Commands,
 * which are also prefixed with an Integer type identifier.
 */
data class BflWrappedTransactionComponent(
    override val id: String,
    val allFields: List<BflStructField>,
) : BflStruct(
    id,
    allFields,
) {
    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincInvokeable> {
        return super.generateMethods(codeGenerationOptions) + generateDeserializeLastFieldMethods(codeGenerationOptions)
    }

    override fun generateDeserializeMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return emptyList()
    }

    override fun deserializeExpr(
        transactionComponentOptions: TransactionComponentOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String {
        throw UnsupportedOperationException("deserializeExpr not supported for ${this::class.simpleName}")
    }

    private fun generateDeserializeLastFieldMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return codeGenerationOptions.transactionComponentOptions.map {
            val lastField = fields.last()
            val offset = lastField.generateConstant(OFFSET) + " + $OFFSET"
            val lastFieldDeserialization = lastField.type.deserializeExpr(it, offset, lastField.name, SERIALIZED)
            ZincFunction.zincFunction {
                name = deserializeLastFieldName(it)
                parameter { name = SERIALIZED; type = it.witnessType }
                parameter { name = OFFSET; type = ZincPrimitive.U24 }
                returnType = lastField.type.toZincType()
                comment = """
                    Deserialize field ${lastField.name} from the ${it.name.capitalize()} group `$SERIALIZED`, at `$OFFSET`.
                """.trimIndent()
                body = """
                    ${lastFieldDeserialization.indent(20.spaces)}
                """.trimIndent()
            }
        }
    }

    private fun deserializeLastFieldName(
        witnessGroupOptions: TransactionComponentOptions,
    ) = witnessGroupOptions.deserializeMethodName
        .replace("deserialize_", "deserialize_${fields.last().name}_")

    val lastField: FieldWithParentStruct by lazy { fields.last() }

    fun deserializeLastFieldExpr(
        witnessGroupOptions: TransactionComponentOptions,
        offset: String,
        witnessGroupVariable: String
    ): String = "$id::${deserializeLastFieldName(witnessGroupOptions)}($witnessGroupVariable, $offset)"
}
