package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.txmetadata.ZKTransactionMetadataProcessor

class CompositeSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return CompositeSymbolProcessor(
            ZKTransactionMetadataProcessor(environment)
        )
    }
}
