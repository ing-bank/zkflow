package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.serialization.SerializerRegistryProvider
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import net.corda.core.internal.objectOrNewInstance
import java.util.ServiceLoader
import kotlin.reflect.full.isSubclassOf

fun instantiateZkCommands(commandRegistryProviders: Iterable<SerializerRegistryProvider>): List<ZKCommandData> =
    commandRegistryProviders
        .map { it.get().klass }
        .filter { it.isSubclassOf(ZKCommandData::class) }
        .map { it.objectOrNewInstance() as ZKCommandData }

fun main() {
    val commandDataSerializerRegistryProviders = ServiceLoader.load(SerializerRegistryProvider::class.java)
    instantiateZkCommands(commandDataSerializerRegistryProviders).forEach {
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
        ConstsFactory,
        CryptoUtilsFactory
    )
}
