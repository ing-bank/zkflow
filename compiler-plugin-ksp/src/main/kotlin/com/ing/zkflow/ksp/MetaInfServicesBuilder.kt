package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import net.corda.core.internal.packageName
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

/**
 * This is a stateful generator of "META-INF/services/" files.
 * It remembers previous state, and renders the file including the previous state.
 */
class MetaInfServicesBuilder(
    private val codeGenerator: CodeGenerator,
    private val interfaceClass: KClass<*>,
) {
    private var allDiscoveredImplementations = mutableListOf<ScopedDeclaration>()

    @Suppress("SpreadOperator")
    fun createOrUpdate(
        implementations: List<ScopedDeclaration>
    ) {
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
    }
}

private fun OutputStream.appendText(text: String) = use {
    write(text.toByteArray(StandardCharsets.UTF_8))
}
