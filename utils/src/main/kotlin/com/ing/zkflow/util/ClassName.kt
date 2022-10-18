package com.ing.zkflow.util

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

fun KClass<*>.buildFullyDistinguishableClassName(vararg postfixes: String): ClassName =
    asClassName().buildFullyDistinguishableClassName(*postfixes)

fun ClassName.buildFullyDistinguishableClassName(vararg postfixes: String): ClassName {
    var prefixes = emptyList<String>()
    var className = this
    var container = className.enclosingClassName()
    while (container != null) {
        prefixes = listOf(container.simpleName) + prefixes
        className = container
        container = className.enclosingClassName()
    }

    val nameComponents = listOf(
        prefixes.joinToString(separator = "") { it.capitalize() },
        simpleName,
    ) + postfixes

    return ClassName(
        packageName,
        nameComponents
            .filter { it.isNotBlank() }
            .joinToString(separator = "")
    )
}
