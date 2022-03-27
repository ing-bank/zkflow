package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.ksp.implementations.ImplementationsSymbolProcessor

class ZKFLowSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val surrogatesSerializerGenerator = SurrogateSerializerGenerator(environment.codeGenerator)

        val zkNetworkParametersProviderProcessor = ZKNetworkParametersProcessor()

        return ImplementationsSymbolProcessor(
            environment.codeGenerator,
            listOf(
                zkNetworkParametersProviderProcessor,
                surrogatesSerializerGenerator,
            )
        )
    }
}
