package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.ksp.LoggingCodeGenerator
import com.ing.zkflow.ksp.implementations.ZKPAnnotatedProcessor

class StableIdVersionedSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ZKPAnnotatedProcessor(environment.logger, LoggingCodeGenerator(environment))
    }
}
