package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import java.util.ServiceLoader
import kotlin.reflect.full.allSuperclasses

private val zkCommands: List<ZKCommandData> by lazy {
    ServiceLoader.load(CommandDataSerializerRegistryProvider::class.java)
        .flatMap { it.list() }
        .map { it.klass }
        .filter { it.allSuperclasses.contains(ZKCommandData::class) }
        .map {
            it.constructors.find { constructor ->
                constructor.parameters.isEmpty()
            } ?: throw IllegalStateException("There MUST be at least one constructor for ZKCommandData without arguments")
        }
        .mapNotNull { it.call() as? ZKCommandData }
}

fun main() {
    println("--> I mean, we're gonna try...")
    zkCommands.forEach {
        println("--> Trying to generate for ${it.metadata.circuit.name}")
        circuitGenerator.generateCircuitFor(it)
    }
}

private val circuitGenerator: CircuitGenerator by lazy {
    val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)
    val zkNetworkParameters = ZKNetworkParametersServiceLoader.latest
    val standardTypes = StandardTypes(zkNetworkParameters)
    CircuitGenerator(
        BuildPathProvider.Default,
        CommandContextFactory(
            standardTypes
        ),
        standardTypes,
        zincTypeResolver,
        ConstsFactory(),
        CryptoUtilsFactory()
    )
}
