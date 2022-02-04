package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.toZincId
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.util.requireNotNull
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.Witness
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.Locale

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

    fun generateCircuitFor(zkCommand: ZKCommandData) {
        val commandMetadata = zkCommand.metadata
        // TODO this is a dirty hack just to make API ends meet, zinc-poet should be changed to operate on individual components instead of lists per type
        val zincInputs = commandMetadata.privateInputs.associate { zincTypeResolver.zincTypeOf(it.type) to 1 }
        val zincOutputs = commandMetadata.privateOutputs.associate { zincTypeResolver.zincTypeOf(it.type) to 1 }
        val zincReferences = commandMetadata.privateReferences.associate { zincTypeResolver.zincTypeOf(it.type) to 1 }

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
        val circuitName = zkCommand.circuitName()

        val buildPath = buildPathProvider.getBuildPath(commandMetadata)
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

    private fun generateMain(
        buildPath: Path,
        witness: Witness,
        ledgerTransaction: BflStruct
    ) {
        buildPath.zincSourceFile("main.zn") {
            mod { module = witness.getModuleName() }
            use { path = "${witness.getModuleName()}::${witness.id}" }
            newLine()
            mod { module = ledgerTransaction.getModuleName() }
            use { path = "${ledgerTransaction.getModuleName()}::${ledgerTransaction.id}" }
            newLine()
            function {
                name = "main"
                parameter {
                    name = "input"
                    type = witness.toZincId()
                }
                returnType = ledgerTransaction.toZincId()
                body = "input.deserialize()"
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
