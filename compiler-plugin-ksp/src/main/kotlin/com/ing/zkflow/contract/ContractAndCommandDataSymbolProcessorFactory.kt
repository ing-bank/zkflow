package com.ing.zkflow.contract

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.ksp.implementations.ImplementationsSymbolProcessor
import com.ing.zkflow.ksp.implementations.MetaInfServicesProcessor
import com.ing.zkflow.serialization.ZKContractStateSerializerMapProvider
import com.ing.zkflow.serialization.ZkCommandDataSerializerMapProvider
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState

class ContractAndCommandDataSymbolProcessorFactory(
    environment: SymbolProcessorEnvironment
) {
    private val zkCommandMetaInfServicesProcessor = MetaInfServicesProcessor(
        environment.codeGenerator,
        ZKTransactionMetadataCommandData::class
    )
    private val contractStateMapProcessor = ContractAndCommandDataSerializerMapProcessor(
        ZKOwnableState::class,
        ContractState::class,
        ZKContractStateSerializerMapProvider::class,
        environment.codeGenerator
    )
    private val commandDataMapProcessor = ContractAndCommandDataSerializerMapProcessor(
        ZKTransactionMetadataCommandData::class,
        CommandData::class,
        ZkCommandDataSerializerMapProvider::class,
        environment.codeGenerator
    )

    val get = ImplementationsSymbolProcessor(
        listOf(
            zkCommandMetaInfServicesProcessor,
            contractStateMapProcessor,
            commandDataMapProcessor
        )
    )
}
