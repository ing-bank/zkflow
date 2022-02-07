package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.Witness
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroupsContainer
import net.corda.core.internal.deleteRecursively
import org.slf4j.LoggerFactory
import java.nio.file.Path

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
        val witnessGroups = WitnessGroupsContainer(
            commandMetadata,
            standardTypes,
            stateAndRefsGroupFactory,
            zincTypeResolver
        )
        val witness = Witness(
            witnessGroups,
            commandMetadata,
            standardTypes,
        )

        val ledgerTransaction = ledgerTransactionFactory.createLedgerTransaction(
            commandMetadata,
            witnessGroups
        )

        val codeGenerationOptions = CodeGenerationOptions(witness.witnessConfigurations)

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
        generateContractRules(buildPath, ledgerTransaction)
        generateMain(buildPath, witness)
        buildPath.zincSourceFile(componentGroupEnum, codeGenerationOptions)
        logger.info("... done")
    }

    private fun generateContractRules(
        buildPath: Path,
        ledgerTransaction: BflStruct
    ) {
        buildPath.zincSourceFile("contract_rules.zn") {
            listOf(ledgerTransaction, ledgerTransaction.fields.single { it.name == "commands" }.type as BflModule)
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
                body = """
                    // TODO Your assertions go here.
                    assert!(true != false, "Reality is in an inconsistent state.");
                """.trimIndent()
            }
        }
    }

    private fun generateMain(
        buildPath: Path,
        witness: Witness
    ) {
        buildPath.zincSourceFile("main.zn") {
            mod { module = "contract_rules" }
            newLine()
            listOf(witness, witness.publicInput)
                .sortedBy { it.getModuleName() }
                .forEach { dependency ->
                    mod { module = dependency.getModuleName() }
                    use { path = "${dependency.getModuleName()}::${dependency.id}" }
                    newLine()
                }
            function {
                name = "main"
                parameter {
                    name = "input"
                    type = witness.toZincId()
                }
                returnType = witness.publicInput.toZincId()
                body = """
                    let tx = input.deserialize();
                    contract_rules::verify(tx);
                    input.generate_hashes()
                """.trimIndent()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
