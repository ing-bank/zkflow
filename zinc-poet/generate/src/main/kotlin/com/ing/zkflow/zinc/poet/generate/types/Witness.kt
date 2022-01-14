package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE_CONSTANT
import com.ing.zinc.bfl.TypeVisitor
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType.Companion.id
import com.ing.zinc.poet.indent
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.util.bitsToByteBoundary
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.CRYPTO_UTILS
import com.ing.zkflow.zinc.poet.generate.types.CommandGroupFactory.Companion.COMMAND_GROUP
import com.ing.zkflow.zinc.poet.generate.types.CommandGroupFactory.Companion.FROM_SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory.Companion.LEDGER_TRANSACTION
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.nonceDigest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.secureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.stateRef
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import net.corda.core.contracts.ComponentGroupEnum

private data class Group(
    val name: String,
    val module: BflType,
    val groupSize: Int,
    val componentGroup: ComponentGroupEnum
)

@Suppress("TooManyFunctions")
class Witness(
    private val transactionMetadata: ResolvedZKTransactionMetadata,
    private val inputs: Map<BflModule, Int>,
    private val outputs: Map<BflModule, Int>,
    private val references: Map<BflModule, Int>,
    private val standardTypes: StandardTypes,
) : BflModule {
    private fun Map<BflModule, Int>.toTransactionStates(): Map<BflModule, Int> = map { (stateType, count) ->
        standardTypes.transactionState(stateType) to count
    }.toMap()

    internal val serializedOutputGroup = SerializedStateGroup(OUTPUTS, "OutputGroup", outputs.toTransactionStates(), ComponentGroupEnum.OUTPUTS_GROUP)
    internal val serializedInputUtxos = SerializedStateGroup(SERIALIZED_INPUT_UTXOS, "InputUtxos", inputs.toTransactionStates(), ComponentGroupEnum.INPUTS_GROUP)
    internal val serializedReferenceUtxos = SerializedStateGroup(SERIALIZED_REFERENCE_UTXOS, "ReferenceUtxos", references.toTransactionStates(), ComponentGroupEnum.REFERENCES_GROUP)

    private val dependencies =
        listOf(stateRef, secureHash, privacySalt, timeWindow, standardTypes.notaryModule, standardTypes.signerModule, nonceDigest, componentGroupEnum)

    private val standardComponentGroups = listOfNotNull(
        Group(INPUTS, stateRef, transactionMetadata.numberOfInputs, ComponentGroupEnum.INPUTS_GROUP),
        Group(REFERENCES, stateRef, transactionMetadata.numberOfReferences, ComponentGroupEnum.REFERENCES_GROUP),
        Group(COMMANDS, BflPrimitive.U32, transactionMetadata.commands.size, ComponentGroupEnum.COMMANDS_GROUP),
        Group(ATTACHMENTS, secureHash, transactionMetadata.attachmentCount, ComponentGroupEnum.ATTACHMENTS_GROUP),
        Group(NOTARY, standardTypes.notaryModule, 1, ComponentGroupEnum.NOTARY_GROUP),
        if (transactionMetadata.hasTimeWindow) Group(TIME_WINDOW, timeWindow, 1, ComponentGroupEnum.TIMEWINDOW_GROUP) else null,
        Group(SIGNERS, standardTypes.signerModule, transactionMetadata.numberOfSigners, ComponentGroupEnum.SIGNERS_GROUP),
        Group(PARAMETERS, secureHash, 1, ComponentGroupEnum.PARAMETERS_GROUP),
    )

    private fun arrayOfSerializedData(capacity: Int, module: BflModule): ZincArray {
        val paddingBits = module.bitSize.bitsToByteBoundary() - module.bitSize
        return if (paddingBits > 0) {
            arrayOfSerializedData(capacity, "${module.getLengthConstant()} + $paddingBits as u24")
        } else {
            arrayOfSerializedData(capacity, module.getLengthConstant())
        }
    }

    private fun arrayOfSerializedData(capacity: Int, type: BflType) =
        arrayOfSerializedData(capacity, "${type.bitSize.bitsToByteBoundary()} as u24")

    private fun arrayOfSerializedData(capacity: Int, stateSize: String) = zincArray {
        size = capacity.toString()
        elementType = zincArray {
            size = "$CORDA_MAGIC_BITS_SIZE_CONSTANT + $stateSize"
            elementType = ZincPrimitive.Bool
        }
    }

    private fun arrayOfNonceDigests(capacity: Int) = zincArray {
        size = "$capacity"
        elementType = nonceDigest.toZincId()
    }

    @Suppress("LongMethod")
    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile = ZincFile.zincFile {
        mod { module = CONSTS }
        newLine()
        listOf(serializedOutputGroup, serializedInputUtxos, serializedReferenceUtxos).forEach {
            mod { module = it.getModuleName() }
            use { path = "${it.getModuleName()}::${it.id}" }
            newLine()
        }
        listOf("InputGroup", "ReferenceGroup").forEach {
            mod { module = it.camelToSnakeCase() }
            use { path = "${it.camelToSnakeCase()}::$it" }
        }
        dependencies.forEach { dependency ->
            mod { module = dependency.getModuleName() }
            use { path = "${dependency.getModuleName()}::${dependency.id}" }
            use { path = "${dependency.getModuleName()}::${dependency.getLengthConstant()}" }
            use { path = "${dependency.getModuleName()}::${dependency.getSerializedTypeDef().getName()}" }
            newLine()
        }
        (
            listOf(LEDGER_TRANSACTION, COMMAND_GROUP) + transactionMetadata.commands.flatMap {
                listOf(
                    CommandGroupFactory.getCommandTypeName(it),
                    standardTypes.getSignerListModule(it.numberOfSigners).id,
                )
            }.distinct()
            ).forEach {
            mod { module = it.camelToSnakeCase() }
            use { path = "${it.camelToSnakeCase()}::$it" }
            newLine()
        }
        mod { module = CRYPTO_UTILS }
        use { path = "$CRYPTO_UTILS::$COMPUTE_NONCE" }
        use { path = "std::crypto::blake2s_multi_input" }
        newLine()
        struct {
            name = Witness::class.java.simpleName
            field { name = INPUTS; type = arrayOfSerializedData(transactionMetadata.numberOfInputs, stateRef) }
            field { name = OUTPUTS; type = serializedOutputGroup.toZincId() }
            field { name = REFERENCES; type = arrayOfSerializedData(transactionMetadata.numberOfReferences, stateRef) }
            field { name = COMMANDS; type = arrayOfSerializedData(transactionMetadata.commands.size, BflPrimitive.U32) }
            field { name = ATTACHMENTS; type = arrayOfSerializedData(transactionMetadata.attachmentCount, secureHash) }
            field { name = NOTARY; type = arrayOfSerializedData(1, standardTypes.notaryModule) }
            if (transactionMetadata.hasTimeWindow) {
                field { name = TIME_WINDOW; type = arrayOfSerializedData(1, timeWindow) }
            }
            field { name = SIGNERS; type = arrayOfSerializedData(transactionMetadata.numberOfSigners, standardTypes.signerModule) }
            field { name = PARAMETERS; type = arrayOfSerializedData(1, secureHash) }
            field { name = PRIVACY_SALT; type = privacySalt.getSerializedTypeDef() }
            field { name = INPUT_NONCES; type = arrayOfNonceDigests(transactionMetadata.numberOfInputs) }
            field { name = REFERENCE_NONCES; type = arrayOfNonceDigests(transactionMetadata.numberOfReferences) }
            field { name = SERIALIZED_INPUT_UTXOS; type = serializedInputUtxos.toZincId() }
            field { name = SERIALIZED_REFERENCE_UTXOS; type = serializedReferenceUtxos.toZincId() }
        }
        newLine()
        impl {
            name = Witness::class.java.simpleName
            addFunctions(generateMethods(codeGenerationOptions))
        }
    }

    fun getWitnessConfigurations(): List<WitnessGroupOptions> {
        return listOfNotNull(
            WitnessGroupOptions.cordaWrapped(INPUTS, stateRef),
            // outputs
            WitnessGroupOptions.cordaWrapped(REFERENCES, stateRef),
            WitnessGroupOptions.cordaWrapped(COMMANDS, BflPrimitive.U32),
            WitnessGroupOptions.cordaWrapped(ATTACHMENTS, secureHash),
            WitnessGroupOptions.cordaWrapped(NOTARY, standardTypes.notaryModule),
            if (transactionMetadata.hasTimeWindow) WitnessGroupOptions.cordaWrapped(TIME_WINDOW, timeWindow) else null,
            WitnessGroupOptions.cordaWrapped(SIGNERS, standardTypes.signerModule),
            WitnessGroupOptions.cordaWrapped(PARAMETERS, secureHash),
            WitnessGroupOptions(PRIVACY_SALT, privacySalt),
            WitnessGroupOptions(INPUT_NONCES, nonceDigest),
            WitnessGroupOptions(REFERENCE_NONCES, nonceDigest),
            // serialized_input_utxos
            // serialized_reference_utxos
        ) + outputs.keys.map {
            WitnessGroupOptions.cordaWrapped("${OUTPUTS}_${it.id.camelToSnakeCase()}", standardTypes.transactionState(it))
        } + inputs.keys.map {
            WitnessGroupOptions.cordaWrapped("${SERIALIZED_INPUT_UTXOS}_${it.id.camelToSnakeCase()}", standardTypes.transactionState(it))
        } + references.keys.map {
            WitnessGroupOptions.cordaWrapped("${SERIALIZED_REFERENCE_UTXOS}_${it.id.camelToSnakeCase()}", standardTypes.transactionState(it))
        }
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> = generateDeserializeMethodsForArraysOfByteArrays(codeGenerationOptions) +
        generateComputeLeafHashesMethodForArraysOfByteArrays() +
        generateComputeLeafHashesMethodForSerializedGroups() +
        generateDeserializeMethod(codeGenerationOptions)

    private fun generateDeserializeMethod(codeGenerationOptions: CodeGenerationOptions) =
        zincMethod {
            comment = "Deserialize ${Witness::class.java.simpleName} into a $LEDGER_TRANSACTION."
            name = "deserialize"
            returnType = id(LEDGER_TRANSACTION)
            val deserializePrivacySalt = generateDeserializeExpression(
                codeGenerationOptions,
                groupName = PRIVACY_SALT,
                bflType = privacySalt,
                witnessVariable = "self.$PRIVACY_SALT",
                offset = "0 as u24"
            )
            body = """
                let $SIGNERS = self.deserialize_$SIGNERS();
                let $INPUTS = InputGroup::from_states_and_refs(
                    self.$SERIALIZED_INPUT_UTXOS.deserialize(),
                    self.deserialize_$INPUTS(),
                );
                let $REFERENCES = ReferenceGroup::from_states_and_refs(
                    self.$SERIALIZED_REFERENCE_UTXOS.deserialize(),
                    self.deserialize_$REFERENCES(),
                );

                $LEDGER_TRANSACTION {
                    $INPUTS: $INPUTS,
                    $OUTPUTS: self.$OUTPUTS.deserialize(),
                    $REFERENCES: $REFERENCES,
                    $COMMANDS: $COMMAND_GROUP::$FROM_SIGNERS($SIGNERS),
                    $ATTACHMENTS: self.deserialize_$ATTACHMENTS(),
                    $NOTARY: self.deserialize_$NOTARY()[0],
                    ${if (transactionMetadata.hasTimeWindow) "$TIME_WINDOW: self.deserialize_$TIME_WINDOW()," else "// $TIME_WINDOW not present"}
                    $SIGNERS: $SIGNERS,
                    $PARAMETERS: self.deserialize_$PARAMETERS()[0],
                    ${PRIVACY_SALT}_field: $deserializePrivacySalt,
                    $INPUT_NONCES: self.$INPUT_NONCES,
                    $REFERENCE_NONCES: self.$REFERENCE_NONCES,
                }
            """.trimIndent()
        }

    private fun generateDeserializeMethodsForArraysOfByteArrays(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return standardComponentGroups.map {
            val deserializeExpression = generateDeserializeExpression(
                codeGenerationOptions,
                groupName = it.name,
                bflType = it.module,
                witnessVariable = "self.${it.name}[i]",
                offset = CORDA_MAGIC_BITS_SIZE_CONSTANT
            )
            zincMethod {
                comment = "Deserialize ${it.name} from the ${Witness::class.java.simpleName}."
                name = "deserialize_${it.name}"
                returnType = zincArray {
                    elementType = it.module.toZincId()
                    size = "${it.groupSize}"
                }
                body = """
                    let mut ${it.name}_array: [${it.module.id}; ${it.groupSize}] = [${it.module.defaultExpr()}; ${it.groupSize}];
                    for i in 0..${it.groupSize} {
                        ${it.name}_array[i] = ${deserializeExpression.indent(24.spaces)};
                    }
                    ${it.name}_array
                """.trimIndent()
            }
        }
    }

    private fun generateComputeLeafHashesMethodForArraysOfByteArrays(): List<ZincFunction> {
        return standardComponentGroups.map {
            zincMethod {
                comment = "Compute the ${it.name} leaf hashes."
                name = "compute_${it.name}_leaf_hashes"
                returnType = zincArray {
                    elementType = nonceDigest.getSerializedTypeDef()
                    size = "${it.groupSize}"
                }
                body = """
                    let mut ${it.name}_leaf_hashes = [[false; ${nonceDigest.getLengthConstant()}]; ${it.groupSize}];
                    for i in (0 as u32)..${it.groupSize} {
                        ${it.name}_leaf_hashes[i] = blake2s_multi_input(
                            compute_nonce(self.$PRIVACY_SALT, ${componentGroupEnum.id}::${it.componentGroup.name} as u32, i),
                            self.${it.name}[i],
                        );
                    }
                    ${it.name}_leaf_hashes
                """.trimIndent()
            }
        }
    }

    private fun generateComputeLeafHashesMethodForSerializedGroups(): List<ZincFunction> {
        return listOf(
            Pair(inputs, SERIALIZED_INPUT_UTXOS),
            Pair(outputs, OUTPUTS),
            Pair(references, SERIALIZED_REFERENCE_UTXOS),
        ).map {
            val arraySize = it.first.values.sum()
            zincMethod {
                comment = "Compute the ${it.second} leaf hashes."
                name = "compute_${it.second}_leaf_hashes"
                returnType = zincArray {
                    elementType = nonceDigest.getSerializedTypeDef()
                    size = "$arraySize"
                }
                body = """
                    self.${it.second}.compute_leaf_hashes(self.$PRIVACY_SALT)
                """.trimIndent()
            }
        }
    }

    private fun generateDeserializeExpression(
        codeGenerationOptions: CodeGenerationOptions,
        groupName: String,
        bflType: BflType,
        witnessVariable: String,
        offset: String
    ): String {
        val witnessGroup: WitnessGroupOptions = codeGenerationOptions.witnessGroupOptions.single { witnessGroup ->
            witnessGroup.deserializeMethodName.endsWith(groupName)
        }
        return bflType.deserializeExpr(witnessGroup, offset, groupName, witnessVariable)
    }

    override val id: String = Witness::class.java.simpleName

    override val bitSize: Int
        get() = TODO("Not yet implemented")

    override fun typeName(): String = id

    override fun deserializeExpr(
        witnessGroupOptions: WitnessGroupOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String {
        throw UnsupportedOperationException()
    }

    override fun defaultExpr(): String {
        throw UnsupportedOperationException()
    }

    override fun equalsExpr(self: String, other: String): String {
        throw UnsupportedOperationException()
    }

    override fun accept(visitor: TypeVisitor) {
        listOf(serializedOutputGroup, serializedInputUtxos, serializedReferenceUtxos).forEach { type ->
            visitor.visitType(type)
        }
        dependencies.forEach { dependency ->
            visitor.visitType(dependency)
        }
    }

    companion object {
        internal const val OUTPUTS = "outputs"
        internal const val INPUTS = "inputs"
        internal const val REFERENCES = "references"
        internal const val COMMANDS = "commands"
        internal const val ATTACHMENTS = "attachments"
        internal const val NOTARY = "notary"
        internal const val TIME_WINDOW = "time_window"
        internal const val SIGNERS = "signers"
        internal const val PARAMETERS = "parameters"
        internal const val PRIVACY_SALT = "privacy_salt"
        internal const val INPUT_NONCES = "input_nonces"
        internal const val REFERENCE_NONCES = "reference_nonces"
        internal const val SERIALIZED_INPUT_UTXOS = "serialized_input_utxos"
        internal const val SERIALIZED_REFERENCE_UTXOS = "serialized_reference_utxos"
    }
}
