package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class CompositeSymbolProcessor(
    private vararg val processors: SymbolProcessor
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        return processors.flatMap { processor ->
            processor.process(resolver)
        }
    }
}
