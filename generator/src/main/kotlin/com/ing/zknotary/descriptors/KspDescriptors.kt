package com.ing.zknotary.descriptors

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Describes the property structure in a way allowing
 * derivation of its fixed length version, it is required to know
 * - the name of the property,
 * - list of user classes, for which, we know, fixed length version will be generated.
 *   This means that these classes are annotated with `Sized`.
 *
 * The latter is required because potentially the property type may include
 * such classes as internal components.
 */
fun KSPropertyDeclaration.describe(annotatedClasses: List<KSClassDeclaration>) = try {
    PropertyDescriptor(
        name = simpleName.asString(),
        typeDescriptor = type.resolve().describe(DescriptionContext(annotatedClasses))
    )
} catch (e: Exception) {
    // Rethrow error providing more context.
    throw IllegalStateException("$parentDeclaration.$this: ${e.message}", e)
}

/**
 * Describes `type` and its inner components recursively
 * allowing its synthesis into a fixed length variant.
 */
fun KSType.describe(context: DescriptionContext): TypeDescriptor {
    val exceptions = mutableListOf<String>()

    // Try built-in generating fixed length versions using built-in functionality.
    try {
        return TypeDescriptor.of(this, context)
    } catch (e: SupportException) {
        exceptions += e.message!!
    }

    // Try to leverage the knowledge of user "promised" fixed length classes.
    try {
        return context.describe(this)
    } catch (e: SupportException) {
        exceptions += e.message!!
    }

    // Fail.
    error(exceptions.joinToString(separator = " "))
}
