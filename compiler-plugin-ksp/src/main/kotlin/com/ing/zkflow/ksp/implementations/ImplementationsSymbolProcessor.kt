package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.ing.zkflow.ksp.implementations.ImplementationsVisitor.Companion.toMapOfLists

/**
 * A [SymbolProcessor] for [ImplementationsProcessor]s.
 * This implementation takes care of extracting the relevant implementations from the code, and apply the corresponding
 * [ImplementationsProcessor]s.
 */
class ImplementationsSymbolProcessor(
    private val implementationsProcessors: List<ImplementationsProcessor<*>>
) : SymbolProcessor {
    private val implementationsVisitor = ImplementationsVisitor(
        implementationsProcessors.map { it.interfaceClass }.distinct()
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val implementations = resolver.getNewFiles()
            .flatMap { implementationsVisitor.visitFile(it, null) }
            .toList()
            .toMapOfLists()

        return implementations.flatMap { (interfaceClass, implementations) ->
            if (implementations.isNotEmpty()) {
                implementationsProcessors.filter { it.interfaceClass == interfaceClass }
                    .flatMap { it.process(implementations) }
            } else {
                emptyList()
            }
        }
    }
}
