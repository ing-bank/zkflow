package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflList
import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE_CONSTANT
import com.ing.zinc.bfl.TypeVisitor
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.indent
import com.ing.zkflow.util.bitsToByteBoundary
import com.ing.zkflow.zinc.poet.generate.COMPUTE_LEAF_HASHES
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.COMPUTE_UTXO_HASHES
import com.ing.zkflow.zinc.poet.generate.CRYPTO_UTILS
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.nonceDigest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt

@Suppress("TooManyFunctions")
data class SerializedStateGroup(
    private val groupName: String,
    private val baseName: String,
    private val transactionStates: Map<BflModule, Int>,
) : BflModule {
    private val serializedStructName: String = "Serialized$baseName"
    internal val deserializedStruct = struct {
        name = "Deserialized$baseName"
        addFields(transactionStates.toFieldList())
        isDeserializable = false
    }

    private val transactionStateLists: List<BflStructField> = transactionStates.toFieldList()
    private val groupSize = transactionStates.values.sum()

    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile = ZincFile.zincFile {
        mod { module = CONSTS }
        newLine()
        transactionStates.forEach { (stateType, _) ->
            mod { module = stateType.getModuleName() }
            use { path = "${stateType.getModuleName()}::${stateType.id}" }
            use { path = "${stateType.getModuleName()}::${stateType.getLengthConstant()}" }
            newLine()
        }
        transactionStateLists.forEach {
            val stateList = it.type as BflModule // Safe cast, because it is generated with transactionStates.toFieldList()
            mod { module = stateList.getModuleName() }
            use { path = "${stateList.getModuleName()}::${stateList.id}" }
            newLine()
        }
        mod { module = deserializedStruct.getModuleName() }
        use { path = "${deserializedStruct.getModuleName()}::${deserializedStruct.id}" }
        newLine()
        listOf(privacySalt, nonceDigest).forEach {
            mod { module = it.getModuleName() }
            use { path = "${it.getModuleName()}::${it.id}" }
            use { path = "${it.getModuleName()}::${it.getSerializedTypeDef().getName()}" }
            use { path = "${it.getModuleName()}::${it.getLengthConstant()}" }
            newLine()
        }
        mod { module = CRYPTO_UTILS }
        use { path = "$CRYPTO_UTILS::$COMPUTE_NONCE" }
        use { path = "std::crypto::blake2s_multi_input" }
        newLine()
        struct {
            name = serializedStructName
            transactionStates.forEach { (stateType, count) ->
                val paddingBits = stateType.bitSize.bitsToByteBoundary() - stateType.bitSize
                field {
                    name = stateTypeFieldName(stateType)
                    type = zincArray {
                        size = count.toString()
                        elementType = zincArray {
                            size = getSerializedBitSize(paddingBits, stateType)
                            elementType = ZincPrimitive.Bool
                        }
                    }
                }
            }
        }
        newLine()
        impl {
            name = serializedStructName
            addFunctions(generateMethods(codeGenerationOptions))
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
            generateDeserializeMethod(),
            generateEmptyMethod(),
            generateEqualsMethod(),
            generateComputeHashes(),
        )
    }

    private fun generateDeserializeMethod() = zincMethod {
        name = "deserialize"
        returnType = deserializedStruct.toZincId()
        val fieldDeserializations = transactionStateLists.joinToString("\n") {
            val bflList = it.type as BflList
            val stateType = bflList.elementType
            val stateTypeId = stateType.id
            val deserializeMethodName = "deserialize_from_${groupName}_${stateTypeFieldName(stateType as BflModule)}"
            val array = "${it.name}_array"
            val i = "${it.name}_i"
            """
                let ${it.name}_var = {
                    let mut $array: [$stateTypeId; ${bflList.capacity}] = [$stateTypeId::empty(); ${bflList.capacity}];
                    for $i in (0 as u24)..${bflList.capacity} {
                        $array[$i] = $stateTypeId::$deserializeMethodName(self.${it.name}[$i], $CORDA_MAGIC_BITS_SIZE_CONSTANT);
                    }
                    ${bflList.id}::list_of($array)
                };
            """.trimIndent()
        }
        val fieldAssignments = transactionStateLists.joinToString(",\n") {
            "${it.name}: ${it.name}_var"
        }
        body = """
            ${fieldDeserializations.indent(12.spaces)}

            ${deserializedStruct.id} {
                ${fieldAssignments.indent(16.spaces)}
            }
        """.trimIndent()
    }

    private fun generateEmptyMethod() = zincFunction {
        val fieldInitializations = transactionStates.entries.joinToString("\n") { (stateType, count) ->
            val paddingBits = stateType.bitSize.bitsToByteBoundary() - stateType.bitSize
            "${stateTypeFieldName(stateType)}: [[false; ${getSerializedBitSize(paddingBits, stateType)}]; $count],"
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
        val fieldEquals = transactionStates.entries.joinToString(" && ") { (stateType, count) ->
            val stateFieldName = stateTypeFieldName(stateType)
            """
                {
                    let mut still_equals: bool = true;
                    for i in 0..$count while still_equals {
                        for j in 0..${stateType.getLengthConstant()} while still_equals {
                            still_equals = self.$stateFieldName[i][j + $CORDA_MAGIC_BITS_SIZE_CONSTANT] == other.$stateFieldName[i][j + $CORDA_MAGIC_BITS_SIZE_CONSTANT];
                        }
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
        val fieldHashes = transactionStates.entries.fold("") { acc, (stateType, count) ->
            val result = """
                for i in (0 as u32)..$count {
                    component_leaf_hashes[i + $groupOffset as u32] = blake2s_multi_input(
                        nonces[i + $groupOffset as u32],
                        self.${stateTypeFieldName(stateType)}[i],
                    );
                }
            """.trimIndent()
            groupOffset += count
            acc + "\n" + result + "\n"
        }
        name = if (baseName.endsWith("Utxos")) COMPUTE_UTXO_HASHES else COMPUTE_LEAF_HASHES
        parameter {
            name = "nonces"
            type = zincArray {
                elementType = nonceDigest.toZincId()
                size = "$groupSize"
            }
        }
        returnType = zincArray {
            elementType = nonceDigest.toZincId()
            size = "$groupSize"
        }
        body = """
            let mut component_leaf_hashes = [${nonceDigest.defaultExpr()}; $groupSize];
            ${fieldHashes.indent(12.spaces)}
            component_leaf_hashes
        """.trimIndent()
    }

    override val id: String = serializedStructName

    override val bitSize: Int = transactionStates.entries.sumBy { (type, count) ->
        (CORDA_MAGIC_BITS_SIZE + type.bitSize) * count
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
        transactionStateLists.forEach {
            visitor.visitType(it.type)
        }
    }
}
