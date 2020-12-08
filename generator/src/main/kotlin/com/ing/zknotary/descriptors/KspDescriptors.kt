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
fun KSPropertyDeclaration.describe(
    annotatedClasses: List<KSClassDeclaration>
): PropertyDescriptor {
    return PropertyDescriptor(
        name = simpleName.asString(),
        typeDescriptor = type.resolve().describe(DescriptionContext(annotatedClasses))
    )
}

/**
 * Describes `type` and its inner components recursively
 * allowing its synthesis into a fixed length variant.
 *
 * `userTypeDescriptor` indicates when further decomposition is possible.
 */
fun KSType.describe(context: DescriptionContext): TypeDescriptor {
    val typename = "$declaration"
    // Try built-in generating fixed length versions using built-in functionality.
    var descriptor = TypeDescriptor.of(this, context)
    if (descriptor != null) {
        return descriptor
    }

    // Try to leverage the knowledge of user "promised" fixed length classes.
    descriptor = context.describe(this)
    if (descriptor != null) {
        return descriptor
    }

    val errors = listOf(
        "No sized version can be constructed for $typename",
        //
        "No built-in support for $typename",
        "Built-in supported types: ", *TypeDescriptor.supported.toTypedArray(),
        //
        "$typename is not marked as `Sized`",
        "Classes marked as sized: ", *context.annotatedClasses.map { it.simpleName.asString() }.toTypedArray(),
        //
        "$typename is not marked with `UseDefault` or has marked but has no default constructor"
    )

    // Fail.
    error(errors.joinToString(separator = "\n"))
}

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"
