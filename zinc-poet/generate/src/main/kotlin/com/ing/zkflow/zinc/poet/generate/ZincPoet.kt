package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.zkp.metadata.CommandMetadataCache
import com.ing.zkflow.zinc.poet.generate.types.CommandGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory

fun main(args: Array<String>) {
    println("Arguments: ${args.joinToString { it }}")

    val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)
    val standardTypes = StandardTypes(zincTypeResolver)
    val commandGroupFactory = CommandGroupFactory(standardTypes)
    val circuitGenerator = CircuitGenerator(
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

    println("Discovered ${CommandMetadataCache.metadata.size} circuits")
    CommandMetadataCache.metadata.entries.forEach {
        println("Generating circuit: ${it.value.circuit.name}")
        circuitGenerator.generateCircuitFor(it.value)
    }
    println("")
}
