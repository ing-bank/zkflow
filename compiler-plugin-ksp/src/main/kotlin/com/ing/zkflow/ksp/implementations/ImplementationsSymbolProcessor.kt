package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.util.merge

/**
 * A [SymbolProcessor] for [ImplementationsProcessor]s.
 * This implementation takes care of extracting the relevant implementations from the code, and apply the corresponding
 * [ImplementationsProcessor]s.
 */
class ImplementationsSymbolProcessor(
    private val logger: KSPLogger,
    codeGenerator: CodeGenerator,
    private val implementationsProcessors: List<ImplementationsProcessor<*>>,
) : SymbolProcessor {
    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()
    private val metaInfServiceRegister = MetaInfServiceRegister(codeGenerator)

    private val implementationsVisitor = ImplementationsVisitor(
        implementationsProcessors.map { Implementor(setOf(it.interfaceClass)) }
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val newFiles = resolver.getNewFiles(visitedFiles)
        logger.info("New Files: ${newFiles.joinToString { it.filePath }}")
        visitedFiles.addAll(newFiles)

        newFiles
            .fold(emptyMap<Implementor, List<ScopedDeclaration>>()) { acc, file ->
                acc.merge(implementationsVisitor.visitFile(file, null))
            }
            .forEach { (implementor, implementations) ->
                implementationsProcessors
                    .filter { setOf(it.interfaceClass) == implementor.superTypes }
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
}
