package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.SerdeLogger
import org.jetbrains.kotlin.psi.KtTypeReference

// Compiler plugins are invoked at a stage past syntactical verification,
// it _feels_ safe to analyze types based on the syntactic expressions without resolving them.
// Nevertheless, this is just a fallback because, respective descriptors are not available,
// if these statements are not true, then this functionality is better be re-implemented using descriptors.

fun KtTypeReference.process(): SerializingObject =
    innerProcess(ignoreNullability = false, depth = 0)

/**
 * Recursively process the type tree to build a serializing object for the type.
 */
private fun KtTypeReference.innerProcess(ignoreNullability: Boolean = false, depth: Int = 0): SerializingObject {
    val type = typeElement
    require(type != null) { "Cannot infer type of: $text" }
    val tab = "|-${List(depth){"-"}.joinToString(separator = "", postfix = " ")}"

    SerdeLogger.log("$tab${if (ignoreNullability) type.text.substring(0 until type.textLength - 1) else type.text}")

    val root = type
        .extractRootType()
        .let { if (ignoreNullability) it.copy(isNullable = false) else it }

    // • Strip nullability.
    if (root.isNullable) {
        return innerProcess(ignoreNullability = true, depth + 1).wrapNull()
    }

    // • Invariant: root.isNullable = false
    if (Processors.isNonNative(root.type)) {
        return Processors.forNonNativeType(this)
    }

    // • Strip outer type.
    val children = type.typeArgumentsAsTypes.map {
        it.innerProcess(ignoreNullability = false, depth + 1).let { so ->
            if (root.isCollection) so.wrapDefault() else so
        }
    }

    return Processors.forNativeType(root.type, this, children)
}
