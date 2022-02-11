package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.serialization.ZkCommandDataSerializerMapProvider
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.corda.core.internal.exists
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.ServiceLoader
import kotlin.reflect.full.createInstance

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

private val metadata: List<ResolvedZKCommandMetadata> by lazy {
    ServiceLoader.load(ZkCommandDataSerializerMapProvider::class.java)
        .flatMap { it.list() }
        .mapNotNull { it.first.createInstance() as? ZKCommandData }
        .map { it.metadata }
}

@SuppressFBWarnings("PATH_TRAVERSAL_IN", justification = "Src path is intentional param")
fun main(args: Array<String>) {
    println("Arguments: ${args.joinToString("", "[\n", "]") { "\t`$it`\n" }}")

    val srcDir = args.single(SRC_DIR)
    val circuitNames = args.multiple(CIRCUIT)

    validateDiscoveredCircuits(circuitNames)

    println("Discovered ${metadata.size} circuits")
    metadata.forEach {
        val circuitName = it.circuit.name
        println("Generating circuit: $circuitName")
        circuitGenerator.generateCircuitFor(it)
        val sourceContractRules = Paths.get(srcDir).resolve(circuitName).resolve(CONTRACT_RULES_ZN)
        val targetContractRules = it.circuit.buildFolder.toPath().resolve("src").resolve(CONTRACT_RULES_ZN)
        if (sourceContractRules.exists()) {
            Files.copy(sourceContractRules, targetContractRules, StandardCopyOption.REPLACE_EXISTING)
        } else {
            println("No $CONTRACT_RULES_ZN defined for circuit $circuitName, copying generated one...")
            Paths.get(srcDir).ensureDirectory(circuitName)
            Files.copy(targetContractRules, sourceContractRules)
        }
        println("Copying $CONTRACT_RULES_ZN for $circuitName")
    }
}

private fun validateDiscoveredCircuits(circuitNames: List<String>) {
    val discoveredCircuitNames = metadata.map {
        it.circuit.name
    }.toSet()
    if (!discoveredCircuitNames.containsAll(circuitNames)) {
        val missingCircuits = circuitNames - discoveredCircuitNames
        println("The following circuits are required, but are not loaded via ${ZkCommandDataSerializerMapProvider::class}: $missingCircuits")
    }
}

private val circuitGenerator: CircuitGenerator by lazy {
    val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)
    val standardTypes = StandardTypes(zincTypeResolver)
    CircuitGenerator(
        BuildPathProvider.Default,
        LedgerTransactionFactory(
            standardTypes
        ),
        standardTypes,
        zincTypeResolver,
        ConstsFactory(),
        CryptoUtilsFactory()
    )
}
