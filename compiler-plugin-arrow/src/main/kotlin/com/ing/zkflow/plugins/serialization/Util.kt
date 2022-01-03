package com.ing.zkflow.plugins.serialization

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * Major source of inspiration
 * https://speakerdeck.com/heyitsmohit/writing-kotlin-compiler-plugins-with-arrow-meta?slide=81
 */

inline fun <reified T> KtAnnotated.hasAnnotation(): Boolean =
    annotationEntries.any { "${it.shortName}" == T::class.simpleName }

inline fun <reified T> KtAnnotated.annotationOrNull(): KtAnnotationEntry? =
    annotationEntries.singleOrNull { "${it.shortName}" == T::class.simpleName }

inline fun <reified T> KtAnnotated.annotationSingleArgOrNull(): String? =
    annotationOrNull<T>()?.valueArguments?.singleOrNull()?.asElement()?.text?.trim()
