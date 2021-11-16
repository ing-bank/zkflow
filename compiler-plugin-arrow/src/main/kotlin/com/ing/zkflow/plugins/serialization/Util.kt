package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.ZKP
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

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

fun <T : Any> KClass<T>.implementsInterface(interfaceClass: KClass<*>): Boolean =
    allSuperclasses.contains(interfaceClass)

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

/**
 * Recursively build the clean type tree.
 * E.g. for  `List<@Annotation CustomType<Int>>`:
 * 1. `List<@Annotation CustomType<Int>>` -> List
 * 2. `CustomType<Int>` -> CustomType
 * 3. `Int` -> Int
 * =. List<CustomType<Int>>
 */
fun KtTypeReference.cleanTypeDeclaration(ignoreNullability: Boolean): String {
    val type = typeElement
    require(type != null) { "Cannot infer type of: `$text`" }

    val rootType = type.extractRootType()
    val nullability = if (ignoreNullability) "" else if (rootType.isNullable) "?" else ""

    return if (type.typeArgumentsAsTypes.isEmpty()) {
        "${rootType.type}$nullability"
    } else {
        "${rootType.type}${
        type.typeArgumentsAsTypes.joinToString(
            prefix = "<",
            separator = ", ",
            postfix = ">"
        ) { it.cleanTypeDeclaration(ignoreNullability = false) }
        }$nullability"
    }
}
/**
 * Recursively build the clean type tree.
 * E.g. for  `List<@Annotation CustomType<Int>>`:
 * 1. `List<@Annotation CustomType<Int>>` -> List
 * 2. `CustomType<Int>` -> CustomType
 * 3. `Int` -> Int
 */
fun KtTypeReference.attachAnnotation(annotation: KClass<*>): String {
    val type = typeElement
    require(type != null) { "Cannot infer type of: `$text`" }

    val annotationsDeclaration = (annotationEntries.map { it.text } + "@${annotation.qualifiedName}").joinToString(separator = " ")

    val rootType = type.extractRootType()
    val nullability = if (rootType.isNullable) "?" else ""

    return if (type.typeArgumentsAsTypes.isEmpty()) {
        "$annotationsDeclaration ${rootType.type}$nullability"
    } else {
        "$annotationsDeclaration ${rootType.type}${
        type.typeArgumentsAsTypes.joinToString(
            prefix = "<",
            separator = ", ",
            postfix = ">"
        ) { it.attachAnnotation(annotation) }
        }$nullability"
    }
}
