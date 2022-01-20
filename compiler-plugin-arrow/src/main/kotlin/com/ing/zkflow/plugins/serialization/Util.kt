package com.ing.zkflow.plugins.serialization

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * Major source of inspiration
 * https://speakerdeck.com/heyitsmohit/writing-kotlin-compiler-plugins-with-arrow-meta?slide=81
 */

inline fun <reified T> KtAnnotated.hasAnnotation(): Boolean =
    annotationEntries.any { "${it.shortName}" == T::class.simpleName }

inline fun <reified T> KtAnnotated.findAnnotation(): KtAnnotationEntry? =
    annotationEntries.singleOrNull { "${it.shortName}" == T::class.simpleName }

/**
 * Find annotation `T`, if it has a single argument, return it,
 * in all other cases, return null.
 */
inline fun <reified T> KtAnnotated.annotationSingleArgument(): String? =
    findAnnotation<T>()?.valueArguments?.singleOrNull()?.asElement()?.text?.trim()

/**
 * Apply [block] when [predicate] is true, otherwise return [this].
 */
fun <T> T.applyWhen(predicate: Boolean, block: T.() -> T): T {
    return if (predicate) block() else this
}
