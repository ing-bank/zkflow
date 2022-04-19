package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import net.corda.core.contracts.CommandData
import java.util.ServiceLoader
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.allSuperclasses

fun instantiateZkCommands(commandRegistryProviders: Iterable<CommandDataSerializerRegistryProvider>): List<ZKCommandData> =
    commandRegistryProviders
        .flatMap { it.list() }
        .map { it.klass }
        .filter { it.allSuperclasses.contains(ZKCommandData::class) }
        .mapNotNull { getDefaultConstructor(it).call() as? ZKCommandData }

private fun getDefaultConstructor(it: KClass<out CommandData>): KFunction<CommandData> =
    it.constructors.find { constructor ->
        constructor.parameters.isEmpty()
    } ?: throw IllegalStateException("There MUST be at least one constructor for ZKCommandData without arguments")

fun main() {
    val commandDataSerializerRegistryProviders = ServiceLoader.load(CommandDataSerializerRegistryProvider::class.java)
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
        ConstsFactory(),
        CryptoUtilsFactory()
    )
}
