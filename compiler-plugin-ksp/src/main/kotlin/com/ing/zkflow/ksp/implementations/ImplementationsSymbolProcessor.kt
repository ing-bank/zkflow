package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.util.merge
import kotlin.reflect.KClass

/**
 * A [SymbolProcessor] for [ImplementationsProcessor]s.
 * This implementation takes care of extracting the relevant implementations from the code, and apply the corresponding
 * [ImplementationsProcessor]s.
 */
class ImplementationsSymbolProcessor(
    private val implementationsProcessors: List<ImplementationsProcessor<*>>
) : SymbolProcessor {
    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()

    private val implementationsVisitor = ImplementationsVisitor(
        implementationsProcessors.map { it.interfaceClass }.distinct()
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // To compute the set of new files, `resolver.getNewFiles()` ought to be used.
        // Unfortunately, this method fails on Mac.
        // To work around this issue the set of new files is computed.
        val allFiles = resolver.getAllFiles().toSet()
        val newFiles = allFiles.complement(visitedFiles)
        visitedFiles.addAll(newFiles)

        val implementations: Map<KClass<*>, List<ScopedDeclaration>> = newFiles
            .fold(emptyMap()) { acc, file ->
                acc.merge(implementationsVisitor.visitFile(file, null))
            }

        return implementations.flatMap { (interfaceClass, implementations) ->
            if (implementations.isNotEmpty()) {
                implementationsProcessors
                    .filter { it.interfaceClass == interfaceClass }
                    .flatMap { it.process(implementations) }
            } else {
                emptyList()
            }
        }
    }

    /**
     * Set<KSFile> - Set<KSFile> does not work as expected.
     */
    private fun Set<KSFile>.complement(that: Set<KSFile>) =
        filter { first -> that.none { second -> first.filePath == second.filePath } }
}
