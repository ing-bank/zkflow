package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.symbol.KSAnnotated
import kotlin.reflect.KClass

/**
 * A processor that processes implementations of [interfaceClass].
 */
interface ImplementationsProcessor<T : Any> {
    val interfaceClass: KClass<T>
    fun process(implementations: List<ScopedDeclaration>): List<KSAnnotated>
}
