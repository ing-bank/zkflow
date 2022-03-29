package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.util.merge
import kotlin.reflect.KClass

/**
 * A [SymbolProcessor] for [ImplementationsProcessor]s.
 * This implementation takes care of extracting the relevant implementations from the code, and apply the corresponding
 * [ImplementationsProcessor]s.
 */
class ImplementationsSymbolProcessor(
    codeGenerator: CodeGenerator,
    private val implementationsProcessors: List<ImplementationsProcessor<*>>,
) : SymbolProcessor {
    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()
    private val metaInfServiceRegister = MetaInfServiceRegister(codeGenerator)

    private val implementationsVisitor = ImplementationsVisitor(
        implementationsProcessors.map { setOf(it.interfaceClass) }
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // To compute the set of new files, `resolver.getNewFiles()` ought to be used.
        // Unfortunately, this method fails on Mac.
        // To work around this issue the set of new files is computed.
        val allFiles = resolver.getAllFiles().toSet()
        val newFiles = allFiles.complement(visitedFiles)
        visitedFiles.addAll(newFiles)

        newFiles
            .fold(emptyMap<Set<KClass<*>>, List<ScopedDeclaration>>()) { acc, file ->
                acc.merge(implementationsVisitor.visitFile(file, null))
            }
            .forEach { (kClassSet, implementations) ->
                implementationsProcessors
                    .filter { setOf(it.interfaceClass) == kClassSet }
                    .map { it.process(implementations) }
                    .forEach {
                        if (it.implementations.isNotEmpty()) {
                            @Suppress("SpreadOperator")
                            metaInfServiceRegister.addImplementation(
                                it.providerClass,
                                *it.implementations.toTypedArray()
                            )
                        }
                    }
            }

        metaInfServiceRegister.emit()

        return emptyList()
    }

    /**
     * Set<KSFile> - Set<KSFile> does not work as expected.
     */
    private fun Set<KSFile>.complement(that: Set<KSFile>) =
        filter { first -> that.none { second -> first.filePath == second.filePath } }
}
