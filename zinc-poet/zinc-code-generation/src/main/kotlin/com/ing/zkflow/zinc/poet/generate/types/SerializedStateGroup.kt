package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE_CONSTANT
import com.ing.zinc.bfl.TypeVisitor
import com.ing.zinc.bfl.dsl.FieldBuilder.Companion.field
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.bfl.mod
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.bfl.use
import com.ing.zinc.bfl.useLengthConstant
import com.ing.zinc.bfl.useSerialized
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincStruct.Companion.zincStruct
import com.ing.zinc.poet.indent
import com.ing.zkflow.util.bitsToByteBoundary
import com.ing.zkflow.util.requireNotNull
import com.ing.zkflow.zinc.poet.generate.COMPUTE_LEAF_HASHES
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.COMPUTE_UTXO_HASHES
import com.ing.zkflow.zinc.poet.generate.CRYPTO_UTILS
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.digest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt

@Suppress("TooManyFunctions")
data class SerializedStateGroup(
    private val groupName: String,
    private val baseName: String,
    private val transactionStates: List<IndexedState>,
) : BflModule {
    private val serializedStructName: String = "Serialized$baseName"
    internal val deserializedStruct = struct {
        name = "Deserialized$baseName"
        addFields(transactionStates.toFieldList())
        isDeserializable = false
    }

    private val groupSize = transactionStates.size

    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions) = zincFile {
        mod { module = CONSTS }
        newLine()
        transactionStates
            .map { it.state }
            .distinctBy { it.id }
            .forEach {
                add(it.mod())
                add(it.use())
                add(it.useLengthConstant())
                newLine()
            }
        add(deserializedStruct.mod())
        add(deserializedStruct.use())
        newLine()
        listOf(privacySalt, digest).forEach {
            add(it.mod())
            add(it.use())
            add(it.useSerialized())
            add(it.useLengthConstant())
            newLine()
        }
        mod { module = CRYPTO_UTILS }
        use { path = "$CRYPTO_UTILS::$COMPUTE_NONCE" }
        use { path = "std::crypto::blake2s_multi_input" }
        newLine()
        add(toZincType())
        newLine()
        impl {
            name = serializedStructName
            addFunctions(generateMethods(codeGenerationOptions))
        }
    }

    override fun toZincType() = zincStruct {
        name = serializedStructName
        transactionStates.forEach {
            val paddingBits = it.state.bitSize.bitsToByteBoundary() - it.state.bitSize
            field {
                name = it.fieldName
                type = zincArray {
                    size = getSerializedBitSize(paddingBits, it.state)
                    elementType = ZincPrimitive.Bool
                }
            }
        }
    }

    private fun getSerializedBitSize(paddingBits: Int, stateType: BflModule) =
        if (paddingBits > 0) {
            "$CORDA_MAGIC_BITS_SIZE_CONSTANT + ${stateType.getLengthConstant()} + $paddingBits as u24"
        } else {
            "$CORDA_MAGIC_BITS_SIZE_CONSTANT + ${stateType.getLengthConstant()}"
        }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return listOf(
            generateDeserializeMethod(codeGenerationOptions),
            generateEmptyMethod(),
            generateEqualsMethod(),
            generateComputeHashes(),
        )
    }

    private fun generateDeserializeMethod(codeGenerationOptions: CodeGenerationOptions) = zincMethod {
        name = "deserialize"
        returnType = deserializedStruct.toZincId()
        val fieldDeserializations = transactionStates.joinToString("\n") {
            val stateTypeId = it.state.id
            val fieldName = it.fieldName
            val witnessGroupOptions = codeGenerationOptions.witnessGroupOptions.find { witnessGroupOptions ->
                "${groupName}_$fieldName".startsWith(witnessGroupOptions.name)
            }.requireNotNull {
                "Could not select Witness group options in group $groupName for state ${it.state.id}"
            }
            val deserializeMethodName = witnessGroupOptions.deserializeMethodName
            "$fieldName: $stateTypeId::$deserializeMethodName(self.$fieldName, $CORDA_MAGIC_BITS_SIZE_CONSTANT),"
        }
        body = """
            ${deserializedStruct.id} {
                ${fieldDeserializations.indent(16.spaces)}
            }
        """.trimIndent()
    }

    private fun generateEmptyMethod() = zincFunction {
        val fieldInitializations = transactionStates.joinToString("\n") {
            val paddingBits = it.state.bitSize.bitsToByteBoundary() - it.state.bitSize
            "${it.fieldName}: [false; ${getSerializedBitSize(paddingBits, it.state)}],"
        }
        name = "empty"
        returnType = this@SerializedStateGroup.toZincId()
        body = """
            ${this@SerializedStateGroup.id} {
                ${fieldInitializations.indent(16.spaces)}
            }
        """.trimIndent()
    }

    private fun generateEqualsMethod() = zincMethod {
        val fieldEquals = transactionStates.joinToString(" && ") {
            val stateFieldName = it.fieldName
            """
                {
                    let mut still_equals: bool = true;
                    for i in 0..${it.state.getLengthConstant()} while still_equals {
                        still_equals = self.$stateFieldName[i + $CORDA_MAGIC_BITS_SIZE_CONSTANT] == other.$stateFieldName[i + $CORDA_MAGIC_BITS_SIZE_CONSTANT];
                    }
                    still_equals
                }
            """.trimIndent()
        }
        name = "equals"
        parameter { name = "other"; type = this@SerializedStateGroup.toZincId() }
        returnType = ZincPrimitive.Bool
        body = """
            ${fieldEquals.indent(12.spaces)}
        """.trimIndent()
    }

    private fun generateComputeHashes() = zincMethod {
        var groupOffset = 0
        val serializedDigest = digest.getSerializedTypeDef().getType() as ZincArray
        val fieldHashes = transactionStates.fold("") { acc, indexedState ->
            val result = """
                component_leaf_hashes[$groupOffset as u32] = blake2s_multi_input(
                    nonces[$groupOffset as u32],
                    self.${indexedState.fieldName},
                );
            """.trimIndent()
            groupOffset += 1
            acc + "\n" + result + "\n"
        }
        name = if (baseName.endsWith("Utxos")) COMPUTE_UTXO_HASHES else COMPUTE_LEAF_HASHES
        parameter {
            name = "nonces"
            type = zincArray {
                elementType = digest.getSerializedTypeDef()
                size = "$groupSize"
            }
        }
        returnType = zincArray {
            elementType = digest.getSerializedTypeDef()
            size = "$groupSize"
        }
        body = """
            let mut component_leaf_hashes: [${digest.getSerializedTypeDef().getName()}; $groupSize] = [[false; ${serializedDigest.getSize()}]; $groupSize];
            ${fieldHashes.indent(12.spaces)}
            component_leaf_hashes
        """.trimIndent()
    }

    override val id: String = serializedStructName

    override val bitSize: Int = transactionStates.sumBy {
        (CORDA_MAGIC_BITS_SIZE + it.state.bitSize)
    }

    override fun typeName(): String = id

    override fun deserializeExpr(
        witnessGroupOptions: WitnessGroupOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String {
        throw UnsupportedOperationException()
    }

    override fun defaultExpr(): String = "$id::empty()"

    override fun equalsExpr(self: String, other: String): String = "$self.equals($other)"

    override fun accept(visitor: TypeVisitor) {
        visitor.visitType(deserializedStruct)
        transactionStates.forEach {
            visitor.visitType(it.state)
        }
    }

    companion object {
        private fun List<IndexedState>.toFieldList(): List<BflStructField> = map {
            field {
                name = it.fieldName
                type = it.state
            }
        }
    }
}
