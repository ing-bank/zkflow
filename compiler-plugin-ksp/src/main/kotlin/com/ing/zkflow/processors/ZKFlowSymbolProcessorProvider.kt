package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.ksp.LoggingCodeGenerator
import com.ing.zkflow.ksp.implementations.ImplementationsSymbolProcessor

class ZKFlowSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val codeGenerator = LoggingCodeGenerator(environment)

        val zkNetworkParametersProviderProcessor = ZKNetworkParametersProcessor()

        return ImplementationsSymbolProcessor(
            environment.logger,
            codeGenerator,
            listOf(zkNetworkParametersProviderProcessor)
        )
    }
}
