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
    propertyName: String,
    annotatedClasses: List<KSClassDeclaration>
): PropertyDescriptor {
    val name = simpleName.asString()
    val typeDef = type.resolve()

    val descriptor = typeDef.describe(UserTypeDescriptor.Sized(annotatedClasses))

    return PropertyDescriptor(
        name = name,
        type = descriptor.type,
        fromInstance = descriptor.toCodeBlock("$propertyName.$name"),
        default = descriptor.default
    )
}

/**
 * Describes `type` and its inner components recursively
 * allowing its synthesis into a fixed length variant.
 *
 * `userTypeDescriptor` indicates when further decomposition is possible.
 */
fun KSType.describe(userTypeDescriptor: UserTypeDescriptor): TypeDescriptor {
    val typename = "$declaration"
    val errors = mutableListOf("Type $typename is not supported\n")

    // Try built-in generating fixed length versions using built-in functionality.
    var result = TypeDescriptor.of(this, userTypeDescriptor)
    if (result.descriptor != null) {
        return result.descriptor!!
    }
    errors += result.error!!

    // Try to leverage the knowledge of user "promised" fixed length classes.
    result = userTypeDescriptor.of(this)
    if (result.descriptor != null) {
        return result.descriptor!!
    }
    errors += result.error!!

    // Fail.
    error(errors.joinToString(separator = "\n"))
}

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"
