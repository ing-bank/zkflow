package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.TypeVisitor
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.mod
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.bfl.use
import com.ing.zinc.bfl.useLengthConstant
import com.ing.zinc.bfl.useSerialized
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincStruct.Companion.zincStruct
import com.ing.zinc.poet.ZincType.Companion.id
import com.ing.zinc.poet.indent
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.util.BflSized
import com.ing.zkflow.util.NodeDescriptor
import com.ing.zkflow.util.Tree
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.CRYPTO_UTILS
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory.Companion.COMMAND_CONTEXT
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.digest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.witness.StandardComponentWitnessGroup
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroup
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroupsContainer

@Suppress("TooManyFunctions")
class Witness(
    private val witnessGroupsContainer: WitnessGroupsContainer,
    private val commandMetadata: ResolvedZKCommandMetadata,
    private val standardTypes: StandardTypes,
) : BflModule {
    private val witnessGroups = witnessGroupsContainer.witnessGroups

    internal val publicInput = PublicInputFactory(witnessGroupsContainer).create()

    private val dependencies = (
        witnessGroups.flatMap { it.dependencies } +
            listOf(componentGroupEnum, digest, publicInput)
        )
        .filterIsInstance<BflModule>()
        .distinctBy { it.id }
        .sortedBy { it.id }

    override fun getModuleName(): String = "command_witness"

    @Suppress("LongMethod", "ComplexMethod")
    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions) = zincFile {
        mod { module = CONSTS }
        newLine()
        (
            dependencies + listOfNotNull(
                if (witnessGroupsContainer.signerGroup.isPresent) {
                    standardTypes.signerList(commandMetadata)
                } else {
                    null
                }
            )
            )
            .distinctBy { it.id }
            .sortedBy { it.id }
            .forEach { dependency ->
                add(dependency.mod())
                add(dependency.use())
                when (dependency) {
                    digest -> {
                        add(dependency.useLengthConstant())
                        add(dependency.useSerialized())
                    }
                    privacySalt -> {
                        add(dependency.useSerialized())
                    }
                }
                newLine()
            }
        listOfNotNull(
            COMMAND_CONTEXT,
        ).forEach {
            mod { module = it.camelToSnakeCase() }
            use { path = "${it.camelToSnakeCase()}::$it" }
            newLine()
        }
        mod { module = CRYPTO_UTILS }
        use { path = "$CRYPTO_UTILS::$COMPUTE_NONCE" }
        use { path = "std::crypto::blake2s_multi_input" }
        newLine()
        add(toZincType())
        newLine()
        impl {
            name = Witness::class.java.simpleName
            addFunctions(generateMethods(codeGenerationOptions))
        }
    }

    override fun toZincType() = zincStruct {
        name = Witness::class.java.simpleName
        witnessGroups.forEach {
            field {
                name = it.groupName
                type = it.serializedType
            }
        }
    }

    val witnessConfigurations: List<WitnessGroupOptions>
        get() = witnessGroups.flatMap {
            it.options
        }

    private fun getStandardComponentWitnessGroups() = witnessGroups
        .filterIsInstance<StandardComponentWitnessGroup>()

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> =
        getStandardComponentWitnessGroups().mapNotNull(StandardComponentWitnessGroup::generateDeserializeMethod) +
            witnessGroups.mapNotNull(WitnessGroup::generateHashesMethod) +
            generateDeserializeMethod() +
            generateGenerateHashesMethod()

    private fun generateDeserializeMethod() = zincMethod {
        comment = "Deserialize ${Witness::class.java.simpleName} into a $COMMAND_CONTEXT."
        name = "deserialize"
        returnType = id(COMMAND_CONTEXT)
        body = """
            $COMMAND_CONTEXT {
                ${if (witnessGroupsContainer.serializedInputUtxos.isPresent) "$INPUTS: self.$SERIALIZED_INPUT_UTXOS.deserialize()," else "// $INPUTS not present in transaction"}
                ${if (witnessGroupsContainer.serializedOutputGroup.isPresent) "$OUTPUTS: self.$OUTPUTS.deserialize()," else "// $OUTPUTS not present in transaction"}
                ${if (witnessGroupsContainer.serializedReferenceUtxos.isPresent) "$REFERENCES: self.$SERIALIZED_REFERENCE_UTXOS.deserialize()," else "// $REFERENCES not present in transaction"}
                ${if (witnessGroupsContainer.notaryGroup.isPresent) "$NOTARY: self.deserialize_$NOTARY()[0]," else "// $NOTARY is not present in transaction"}
                ${if (witnessGroupsContainer.timeWindowGroup.isPresent) "$TIME_WINDOW: self.deserialize_$TIME_WINDOW()[0]," else "// $TIME_WINDOW not present in transaction"}
                ${if (witnessGroupsContainer.parameterGroup.isPresent) "$PARAMETERS: self.deserialize_$PARAMETERS()[0]," else "// $PARAMETERS not present in transaction"}
                ${if (witnessGroupsContainer.signerGroup.isPresent) "$SIGNERS: ${standardTypes.signerList(commandMetadata).id}::list_of(self.deserialize_$SIGNERS())," else "// $SIGNERS not present in transaction"}
            }
        """.trimIndent()
    }

    private fun generateGenerateHashesMethod() = zincMethod {
        val hashInitializers = witnessGroups.mapNotNull { witnessGroup ->
            witnessGroup.generateHashesMethod?.getName()?.let { hashFunctionName ->
                "${witnessGroup.publicInputFieldName}: self.$hashFunctionName(),"
            }
        }.joinToString("\n") { it }
        name = "generate_hashes"
        returnType = publicInput.toZincId()
        body = """
            ${publicInput.id} {
                ${hashInitializers.indent(16.spaces)}
            }
        """.trimIndent()
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
        dependencies.forEach { dependency ->
            visitor.visitType(dependency)
        }
    }

    override fun toStructureTree(): Tree<BflSized, BflSized> {
        return Tree.node(NodeDescriptor(id, 0)) {
            leaf(NodeDescriptor("Content not supported", 0))
        }
    }

    companion object {
        internal const val OUTPUTS = "outputs"
        internal const val INPUTS = "inputs"
        internal const val REFERENCES = "references"
        internal const val COMMANDS = "commands"
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
