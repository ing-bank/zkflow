package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.ZKP
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeElement

/**
 * Major source of inspiration
 * https://speakerdeck.com/heyitsmohit/writing-kotlin-compiler-plugins-with-arrow-meta?slide=81
 */

inline fun <reified T> KtAnnotated.hasAnnotation(): Boolean =
    annotationEntries.any { "${it.shortName}" == T::class.simpleName }

inline fun <reified T> KtAnnotated.annotationOrNull(): KtAnnotationEntry? =
    annotationEntries.singleOrNull { "${it.shortName}" == T::class.simpleName }

inline fun <reified T> KtAnnotated.annotationSingleArgOrNull(): String? =
    annotationOrNull<T>()?.run { valueArguments.single().asElement().text.trim() }

fun KtClass.isCorrectClassTypeForZKPAnnotation(): Boolean {
    require(!(isAnnotation() || isInterface())) {
        "Review class definition `${this.name}`. Classes annotated with ${ZKP::class.simpleName} may not be annotation classes, interfaces or abstract classes"
    }

    require(typeParameters.isEmpty()) {
        "Review class definition `${this.name}`. Classes annotated with ${ZKP::class.simpleName} may not contain generics"
    }

    return true
}

/**
 * Extract the root type of the full type, e.g., List<@A Int> -> List
 * Proceeds by removing type arguments in their entirety without recursion.
 * E.g., for `List<@Annotation CustomType<Int>>` the inner list type argument will be removed in a single go.
 * */
fun KtTypeElement.extractRootType(): TypeDefinition =
    // KtTypeElement does not have annotations attached to the outer type.
    // Its typeArguments are TypeReferences, their text representation does contain annotations.
    typeArgumentsAsTypes
        .fold(text) { acc, arg -> acc.replace(arg.text, "") }
        .replace("[<>, \n\t]".toRegex(), "")
        .let {
            val isNullable = it.last() == '?'
            // Drop the last `?` if present.
            val type = if (isNullable) it.substring(0, it.lastIndex) else it
            val isCollection = type in setOf(List::class.simpleName!!)
            TypeDefinition(type, isNullable, isCollection)
        }

data class TypeDefinition(val type: String, val isNullable: Boolean, val isCollection: Boolean)
