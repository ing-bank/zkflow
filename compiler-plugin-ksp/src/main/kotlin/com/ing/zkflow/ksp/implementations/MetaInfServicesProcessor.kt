package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotated
import com.ing.zkflow.util.appendText
import net.corda.core.internal.packageName
import kotlin.reflect.KClass

/**
 * This is a stateful generator of "META-INF/services/" files.
 * It remembers previous state, and renders the file including the previous state.
 */
class MetaInfServicesProcessor<T : Any>(
    private val codeGenerator: CodeGenerator,
    override val interfaceClass: KClass<T>,
) : ImplementationsProcessor<T> {
    private val allDiscoveredImplementations = mutableListOf<ScopedDeclaration>()

    @Suppress("SpreadOperator")
    override fun process(implementations: List<ScopedDeclaration>): List<KSAnnotated> {
        allDiscoveredImplementations.addAll(implementations)
        codeGenerator.createNewFile(
            Dependencies(
                false,
                *allDiscoveredImplementations
                    .mapNotNull { it.declaration.containingFile }
                    .toTypedArray()
            ),
            "META-INF/services",
            interfaceClass.packageName,
            interfaceClass.simpleName!!
        ).appendText(
            allDiscoveredImplementations
                .map { it.java.qualifiedName }
                .joinToString("\n") { it }
        )
        return emptyList()
    }
}
