package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE
import com.ing.zinc.bfl.CORDA_MAGIC_BITS_SIZE_CONSTANT_NAME
import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ContractStateTypeCount
import com.ing.zkflow.util.requireNotNull
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.Witness
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.PublicKey
import java.util.Locale
import kotlin.reflect.KClass

class CircuitGenerator(
    private val zincTypeResolver: ZincTypeResolver,
    private val buildPathProvider: BuildPathProvider
) {
    private val standardTypes = StandardTypes(zincTypeResolver)
    private val ledgerTransactionFactory = LedgerTransactionFactory()

    private fun Map<KClass<out ContractState>, Int>.toZincTransactionState(): Map<BflModule, Int> = entries.associate {
        standardTypes.transactionState(zincTypeResolver.zincTypeOf(it.key)) to it.value
    }

    fun generateCircuitFor(zkTransaction: ZKTransactionMetadataCommandData) {
        val transactionMetadata = zkTransaction.transactionMetadata
        val inputs = transactionMetadata.commands.flatMap { it.inputs }.sumClasses()
        val outputs = transactionMetadata.commands.flatMap { it.outputs }.sumClasses()
        val references = transactionMetadata.commands.flatMap { it.references }.sumClasses()

        val witness = createWitness(zkTransaction, inputs, outputs, references)
        val ledgerTransaction =
            ledgerTransactionFactory.createLedgerTransaction(inputs, references, transactionMetadata, witness)

        val codeGenerationOptions = CodeGenerationOptions(witness.getWitnessConfigurations())
        val circuitName = zkTransaction.circuitName()

        val buildPath = buildPathProvider.getBuildPath(transactionMetadata)
        buildPath.toFile().mkdirs()

        logger.info("Generating circuit $circuitName in folder $buildPath")

        buildPath.createZargoToml(circuitName, "1.0.0")

        sequenceOf(witness, ledgerTransaction).allModules {
            buildPath.zincSourceFile(this, codeGenerationOptions)
        }

        generateConsts(buildPath, codeGenerationOptions)
        generateMain(buildPath, witness, ledgerTransaction)
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

    private fun generateConsts(
        buildPath: Path,
        codeGenerationOptions: CodeGenerationOptions
    ) {
        buildPath.zincSourceFile("$CONSTS.zn") {
            constant {
                name = CORDA_MAGIC_BITS_SIZE_CONSTANT_NAME
                type = ZincPrimitive.U24
                initialization = "$CORDA_MAGIC_BITS_SIZE"
                comment = "Number of bits in Corda serialization header"
            }
            codeGenerationOptions.witnessGroupOptions.forEach {
                add(it.witnessSizeConstant)
            }
        }
    }

    private fun createWitness(
        zkTransaction: ZKTransactionMetadataCommandData,
        inputs: Map<KClass<out ContractState>, Int>,
        outputs: Map<KClass<out ContractState>, Int>,
        references: Map<KClass<out ContractState>, Int>
    ): Witness {
        return Witness(
            zkTransaction,
            inputs.toZincTransactionState(),
            outputs.toZincTransactionState(),
            references.toZincTransactionState(),
            zincTypeResolver.zincTypeOf(Party::class),
            zincTypeResolver.zincTypeOf(PublicKey::class),
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
