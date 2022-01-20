package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.common.zkp.metadata.CommandMetadataCache
import com.ing.zkflow.zinc.poet.generate.types.CommandGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory
import kotlin.reflect.KClass

private object MyZincTypeResolved : ZincTypeResolver {
    override fun zincTypeOf(kClass: KClass<*>): BflModule {
        return struct {
            name = kClass.simpleName
            field {
                name = "bogus"
                type = BflPrimitive.Bool
            }
        }
    }
}

fun main(args: Array<String>) {
    println("Arguments: ${args.joinToString { it }}")

    // val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)
    val zincTypeResolver = MyZincTypeResolved
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
