package com.ing.zkflow.util

import kotlin.reflect.KClass

/**
 * Construct a scoped name, including all elements from the qualified name that start with an uppercase character.
 * This name can be used instead of simpleName to avoid naming conflicts with nested classes.
 * Returns null when qualifiedName is not available, f.e. in anonymous objects.
 */
val <T : Any> KClass<T>.scopedName: String? get() =
    qualifiedName
        ?.split(".")
        ?.filter {
            it[0].isUpperCase()
        }
        ?.joinToString("") { it }
        ?: simpleName

/**
 * Try to get a [KClass] for this string, return null if it fails.
 */
fun String.tryGetKClass(): KClass<out Any>? = jvmClassNamePermutations()
    .asSequence()
    .mapNotNull {
        try {
            Class.forName(it).kotlin
        } catch (e: ClassNotFoundException) {
            null
        }
    }
    .firstOrNull()

/**
 * Take a string and create all possible JVM class name permutations, ordered in such a way that a correct match is
 * likely at the start of the list.
 * A JVM Class name for a Kotlin class can use either a '.' or a '$' as a separator.
 * Consider the input string "example.MyClass.InnerClass", this will generate the following list:
 * - "example.MyClass.InnerClass"
 * - "example.MyClass$InnerClass"
 * - "example$MyClass.InnerClass"
 * - "example$MyClass$InnerClass"
 */
internal fun String.jvmClassNamePermutations(): List<String> = split(".").reversed()
    .fold(emptyList()) { acc, part ->
        if (acc.isEmpty()) {
            listOf(part)
        } else {
            acc.map { "$part.$it" } + acc.map { "$part\$$it" }
        }
    }
