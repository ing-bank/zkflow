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
