package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.toZincId
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ContractStateTypeCount
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.util.requireNotNull
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.componentGroupEnum
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.Witness
import net.corda.core.contracts.ContractState
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
    private fun Map<KClass<out ContractState>, Int>.toZincType(): Map<BflModule, Int> = entries.associate {
        zincTypeResolver.zincTypeOf(it.key) to it.value
    }

    fun generateCircuitFor(zkTransaction: ZKTransactionMetadataCommandData) {
        val transactionMetadata = zkTransaction.transactionMetadata
        val inputs = transactionMetadata.commands.flatMap { it.inputs }.sumClasses().toZincType()
        val outputs = transactionMetadata.commands.flatMap { it.outputs }.sumClasses().toZincType()
        val references = transactionMetadata.commands.flatMap { it.references }.sumClasses().toZincType()

        val witness = createWitness(transactionMetadata, inputs, outputs, references)
        val inputGroup = stateAndRefsGroupFactory.createStructWithStateAndRefs(
            "InputGroup",
            inputs,
            witness.serializedInputUtxos.deserializedStruct
        )
        val referenceGroup = stateAndRefsGroupFactory.createStructWithStateAndRefs(
            "ReferenceGroup",
            references,
            witness.serializedReferenceUtxos.deserializedStruct
        )
        val ledgerTransaction = ledgerTransactionFactory.createLedgerTransaction(
            inputGroup,
            referenceGroup,
            transactionMetadata,
            witness
        )

        val codeGenerationOptions = CodeGenerationOptions(witness.getWitnessConfigurations())
        val circuitName = zkTransaction.circuitName()

        val buildPath = buildPathProvider.getBuildPath(transactionMetadata)
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
        transactionMetadata: ResolvedZKTransactionMetadata,
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

    /**
     * Returns a map of the different types, with the number of occurrences.
     */
    private fun List<ContractStateTypeCount>.sumClasses(): Map<KClass<out ContractState>, Int> =
        map { it.type to it.count }
            .fold(mutableMapOf<KClass<out ContractState>, Int>()) { map, item ->
                val currentCount = map[item.first] ?: 0
                map[item.first] = item.second + currentCount
                map
            }
            .toMap()

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

private fun ZKTransactionMetadataCommandData.circuitName(): String {
    return this::class.qualifiedName.requireNotNull { "ZKCommand class should be a named class." }
        .split(".")
        .filter { it[0].isUpperCase() && it != "Companion" }
        .joinToString("-") { it.toLowerCase(Locale.getDefault()) }
}
