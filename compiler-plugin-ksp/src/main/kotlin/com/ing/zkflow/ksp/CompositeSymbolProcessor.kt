package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile

/**
 * A [SymbolProcessor] that processes only Kotlin files.
 *
 * [CompositeSymbolProcessor] will only invoke this processor with all kotlin files when there are new Kotlin files,
 * thus preventing an endless loop when f.e. resource files are generated.
 */
interface KotlinSymbolProcessor : SymbolProcessor {
    fun process(resolver: Resolver, allKotlinFiles: Sequence<KSFile>): List<KSAnnotated>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return process(resolver, resolver.getAllFiles().filter(::isKotlinFile))
    }
}

class CompositeSymbolProcessor(
    private vararg val processors: SymbolProcessor
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allFiles by lazy { getKotlinFilesToProcess(resolver) }

        return processors.flatMap { processor ->
            when (processor) {
                is KotlinSymbolProcessor -> allFiles?.let { processor.process(resolver, it) } ?: emptyList()
                else -> processor.process(resolver)
            }
        }
    }
}

/**
 * Returns all Kotlin files only when there are new Kotlin source files, otherwise null.
 */
private fun getKotlinFilesToProcess(resolver: Resolver): Sequence<KSFile>? =
    if (resolver.getNewFiles().containsKotlinFiles()) {
        resolver.getAllFiles().filter(::isKotlinFile)
    } else {
        null
    }

private fun Sequence<KSFile>.containsKotlinFiles(): Boolean = any(::isKotlinFile)

private fun isKotlinFile(ksFile: KSFile) = ksFile.fileName.endsWith(".kt")
