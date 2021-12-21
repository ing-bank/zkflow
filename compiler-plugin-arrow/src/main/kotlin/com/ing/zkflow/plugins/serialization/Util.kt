package com.ing.zkflow.plugins.serialization

import org.jetbrains.kotlin.psi.KtAnnotated

/**
 * Major source of inspiration
 * https://speakerdeck.com/heyitsmohit/writing-kotlin-compiler-plugins-with-arrow-meta?slide=81
 */

inline fun <reified T> KtAnnotated.hasAnnotation(): Boolean =
    annotationEntries.any { "${it.shortName}" == T::class.simpleName }
