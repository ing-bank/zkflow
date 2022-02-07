package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.TypeVisitor
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincType.Companion.id
import com.ing.zinc.poet.indent
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.CRYPTO_UTILS
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory.Companion.LEDGER_TRANSACTION
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.nonceDigest
import com.ing.zkflow.zinc.poet.generate.types.witness.StandardComponentWitnessGroup
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroup
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroupsContainer

@Suppress("TooManyFunctions")
class Witness(
    witnessGroupsContainer: WitnessGroupsContainer,
    private val commandMetadata: ResolvedZKCommandMetadata,
    private val standardTypes: StandardTypes,
) : BflModule {
    private val witnessGroups = witnessGroupsContainer.witnessGroups

    internal val publicInput = PublicInputFactory(witnessGroupsContainer).create()

    private val dependencies = (
        witnessGroups.flatMap { it.dependencies } +
            listOf(componentGroupEnum, nonceDigest, publicInput)
        )
        .filterIsInstance<BflModule>()
        .distinctBy { it.id }
        .sortedBy { it.id }

    @Suppress("LongMethod", "ComplexMethod")
    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile = ZincFile.zincFile {
        mod { module = CONSTS }
        newLine()
        dependencies
            .forEach { dependency ->
                mod { module = dependency.getModuleName() }
                use { path = "${dependency.getModuleName()}::${dependency.id}" }
                if (dependency == nonceDigest) {
                    use { path = "${dependency.getModuleName()}::${dependency.getLengthConstant()}" }
                }
                newLine()
            }
        (
            listOfNotNull(
                LEDGER_TRANSACTION,
            ) + listOf(
                standardTypes.getSignerListModule(commandMetadata.numberOfSigners, commandMetadata).id,
            )
            ).distinct().sortedBy { it }
            .forEach {
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
            witnessGroups.forEach {
                field {
                    name = it.groupName
                    type = it.serializedType
                }
            }
        }
        newLine()
        impl {
            name = Witness::class.java.simpleName
            addFunctions(generateMethods(codeGenerationOptions))
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

    private fun generateDeserializeMethod() =
        zincMethod {
            comment = "Deserialize ${Witness::class.java.simpleName} into a $LEDGER_TRANSACTION."
            name = "deserialize"
            returnType = id(LEDGER_TRANSACTION)

            body = """
                let $SIGNERS = self.deserialize_$SIGNERS();

                $LEDGER_TRANSACTION {
                    ${if (commandMetadata.privateInputs.isNotEmpty()) "$INPUTS: self.$SERIALIZED_INPUT_UTXOS.deserialize()," else "// $INPUTS not present"}
                    ${if (commandMetadata.privateOutputs.isNotEmpty()) "$OUTPUTS: self.$OUTPUTS.deserialize()," else "// $OUTPUTS not present"}
                    ${if (commandMetadata.privateReferences.isNotEmpty()) "$REFERENCES: self.$SERIALIZED_REFERENCE_UTXOS.deserialize()," else "// $REFERENCES not present"}
                    $NOTARY: self.deserialize_$NOTARY()[0],
                    ${if (commandMetadata.timeWindow) "$TIME_WINDOW: self.deserialize_$TIME_WINDOW()[0]," else "// $TIME_WINDOW not present"}
                    $PARAMETERS: self.deserialize_$PARAMETERS()[0],
                    $SIGNERS: $SIGNERS,
                }
            """.trimIndent()
        }

    private fun generateGenerateHashesMethod() = zincMethod {
        val hashInitializers = witnessGroups.mapNotNull { witnessGroup ->
            witnessGroup.generateHashesMethod?.getName()?.let { hashFunctionName ->
                "${witnessGroup.groupName}: self.$hashFunctionName(),"
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
