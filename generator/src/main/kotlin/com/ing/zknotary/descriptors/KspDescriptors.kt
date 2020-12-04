package com.ing.zknotary.descriptors

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.descriptors.types.AnnotatedSizedClass
import com.ing.zknotary.descriptors.types.DefaultableClass
import com.ing.zknotary.descriptors.types.IntDescriptor
import com.ing.zknotary.descriptors.types.ListDescriptor
import com.ing.zknotary.descriptors.types.PairDescriptor
import com.ing.zknotary.descriptors.types.TripleDescriptor

/**
 * Describes the property structure in a way allowing
 * derivation of its fixed length version, it is required to know
 * - the name of the property,
 * - list of user classes, for which fixed length version will be generated.
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

    val descriptor = typeDef.describe(Support.SizedClasses(annotatedClasses))

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
 * `support` indicates when further decomposition is possible.
 */
fun KSType.describe(support: Support): TypeDescriptor {
    support.requireFor(this)

    return when ("$declaration") {
        // Primitive types
        Int::class.simpleName -> IntDescriptor(0, declaration)

        //
        // Compound types
        Pair::class.simpleName -> PairDescriptor(
            arguments.subList(0, 2).map {
                val innerType = it.type?.resolve()
                require(innerType != null) { "Pair must have type arguments" }
                innerType.describe(support)
            }
        )

        Triple::class.simpleName -> TripleDescriptor(
            arguments.subList(0, 3).map {
                val innerType = it.type?.resolve()
                require(innerType != null) { "Pair must have type arguments" }
                innerType.describe(support)
            }
        )

        //
        // Collections
        List::class.simpleName -> ListDescriptor.fromKSP(this, support)

        // Unknown type allowing fixed length representation.
        else -> {
            val clazz = declaration as KSClassDeclaration
            when (support) {
                is Support.Default -> DefaultableClass(clazz)
                is Support.SizedClasses -> AnnotatedSizedClass(clazz)
            }
        }
    }
}

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"
