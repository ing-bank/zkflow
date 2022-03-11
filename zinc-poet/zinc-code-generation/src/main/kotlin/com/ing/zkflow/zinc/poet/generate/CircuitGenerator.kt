package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.mod
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.bfl.use
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.util.ensureFile
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.Witness
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroupsContainer
import net.corda.core.internal.deleteRecursively
import net.corda.core.internal.writeText
import org.slf4j.LoggerFactory
import java.nio.file.Path

@Suppress("LongParameterList")
class CircuitGenerator(
    private val buildPathProvider: BuildPathProvider,
    private val commandContextFactory: CommandContextFactory,
    private val standardTypes: StandardTypes,
    private val zincTypeResolver: ZincTypeResolver,
    private val constsFactory: ConstsFactory,
    private val cryptoUtilsFactory: CryptoUtilsFactory
) {
    fun generateCircuitFor(zkCommand: ZKCommandData) {
        val commandMetadata = zkCommand.metadata
        val witnessGroups = WitnessGroupsContainer(
            commandMetadata,
            standardTypes,
            zincTypeResolver
        )
        val witness = Witness(
            witnessGroups,
            commandMetadata,
            standardTypes,
        )

        val commandContext = commandContextFactory.createCommandContext(
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

        sequenceOf(witness, commandContext).allModules {
            buildPath.zincSourceFile(this, codeGenerationOptions)
        }

        constsFactory.generateConsts(buildPath, codeGenerationOptions)
        cryptoUtilsFactory.generateCryptoUtils(buildPath)
        copyContractRules(buildPath, zkCommand.verifyPrivate())
        generateMain(buildPath, witness)
        buildPath.zincSourceFile(componentGroupEnum, codeGenerationOptions)
        logger.info("... done")
    }

    private fun copyContractRules(buildPath: Path, privateVerifyFunction: String) =
        buildPath.ensureDirectory("src").ensureFile("contract_rules.zn").writeText(privateVerifyFunction)

    private fun generateMain(
        buildPath: Path,
        witness: Witness
    ) {
        buildPath.zincSourceFile("main.zn") {
            mod { module = "contract_rules" }
            newLine()
            listOf(witness, witness.publicInput).sortedBy { it.getModuleName() }.forEach { dependency ->
                add(dependency.mod())
                add(dependency.use())
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
