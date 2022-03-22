package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistryProvider
import com.ing.zkflow.ksp.implementations.ImplementationsSymbolProcessor

class ZKFLowSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val surrogatesSerializerGenerator = SurrogateSerializerGenerator(environment.codeGenerator)

        val zkNetworkParametersProviderProcessor = ZKNetworkParametersProcessor()

        val contractStateRegistryProcessor = SerializerRegistryProcessor(
            ZKContractState::class, // For now only ZKContractState, this may change to ContractState when we have @ZKPSurrogate for toplevel ContractStates
            emptySet(),
            ContractStateSerializerRegistryProvider::class,
            environment.codeGenerator
        )

        val commandDataRegistryProcessor = SerializerRegistryProcessor(
            ZKCommandData::class, // For now only ZKCommandData, this may change to CommandData when we have @ZKPSurrogate for toplevel CommandData
            emptySet(),
            CommandDataSerializerRegistryProvider::class,
            environment.codeGenerator
        )

        return ImplementationsSymbolProcessor(
            environment.codeGenerator,
            listOf(
                contractStateRegistryProcessor,
                commandDataRegistryProcessor,
                zkNetworkParametersProviderProcessor,
                surrogatesSerializerGenerator,
            )
        )
    }
}
