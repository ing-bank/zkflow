package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ZKTypedElement
import com.ing.zkflow.util.requireNotNull
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.Witness
import net.corda.core.contracts.ContractState
import net.corda.core.internal.deleteRecursively
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.Locale
import kotlin.reflect.KClass

@Suppress("LongParameterList")
class CircuitGenerator(
    private val buildPathProvider: BuildPathProvider,
    private val ledgerTransactionFactory: LedgerTransactionFactory,
    private val standardTypes: StandardTypes,
    private val stateAndRefsGroupFactory: StateAndRefsGroupFactory,
    private val zincTypeResolver: ZincTypeResolver,
    private val constsFactory: ConstsFactory,
    private val cryptoUtilsFactory: CryptoUtilsFactory,
) {

    fun generateCircuitFor(commandMetadata: ResolvedZKCommandMetadata) {
        // TODO this is a dirty hack just to make API ends meet, zinc-poet should be changed to operate on individual components instead of lists per type
        val zincInputs = commandMetadata.privateInputs.toBflModuleMap()
        val zincOutputs = commandMetadata.privateOutputs.toBflModuleMap()
        val zincReferences = commandMetadata.privateReferences.toBflModuleMap()

        val witness = createWitness(commandMetadata, zincInputs, zincOutputs, zincReferences)
        val inputGroup = stateAndRefsGroupFactory.createStructWithStateAndRefs(
            "InputGroup",
            zincInputs,
            witness.serializedInputUtxos.deserializedStruct
        )
        val referenceGroup = stateAndRefsGroupFactory.createStructWithStateAndRefs(
            "ReferenceGroup",
            zincReferences,
            witness.serializedReferenceUtxos.deserializedStruct
        )
        val ledgerTransaction = ledgerTransactionFactory.createLedgerTransaction(
            inputGroup,
            referenceGroup,
            commandMetadata,
            witness
        )

        val codeGenerationOptions = CodeGenerationOptions(witness.getWitnessConfigurations())

        val circuitName = commandMetadata.circuit.name

        val buildPath = buildPathProvider.getBuildPath(commandMetadata)
        buildPath.deleteRecursively()
        buildPath.toFile().mkdirs()

        logger.info("Generating circuit $circuitName in folder $buildPath")

        buildPath.createZargoToml(circuitName, "1.0.0")

        sequenceOf(witness, ledgerTransaction).allModules {
            buildPath.zincSourceFile(this, codeGenerationOptions)
        }

        constsFactory.generateConsts(buildPath, codeGenerationOptions)
        cryptoUtilsFactory.generateCryptoUtils(buildPath)
        generateMain(buildPath, witness, ledgerTransaction)
        buildPath.zincSourceFile(componentGroupEnum, codeGenerationOptions)
        logger.info("... done")
    }

    private fun List<ZKTypedElement>.toBflModuleMap(): Map<BflModule, Int> {
        return this.fold<ZKTypedElement, MutableMap<KClass<out ContractState>, Int>>(mutableMapOf()) { acc, zkReference: ZKTypedElement ->
            acc[zkReference.type] = acc[zkReference.type]?.let { it + 1 } ?: 1
            acc
        }.mapKeys {
            zincTypeResolver.zincTypeOf(it.key)
        }
    }

    private fun generateMain(
        buildPath: Path,
        witness: Witness,
        ledgerTransaction: BflStruct
    ) {
        buildPath.zincSourceFile("main.zn") {
            listOf(witness, ledgerTransaction, witness.publicInput)
                .sortedBy { it.getModuleName() }
                .forEach { dependency ->
                    mod { module = dependency.getModuleName() }
                    use { path = "${dependency.getModuleName()}::${dependency.id}" }
                    newLine()
                }

            function {
                comment = "TODO"
                name = "verify"
                parameter {
                    name = "tx"
                    type = ledgerTransaction.toZincId()
                }
                returnType = ZincPrimitive.Unit
                body = ""
            }
            newLine()
            function {
                name = "main"
                parameter {
                    name = "input"
                    type = witness.toZincId()
                }
                returnType = witness.publicInput.toZincId()
                body = """
                    let tx = input.deserialize();
                    verify(tx);
                    input.generate_hashes()
                """.trimIndent()
            }
        }
    }

    private fun createWitness(
        transactionMetadata: ResolvedZKCommandMetadata,
        inputs: Map<BflModule, Int>,
        outputs: Map<BflModule, Int>,
        references: Map<BflModule, Int>
    ): Witness {
        return Witness(
            transactionMetadata,
            inputs,
            outputs,
            references,
            standardTypes,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

private fun ZKCommandData.circuitName(): String {
    return this::class.qualifiedName.requireNotNull { "ZKCommand class should be a named class." }
        .split(".")
        .filter { it[0].isUpperCase() && it != "Companion" }
        .joinToString("-") { it.toLowerCase(Locale.getDefault()) }
}
