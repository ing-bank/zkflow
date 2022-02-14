package com.ing.zkflow.stateandcommanddata

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.ksp.implementations.ImplementationsSymbolProcessor
import com.ing.zkflow.serialization.ZKContractStateSerializerMapProvider
import com.ing.zkflow.serialization.ZkCommandDataSerializerMapProvider
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState

class ContractStateAndCommandDataSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val contractStateMapProcessor = ContractStateAndCommandDataSerializerMapProcessor(
            ZKOwnableState::class,
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
                commandDataMapProcessor
            )
        )
    }
}
