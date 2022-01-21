package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.TypeVisitor
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincType.Companion.id
import com.ing.zinc.poet.indent
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.util.ifOrNull
import com.ing.zkflow.zinc.poet.generate.COMPUTE_LEAF_HASHES
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.COMPUTE_UTXO_HASHES
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

@Suppress("TooManyFunctions")
class Witness(
    private val commandMetadata: ResolvedZKCommandMetadata,
    private val inputs: Map<BflModule, Int>,
    private val outputs: Map<BflModule, Int>,
    private val references: Map<BflModule, Int>,
    private val standardTypes: StandardTypes,
) : BflModule {
    private fun Map<BflModule, Int>.toTransactionStates(): Map<BflModule, Int> = map { (stateType, count) ->
        standardTypes.transactionState(stateType) to count
    }.toMap()

    internal val serializedOutputGroup = SerializedStateGroup(OUTPUTS, "OutputGroup", outputs.toTransactionStates())
    internal val serializedInputUtxos = SerializedStateGroup(SERIALIZED_INPUT_UTXOS, "InputUtxos", inputs.toTransactionStates())
    internal val serializedReferenceUtxos = SerializedStateGroup(SERIALIZED_REFERENCE_UTXOS, "ReferenceUtxos", references.toTransactionStates())

    val publicInput = PublicInputFactory(commandMetadata).create()

    private val dependencies =
        listOfNotNull(
            stateRef, secureHash, privacySalt, ifOrNull(commandMetadata.timeWindow) { timeWindow },
            standardTypes.notaryModule, standardTypes.signerModule, nonceDigest, componentGroupEnum, publicInput
        ).sortedBy { it.getModuleName() }

    private val inputGroup = WitnessGroup(INPUTS, stateRef, commandMetadata.privateInputs.size, ComponentGroupEnum.INPUTS_GROUP)
    private val referenceGroup = WitnessGroup(REFERENCES, stateRef, commandMetadata.privateReferences.size, ComponentGroupEnum.REFERENCES_GROUP)
    private val commandGroup = WitnessGroup(COMMANDS, BflPrimitive.U32, 1, ComponentGroupEnum.COMMANDS_GROUP)
    // private val attachmentGroup = WitnessGroup(ATTACHMENTS, secureHash, 0, ComponentGroupEnum.ATTACHMENTS_GROUP)
    private val notaryGroup = WitnessGroup(NOTARY, standardTypes.notaryModule, 1, ComponentGroupEnum.NOTARY_GROUP)
    private val timeWindowGroup = WitnessGroup(TIME_WINDOW, timeWindow, 1, ComponentGroupEnum.TIMEWINDOW_GROUP)
    private val parameterGroup = WitnessGroup(PARAMETERS, secureHash, 1, ComponentGroupEnum.PARAMETERS_GROUP)
    private val signerGroup = WitnessGroup(SIGNERS, standardTypes.signerModule, commandMetadata.numberOfSigners, ComponentGroupEnum.SIGNERS_GROUP)

    private val standardComponentGroups = listOfNotNull(
        ifOrNull(commandMetadata.privateInputs.isNotEmpty()) { inputGroup },
        ifOrNull(commandMetadata.privateInputs.isNotEmpty()) { referenceGroup },
        commandGroup,
        // ifOrNull(commandMetadata.attachmentCount > 0) { attachmentGroup },
        notaryGroup,
        ifOrNull(commandMetadata.timeWindow) { timeWindowGroup },
        parameterGroup,
        signerGroup,
    )

    private fun arrayOfNonceDigests(capacity: Int) = zincArray {
        size = "$capacity"
        elementType = nonceDigest.toZincId()
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile = ZincFile.zincFile {
        mod { module = CONSTS }
        newLine()
        listOfNotNull(
            ifOrNull(commandMetadata.privateOutputs.isNotEmpty()) { serializedOutputGroup },
            ifOrNull(commandMetadata.privateInputs.isNotEmpty()) { serializedInputUtxos },
            ifOrNull(commandMetadata.privateReferences.isNotEmpty()) { serializedReferenceUtxos },
        ).forEach {
            mod { module = it.getModuleName() }
            use { path = "${it.getModuleName()}::${it.id}" }
            newLine()
        }
        listOfNotNull(
            ifOrNull(commandMetadata.privateInputs.isNotEmpty()) { "InputGroup" },
            ifOrNull(commandMetadata.privateReferences.isNotEmpty()) { "ReferenceGroup" },
        ).forEach {
            mod { module = it.camelToSnakeCase() }
            use { path = "${it.camelToSnakeCase()}::$it" }
        }
        dependencies.forEach { dependency ->
            mod { module = dependency.getModuleName() }
            use { path = "${dependency.getModuleName()}::${dependency.id}" }
            if ((dependency is BflStruct && dependency.isDeserializable) || (dependency !is BflStruct)) {
                use { path = "${dependency.getModuleName()}::${dependency.getLengthConstant()}" }
                use { path = "${dependency.getModuleName()}::${dependency.getSerializedTypeDef().getName()}" }
            }
            newLine()
        }
        (
            listOf(LEDGER_TRANSACTION, COMMAND_GROUP) +
                listOf(
                    CommandGroupFactory.getCommandTypeName(commandMetadata),
                    standardTypes.getSignerListModule(commandMetadata.numberOfSigners).id,
                ).distinct()
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
            if (commandMetadata.privateInputs.isNotEmpty()) {
                field { name = INPUTS; type = inputGroup.arrayOfSerializedData() }
            }
            if (commandMetadata.privateOutputs.isNotEmpty()) {
                field { name = OUTPUTS; type = serializedOutputGroup.toZincId() }
            }
            if (commandMetadata.privateReferences.isNotEmpty()) {
                field { name = REFERENCES; type = referenceGroup.arrayOfSerializedData() }
            }
            field { name = COMMANDS; type = commandGroup.arrayOfSerializedData() }
//            if (commandMetadata.attachmentCount > 0) {
//                field { name = ATTACHMENTS; type = attachmentGroup.arrayOfSerializedData() }
//            }
            field { name = NOTARY; type = notaryGroup.arrayOfSerializedData() }
            if (commandMetadata.timeWindow) {
                field { name = TIME_WINDOW; type = timeWindowGroup.arrayOfSerializedData() }
            }
            field { name = PARAMETERS; type = parameterGroup.arrayOfSerializedData() }
            field { name = SIGNERS; type = signerGroup.arrayOfSerializedData() }
            field { name = PRIVACY_SALT; type = privacySalt.toZincId() }
            if (commandMetadata.privateInputs.isNotEmpty()) {
                field { name = INPUT_NONCES; type = arrayOfNonceDigests(commandMetadata.privateInputs.size) }
            }
            if (commandMetadata.privateReferences.isNotEmpty()) {
                field { name = REFERENCE_NONCES; type = arrayOfNonceDigests(commandMetadata.privateReferences.size) }
            }
            if (commandMetadata.privateInputs.isNotEmpty()) {
                field { name = SERIALIZED_INPUT_UTXOS; type = serializedInputUtxos.toZincId() }
            }
            if (commandMetadata.privateReferences.isNotEmpty()) {
                field { name = SERIALIZED_REFERENCE_UTXOS; type = serializedReferenceUtxos.toZincId() }
            }
        }
        newLine()
        impl {
            name = Witness::class.java.simpleName
            addFunctions(generateMethods(codeGenerationOptions))
        }
    }

    fun getWitnessConfigurations(): List<WitnessGroupOptions> {
        return standardComponentGroups.map {
            it.witnessGroupOptions
        } + outputs.keys.map {
            WitnessGroupOptions.cordaWrapped("${OUTPUTS}_${it.id.camelToSnakeCase()}", standardTypes.transactionState(it))
        } + inputs.keys.map {
            WitnessGroupOptions.cordaWrapped("${SERIALIZED_INPUT_UTXOS}_${it.id.camelToSnakeCase()}", standardTypes.transactionState(it))
        } + references.keys.map {
            WitnessGroupOptions.cordaWrapped("${SERIALIZED_REFERENCE_UTXOS}_${it.id.camelToSnakeCase()}", standardTypes.transactionState(it))
        }
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> =
        standardComponentGroups.mapNotNull(WitnessGroup::generateDeserializeMethod) +
            standardComponentGroups.mapNotNull(WitnessGroup::generateHashesMethod) +
            generateComputeHashesMethodForUtxos() +
            generateComputeHashesMethodForOutputs() +
            generateDeserializeMethod() +
            generateGenerateHashesMethod()

    private fun generateDeserializeMethod() =
        zincMethod {
            comment = "Deserialize ${Witness::class.java.simpleName} into a $LEDGER_TRANSACTION."
            name = "deserialize"
            returnType = id(LEDGER_TRANSACTION)
            val deserializeInputs = if (commandMetadata.privateInputs.isNotEmpty()) {
                """
                    let $INPUTS = InputGroup::from_states_and_refs(
                        self.$SERIALIZED_INPUT_UTXOS.deserialize(),
                        self.deserialize_$INPUTS(),
                    );
                """.trimIndent()
            } else {
                "// $INPUTS not present"
            }
            val deserializeReferences = if (commandMetadata.privateReferences.isNotEmpty()) {
                """
                    let $REFERENCES = ReferenceGroup::from_states_and_refs(
                        self.$SERIALIZED_REFERENCE_UTXOS.deserialize(),
                        self.deserialize_$REFERENCES(),
                    );
                """.trimIndent()
            } else {
                "// $REFERENCES not present"
            }
            body = """
                let $SIGNERS = self.deserialize_$SIGNERS();
                ${deserializeInputs.indent(16.spaces)}
                ${deserializeReferences.indent(16.spaces)}

                $LEDGER_TRANSACTION {
                    ${if (commandMetadata.privateInputs.isNotEmpty()) "$INPUTS: $INPUTS," else "// $INPUTS not present"}
                    ${if (commandMetadata.privateOutputs.isNotEmpty()) "$OUTPUTS: self.$OUTPUTS.deserialize()," else "// $OUTPUTS not present"}
                    ${if (commandMetadata.privateReferences.isNotEmpty()) "$REFERENCES: $REFERENCES," else "// $REFERENCES not present"}
                    $COMMANDS: $COMMAND_GROUP::$FROM_SIGNERS($SIGNERS),
                    // $ATTACHMENTS: self.deserialize_$ATTACHMENTS(),
                    $NOTARY: self.deserialize_$NOTARY()[0],
                    ${if (commandMetadata.timeWindow) "$TIME_WINDOW: self.deserialize_$TIME_WINDOW()[0]," else "// $TIME_WINDOW not present"}
                    $PARAMETERS: self.deserialize_$PARAMETERS()[0],
                    $SIGNERS: $SIGNERS,
                    ${PRIVACY_SALT}_field: self.$PRIVACY_SALT,
                    ${if (commandMetadata.privateInputs.isNotEmpty()) "$INPUT_NONCES: self.$INPUT_NONCES," else "// $INPUT_NONCES not present"}
                    ${if (commandMetadata.privateReferences.isNotEmpty()) "$REFERENCE_NONCES: self.$REFERENCE_NONCES," else "// $REFERENCE_NONCES not present"}
                }
            """.trimIndent()
        }

    private fun generateGenerateHashesMethod() = zincMethod {
        val hashInitializers = listOf(
            Pair(commandMetadata.privateInputs.size, "$INPUTS: self.compute_${INPUTS}_leaf_hashes(),"),
            Pair(commandMetadata.privateOutputs.size, "$OUTPUTS: self.compute_${OUTPUTS}_leaf_hashes(),"),
            Pair(commandMetadata.privateReferences.size, "$REFERENCES: self.compute_${REFERENCES}_leaf_hashes(),"),
            Pair(1, "$COMMANDS: self.compute_${COMMANDS}_leaf_hashes(),"),
            // Pair(commandMetadata.attachmentCount, "$ATTACHMENTS: self.compute_${ATTACHMENTS}_leaf_hashes(),"),
            Pair(1, "$NOTARY: self.compute_${NOTARY}_leaf_hashes(),"),
            Pair(if (commandMetadata.timeWindow) 1 else 0, "$TIME_WINDOW: self.compute_${TIME_WINDOW}_leaf_hashes(),"),
            Pair(commandMetadata.numberOfSigners, "$SIGNERS: self.compute_${SIGNERS}_leaf_hashes(),"),
            Pair(1, "$PARAMETERS: self.compute_${PARAMETERS}_leaf_hashes(),"),
            Pair(commandMetadata.privateInputs.size, "$SERIALIZED_INPUT_UTXOS: self.compute_${SERIALIZED_INPUT_UTXOS}_hashes(),"),
            Pair(commandMetadata.privateReferences.size, "$SERIALIZED_REFERENCE_UTXOS: self.compute_${SERIALIZED_REFERENCE_UTXOS}_hashes(),"),
        ).filter { it.first > 0 }.joinToString("\n") { it.second }
        name = "generate_hashes"
        returnType = publicInput.toZincId()
        body = """
            ${publicInput.id} {
                ${hashInitializers.indent(16.spaces)}
            }
        """.trimIndent()
    }

    private fun generateComputeHashesMethodForUtxos(): List<ZincFunction> {
        return listOfNotNull(
            ifOrNull(commandMetadata.privateInputs.isNotEmpty()) {
                Triple(inputs, SERIALIZED_INPUT_UTXOS, INPUT_NONCES)
            },
            ifOrNull(commandMetadata.privateReferences.isNotEmpty()) {
                Triple(references, SERIALIZED_REFERENCE_UTXOS, REFERENCE_NONCES)
            },
        ).map {
            val arraySize = it.first.values.sum()
            zincMethod {
                comment = "Compute the ${it.second} leaf hashes."
                name = "compute_${it.second}_hashes"
                returnType = zincArray {
                    elementType = nonceDigest.toZincId()
                    size = "$arraySize"
                }
                body = """
                    self.${it.second}.$COMPUTE_UTXO_HASHES(self.${it.third})
                """.trimIndent()
            }
        }
    }

    private fun generateComputeHashesMethodForOutputs(): List<ZincFunction> {
        return listOfNotNull(
            ifOrNull(commandMetadata.privateOutputs.isNotEmpty()) { Pair(outputs, OUTPUTS) },
        ).map {
            val arraySize = it.first.values.sum()
            zincMethod {
                comment = "Compute the ${it.second} leaf hashes."
                name = "compute_${it.second}_leaf_hashes"
                returnType = zincArray {
                    elementType = nonceDigest.toZincId()
                    size = "$arraySize"
                }
                body = """
                    let mut nonces = [${nonceDigest.defaultExpr()}; ${commandMetadata.privateOutputs.size}];

                    for i in (0 as u32)..${commandMetadata.privateOutputs.size} {
                        nonces[i] = $COMPUTE_NONCE(self.$PRIVACY_SALT, ${componentGroupEnum.id}::${ComponentGroupEnum.OUTPUTS_GROUP.name} as u32, i);
                    }

                    self.${it.second}.$COMPUTE_LEAF_HASHES(nonces)
                """.trimIndent()
            }
        }
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
        listOfNotNull(
            ifOrNull(commandMetadata.privateOutputs.isNotEmpty()) { serializedOutputGroup },
            ifOrNull(commandMetadata.privateInputs.isNotEmpty()) { serializedInputUtxos },
            ifOrNull(commandMetadata.privateReferences.isNotEmpty()) { serializedReferenceUtxos },
        ).forEach { type ->
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
        internal const val PARAMETERS = "parameters"
        internal const val SIGNERS = "signers"
        internal const val PRIVACY_SALT = "privacy_salt"
        internal const val INPUT_NONCES = "input_nonces"
        internal const val REFERENCE_NONCES = "reference_nonces"
        internal const val SERIALIZED_INPUT_UTXOS = "serialized_input_utxos"
        internal const val SERIALIZED_REFERENCE_UTXOS = "serialized_reference_utxos"
    }
}
