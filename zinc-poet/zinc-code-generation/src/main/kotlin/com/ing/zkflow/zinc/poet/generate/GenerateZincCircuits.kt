package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import java.util.ServiceLoader
import kotlin.reflect.full.createInstance

private val zkCommands: List<ZKCommandData> by lazy {
    ServiceLoader.load(CommandDataSerializerRegistryProvider::class.java)
        .flatMap { it.list() }
        .mapNotNull { it.first.createInstance() as? ZKCommandData }
}

fun main() {
    zkCommands.forEach { circuitGenerator.generateCircuitFor(it) }
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
