package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import net.corda.core.internal.objectOrNewInstance
import java.util.ServiceLoader
import kotlin.reflect.full.isSubclassOf

fun instantiateZkCommands(commandRegistryProviders: Iterable<KClassSerializerProvider>): List<ZKCommandData> =
    commandRegistryProviders
        .map { it.get().klass }
        .filter { it.isSubclassOf(ZKCommandData::class) }
        .map { it.objectOrNewInstance() as ZKCommandData }

fun main() {
    instantiateZkCommands(ServiceLoader.load(KClassSerializerProvider::class.java)).forEach {
        DefaultCircuitGenerator.generateCircuitFor(it)
    }
}

private val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)
private val zkNetworkParameters = ZKNetworkParametersServiceLoader.latest
private val standardTypes = StandardTypes(zkNetworkParameters)

object DefaultCircuitGenerator : CircuitGenerator(
    BuildPathProvider.Default,
    CommandContextFactory(standardTypes),
    standardTypes,
    zincTypeResolver,
    ConstsFactory,
    CryptoUtilsFactory
)
