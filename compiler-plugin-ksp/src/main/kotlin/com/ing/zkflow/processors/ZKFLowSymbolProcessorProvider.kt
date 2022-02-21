package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.ksp.implementations.ImplementationsSymbolProcessor
import com.ing.zkflow.serialization.infra.ZKContractStateSerializerMapProvider
import com.ing.zkflow.serialization.infra.ZkCommandDataSerializerMapProvider
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState

class ZKFLowSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val zkNetworkParametersProviderProcessor = ZKNetworkParametersProcessor()

        val contractStateMapProcessor = ContractStateAndCommandDataSerializerMapProcessor(
            ZKOwnableState::class, // TODO: should this not just be ZKContractState? This is too specific
            ContractState::class,
            ZKContractStateSerializerMapProvider::class,
            environment.codeGenerator
        )

        val commandDataMapProcessor = ContractStateAndCommandDataSerializerMapProcessor(
            ZKCommandData::class,
            CommandData::class,
            ZkCommandDataSerializerMapProvider::class,
            environment.codeGenerator
        )

        return ImplementationsSymbolProcessor(
            environment.codeGenerator,
            listOf(
                contractStateMapProcessor,
                commandDataMapProcessor,
                zkNetworkParametersProviderProcessor
            )
        )
    }
}
