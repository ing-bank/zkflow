package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.zkp.metadata.CommandMetadataCache
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.zinc.poet.generate.types.CommandGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory
import net.corda.core.internal.exists
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

const val SRC_DIR = "srcDir"
const val CIRCUIT = "circuit"
const val CONTRACT_RULES_ZN = "contract_rules.zn"

private fun Array<String>.single(key: String): String = asSequence()
    .map { it.split("=", limit = 2) }
    .single { it[0] == key }[1]

private fun Array<String>.multiple(key: String): List<String> = asSequence()
    .map { it.split("=", limit = 2) }
    .filter { it[0] == key }
    .map { it[1] }
    .toList()

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("zincPoet")

    logger.info("Arguments: ${args.joinToString("", "[\n", "]") { "\t`$it`\n" }}")

    val srcDir = args.single(SRC_DIR)
    val circuitNames = args.multiple(CIRCUIT)

    val circuitGenerator = instantiate()

    validateDiscoveredCircuits(circuitNames)

    logger.info("Discovered ${CommandMetadataCache.metadata.size} circuits")
    CommandMetadataCache.metadata.forEach {
        val circuitName = it.value.circuit.name
        logger.info("Generating circuit: $circuitName")
        circuitGenerator.generateCircuitFor(it.value)
        val sourceContractRules = Paths.get(srcDir).resolve(circuitName).resolve(CONTRACT_RULES_ZN)
        val targetContractRules = it.value.circuit.buildFolder.toPath().resolve("src").resolve(CONTRACT_RULES_ZN)
        if (sourceContractRules.exists()) {
            Files.copy(sourceContractRules, targetContractRules, StandardCopyOption.REPLACE_EXISTING)
        } else {
            logger.info("No $CONTRACT_RULES_ZN defined for circuit $circuitName, copying generated one...")
            Paths.get(srcDir).ensureDirectory(circuitName)
            Files.copy(targetContractRules, sourceContractRules)
        }
        logger.info("Copying $CONTRACT_RULES_ZN for $circuitName")
    }
}

private fun validateDiscoveredCircuits(circuitNames: List<String>) {
    val discoveredCircuitNames = CommandMetadataCache.metadata.values.map {
        it.circuit.name
    }.toSet()
    if (!discoveredCircuitNames.containsAll(circuitNames)) {
        val missingCircuits = circuitNames - discoveredCircuitNames
        println("The following circuits are required, but are not discovered from ${CommandMetadataCache::class}: $missingCircuits")
    }
}

private fun instantiate(): CircuitGenerator {
    val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)
    val standardTypes = StandardTypes(zincTypeResolver)
    val commandGroupFactory = CommandGroupFactory(standardTypes)
    return CircuitGenerator(
        BuildPathProvider.Default,
        LedgerTransactionFactory(
            commandGroupFactory,
            standardTypes
        ),
        standardTypes,
        StateAndRefsGroupFactory(standardTypes),
        zincTypeResolver,
        ConstsFactory(),
        CryptoUtilsFactory()
    )
}
